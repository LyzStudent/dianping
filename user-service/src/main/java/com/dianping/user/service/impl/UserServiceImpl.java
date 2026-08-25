package com.dianping.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.common.dto.LoginFormDTO;
import com.dianping.common.dto.Result;
import com.dianping.common.dto.UserDTO;
import com.dianping.common.jwt.JwtProperties;
import com.dianping.common.jwt.JwtUtil;
import com.dianping.common.util.RedisContants;
import com.dianping.common.util.RegexUtils;
import com.dianping.common.util.SystemConstants;
import com.dianping.common.util.UserHolder;
import com.dianping.user.entity.User;
import com.dianping.user.mapper.UserMapper;
import com.dianping.user.service.UserInfoService;
import com.dianping.user.service.UserService;
import com.dianping.user.util.MailUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dianping.common.util.RedisContants.*;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendcode(String phone, HttpSession session) throws MessagingException {
        //1.判断是否再一级限制条件内
        Boolean oneLevelLimit=stringRedisTemplate.opsForSet().isMember(ONE_LEVELLIMIT_KEY+phone,"1");
        if(oneLevelLimit!=null&&oneLevelLimit){
            //在一级限制内，不能发送验证码
            return Result.fail("您需要等待5分钟后再请求");
        }
        //2.判断是否在二级限制条件内
        Boolean twoLevelLimit=stringRedisTemplate.opsForSet().isMember(TWO_LEVELLIMIT_KEY+phone,"1");
        if(twoLevelLimit!=null&&twoLevelLimit){
            //在二级限制条件内，不能发送验证码
            return Result.fail("您需要等待20分钟后再请求");
        }

        //3.检查过去1分钟内发送验证码的次数
        long oneMinuteAgo=System.currentTimeMillis()-1000*60;
        long oneMinuteCount=stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY+phone,oneMinuteAgo,System.currentTimeMillis());
        if(oneMinuteCount>=1){
            return Result.fail("距离上次发送时间不足1分钟，请1分钟后重试");
        }

        //4.检查发送验证码的次数
        long fiveMinuteAgo=System.currentTimeMillis()-1000*60*5;
        long fiveMinuteCount=stringRedisTemplate.opsForZSet().count(SENDCODE_SENDTIME_KEY+phone,fiveMinuteAgo,System.currentTimeMillis());
        if(fiveMinuteCount%3==2&&fiveMinuteCount>5){
            //发送了8，11，14，...次，进入二级限制
            stringRedisTemplate.opsForSet().add(TWO_LEVELLIMIT_KEY+phone,"1");
            stringRedisTemplate.expire(TWO_LEVELLIMIT_KEY+phone,20, TimeUnit.MINUTES);
            return Result.fail("接下来如需再发送，请等待20分钟后再请求");
        }else if(fiveMinuteCount==5){
            //过去5分钟已经发送了5次，进入一级限制
            stringRedisTemplate.opsForSet().add(ONE_LEVELLIMIT_KEY+phone,"1");
            stringRedisTemplate.expire(ONE_LEVELLIMIT_KEY+phone,5,TimeUnit.MINUTES);
            return Result.fail("5分钟内已经发送了5次，接下来如需再发送请等待5分钟");
        }

        //生成验证码
        String code= MailUtils.achieveCode();

        //将生成的验证码保持到redis中
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL,TimeUnit.MINUTES);

        log.info("发送登录验证码: {}",code);
        //发送验证码（失败不抛异常，避免异常逃逸到错误分发被误判为未登录）
        try {
            MailUtils.sendtoMail(phone,code);
        } catch (MessagingException e) {
            //演示环境降级：SMTP 不通不阻断流程，验证码已在上面 log.info 输出，登录照样能用
            log.warn("验证码邮件发送失败，已降级为仅日志模式（演示环境）, phone={}",phone,e);
        }

        //更新发送时间和次数
        stringRedisTemplate.opsForZSet().add(SENDCODE_SENDTIME_KEY+phone,System.currentTimeMillis()+"",System.currentTimeMillis());

        return Result.ok();
    }

    //登录注册
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //获取信息（本项目验证码走邮箱，phone字段实际存放的是邮箱地址）
        String phone=loginForm.getPhone();
        String code=loginForm.getCode();
        //校验邮箱格式是否正确（去掉旧版手机号正则校验：手机号与邮箱正则互斥，会永远登录失败）
        if(RegexUtils.isEmailInvalid(phone)){
            return Result.fail("邮箱格式不正确");
        }

        //从redis中获取验证码，并进行校验
        String CacheCode=stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        //校验验证码（code或CacheCode为空时防NPE）
        if(code==null||CacheCode==null||!code.equals(CacheCode)){
            return Result.fail("无效的验证码");
        }
        //上述都正确则从数据库中查用户信息
        User user=query().eq("phone",phone).one();

        //判断用户是否存在
        if(user==null){
            user=createUser(phone);
        }

        //将User转为UserDTO并签发JWT（不再用UUID+Redis hash）
        UserDTO userDTO= BeanUtil.copyProperties(user,UserDTO.class);
        String token=jwtUtil.createToken(userDTO);

        //登录成功则删除验证码消息
        stringRedisTemplate.delete(LOGIN_CODE_KEY+phone);

        //返回token
        return Result.ok(token);
    }

    //创建新用户
    private User createUser(String phone){
        //创建对象
        User user=new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX+ RandomUtil.randomString(10));
        //保存用户 insert into tb_user(phone,nick_name) values(?,?);
        save(user);
        return user;
    }

    //签到功能
    @Override
    public Result sign() {
        //1.获取当前用户
        Long userId= UserHolder.getUser().getId();

        //2.获取日期
        LocalDateTime now=LocalDateTime.now();

        //3.拼接key
        String keySuffix=now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String key=USER_SIGN_KEY+userId+keySuffix;

        //4.获取今天是当月第几天
        int dayOfMonth=now.getDayOfMonth();

        //5.写入Redis
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    //统计签到功能
    @Override
    public Result signCount() {
        //1.获取当前用户
        Long userId=UserHolder.getUser().getId();

        //2.获取日期
        LocalDateTime now= LocalDateTime.now();

        //3.拼接key
        String keySuffix=now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String key=USER_SIGN_KEY+userId+keySuffix;

        //4.获取今天是当月第几天
        int dayOfMonth= now.getDayOfMonth();


        //5.获取截止今日的签到记录
        List<Long> result=stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        if(result==null||result.isEmpty()){
            return Result.ok(0);
        }

        //6.循环遍历
        int count=0;
        Long num=result.get(0);
        while(true){
            if((num & 1)==1){
                count++; //签到天数+1
                num=num>>1; //右移抛弃今天看下一天
            }else{
                break;
            }
        }
        return  Result.ok(count);
    }


    /**
     * 登出功能：JWT无状态不能删，改为写黑名单，网关校验命中即拒绝
     * @param token
     * @return
     */
    @Override
    public Result logout(String token) {
        //1.校验token是否为空
        if(token==null||token.isBlank()){
            return Result.fail("用户未登录");
        }

        //2.将token写入黑名单，TTL与JWT有效期一致
        stringRedisTemplate.opsForValue().set(
                LOGIN_BLACKLIST_KEY+token,"1",jwtProperties.getExpireMinutes(),TimeUnit.MINUTES);

        //3.返回成功
        return Result.ok();
    }
}
