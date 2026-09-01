package com.dianping.common.jwt;

import com.dianping.common.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;

/**
 * JWT签发与校验（HS256）
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    private SecretKey getKey(){
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发token：userId放subject，昵称头像放claim
     */
    public String createToken(UserDTO userDTO){
        return Jwts.builder()
                .subject(String.valueOf(userDTO.getId()))
                .claim("nickName",userDTO.getNickName())
                .claim("icon",userDTO.getIcon())
                .claim("role",userDTO.getRole()==null?1:userDTO.getRole())
                .expiration(Date.from(Instant.now().plusSeconds(jwtProperties.getExpireMinutes()*60L)))
                .signWith(getKey())
                .compact();
    }

    /**
     * 校验并解析token，非法或过期抛JwtException
     */
    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * token有效期（秒），登出写黑名单TTL用
     */
    public long getExpireSeconds(){
        return jwtProperties.getExpireMinutes()*60L;
    }
}
