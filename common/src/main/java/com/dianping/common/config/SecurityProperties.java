package com.dianping.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 免登录路径白名单（读取hmdp.security.exclude-paths）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hmdp.security")
public class SecurityProperties {
    private List<String> excludepaths=new ArrayList<>();
}
