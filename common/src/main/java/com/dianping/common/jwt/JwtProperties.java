package com.dianping.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置（网关与user-service必须使用完全相同的secret）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hmdp.jwt")
public class JwtProperties {

    /**
     * HS256密钥，至少32字节
     */
    private String secret;

    /**
     * token有效期
     */
    private Integer expireMinutes=120;
}
