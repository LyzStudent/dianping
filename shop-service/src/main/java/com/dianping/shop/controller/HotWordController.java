package com.dianping.shop.controller;

import com.dianping.common.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.dianping.common.util.RedisContants.HOT_WORD_KEY;

@RestController
@RequestMapping("/shop/hot")
public class HotWordController {

    /**
     * redis无数据时兜底，保证前端永远有热词
     */
    private static final List<String> DEFAULT_WORDS= Arrays.asList("火锅", "自助餐", "烤肉", "奶茶", "日料", "KTV", "咖啡");

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 取热度最高的前10个搜索词
     */
    @GetMapping("/keywords")
    public Result keywords(){
        Set<String> words=stringRedisTemplate.opsForZSet().reverseRange(HOT_WORD_KEY,0,9);
        if(words==null||words.isEmpty()) {
            return Result.ok(DEFAULT_WORDS);
        }
        return Result.ok(List.copyOf(words));
    }

    /**
     * 记录一次搜索（热度+1）
     */
    @PostMapping("/add")
    public Result add(@RequestParam String keyword){
        if(keyword==null||keyword.trim().isEmpty()){
            return Result.fail("关键词不能为空");
        }
        stringRedisTemplate.opsForZSet().incrementScore(HOT_WORD_KEY,keyword.trim(),1);
        return Result.ok();
    }
}
