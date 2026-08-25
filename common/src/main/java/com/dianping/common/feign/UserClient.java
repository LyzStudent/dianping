package com.dianping.common.feign;

import com.dianping.common.dto.Result;
import com.dianping.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 跨服务用户信息
 */
@FeignClient(name = "user-service",path = "/user")
public interface UserClient {

    /**
     * 单个用户昵称、头像
     */
    @GetMapping("/dto/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    /**
     * 批量，user-service保证按传入顺序返回
     */
    @GetMapping("/dtos")
    List<UserDTO> getUsersById(@RequestParam("ids") String ids);

    @PostMapping("/inner/points/add")
    Result addPoints(@RequestParam Long userId,@RequestParam Integer points);
}
