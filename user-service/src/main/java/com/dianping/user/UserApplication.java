package com.dianping.user;

import com.dianping.user.entity.User;
import com.dianping.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication(scanBasePackages = "com.dianping")
@MapperScan("com.dianping.user.mapper")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    @Bean
    public CommandLineRunner adminInitalizer(UserService userService){
        return args -> {
            if(userService.query().eq("phone","admin@dianping.com").count()==0){
                User admin=new User();
                admin.setPhone("admin@dianping.com");
                admin.setNickName("系统管理员");
                admin.setPassword(com.dianping.user.util.PasswordEncoder.encodes("123456"));
                admin.setRole(3);
                userService.save(admin);
                log.info("已创建管理员账号 admin@dianping.com / 123456");
            }
        };
    }
}
