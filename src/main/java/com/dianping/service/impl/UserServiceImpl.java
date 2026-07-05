package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.UserInfoService;
import com.dianping.service.UserService;
import com.dianping.utils.MailUtils;
import com.dianping.utils.RegexUtils;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisContants.*;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        //发送验证码
        MailUtils.sendtoMail(phone,code);

        //更新发送时间和次数
        stringRedisTemplate.opsForZSet().add(SENDCODE_SENDTIME_KEY+phone,System.currentTimeMillis()+"",System.currentTimeMillis());

        return Result.ok();
    }

    //登录注册
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //获取信息
        String phone=loginForm.getPhone();
        String code=loginForm.getCode();
        //校验手机号是否正确，不同的请求应该再去确认
        if(RegexUtils.isPhoneInvalid(phone)){
            //无效则报错信息
            return Result.fail("手机号格式不正确!");
        }

        //从redis中获取验证码，并进行校验
        String CacheCode=stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        //1.校验邮箱
        if(RegexUtils.isEmailInvalid(phone)){
            return Result.fail("邮箱格式不正确");
        }
        //2.不符合格式则报错
        if(CacheCode==null||!code.equals(CacheCode)){
            return Result.fail("无效的验证码");
        }
        //上述都正确则从数据库中查用户信息

        //select* from tb_user where phone=?
        User user=query().eq("phone",phone).one();

        //判断用户是否存在
        if(user==null){
            user=createUser(phone);
        }
        //保存用户信息到redis中
        String token= UUID.randomUUID().toString();

        //将userDto对象转化为HashMap存储
        UserDTO userDTO= BeanUtil.copyProperties(user,UserDTO.class);
        HashMap<String,String> userMap=new HashMap<>();
        userMap.put("id",String.valueOf(userDTO.getId()));
        userMap.put("nickName",userDTO.getNickName());
        userMap.put("icon",userDTO.getIcon());

        //存储
        String tokenKey=LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);

        //设置token有效期为30分钟
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);

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
            if((num%1)==1){
                count++; //签到天数+1
                num=num>>1; //右移抛弃今天看下一天
            }else{
                break;
            }
        }
        return  Result.ok(count);
    }


    /**
     * 登出功能
     * @param token
     * @return
     */
    @Override
    public Result logout(String token) {
        //1.校验token是否为空
        if(token==null||token.isBlank()){
            return Result.fail("用户未登录");
        }

        //2.删除redis中的用户信息
        String key=LOGIN_USER_KEY+token;
        Boolean deleted=stringRedisTemplate.delete(key);

        //3.返回成功
        return Result.ok();
    }
}
