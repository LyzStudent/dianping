package com.dianping.common.feign;

import com.dianping.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 跨服务店铺信息
 */
@FeignClient(name = "shop-service",path = "/shop")
public interface ShopClient {

    /**
     * 内部:返回店铺所属用户id
     */
    @GetMapping("/inner/owner/{id}")
    Result getShopOwner(@PathVariable("id") Long shopId);
}
