package com.dianping.utils;

import cn.hutool.extra.mail.Mail;

import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

public class MailUtils {
    public static void main(String[] args) throws MessagingException {
        //测试方法
        sendtoMail("3476422215@qq.com",new MailUtils().achieveCode());
    }

    //发送email处的代码
    public static void sendtoMail(String email,String code) throws MessagingException {
        //创建properties类用于记录邮箱的一些属性
        Properties properties=new Properties();
        //表示SMTP发送邮件进行身份认证
        properties.put("mail.smtp.auth","true");
        //填写SMTP服务器
        properties.put("mail.smtp.host","smtp.qq.com");
        //QQ邮箱端口号
        properties.put("mail.smtp.port","587");
        //写信人账号
        properties.put("mail.user","3476422215@qq.com");
        //STMP口令
        properties.put("mail.password","tbrymqzjloiedbaj");
        //构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator=new Authenticator() {
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                String username=properties.getProperty("mail.user");
                String password=properties.getProperty("mail.password");
                return new PasswordAuthentication(username,password);
            }
        };
        //使用环境属性和授权信息，创建邮件会话
        Session mailsession=Session.getInstance(properties, authenticator);
        //创建邮件消息
        MimeMessage message=new MimeMessage(mailsession);
        //设置发件人
        InternetAddress from=new InternetAddress(properties.getProperty("mail.user"));
        //设置收件人的邮箱
        InternetAddress to=new InternetAddress(email);
        message.setRecipient(MimeMessage.RecipientType.TO,to);
        //设置邮件标题
        message.setSubject("验证码");
        //设置邮件的内容体
        message.setContent("尊敬的用户：你好！\n注册验证码为："+code+"(有效期为一分钟，请勿告知他人)","text/html;charset=UTF-8");
        //发送邮件
        Transport.send(message);
    }

    //产生一个验证码的逻辑代码
    public static String achieveCode(){
        String[] beforeShuffle=new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a",
                "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z"};
        List<String> list= Arrays.asList(beforeShuffle);//将String转化为List
        Collections.shuffle(list);//打乱
        StringBuilder sb=new StringBuilder();
        for(String s:beforeShuffle){
            sb.append(s);//将集合转化为字符串
        }
        return sb.substring(3,8);
    }
}
