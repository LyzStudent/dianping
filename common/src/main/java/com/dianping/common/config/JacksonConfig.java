package com.dianping.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局统一 Jackson 时间格式：把 LocalDateTime/LocalDate/LocalTime 的默认 ISO 格式
 * （2026-08-30T23:59:59，T 分隔）改成 yyyy-MM-dd HH:mm:ss（空格分隔），
 * 反序列化（请求体解析）和序列化（返回 JSON）同时生效。
 * 所有服务都 @SpringBootApplication(scanBasePackages = "com.dianping")，自动被扫到。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // java.util.Date 也统一成同一种格式（可选）
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(DATE));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(DATE));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(TIME));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(TIME));
        };
    }
}
