package com.dianping.common.util;

import java.util.zip.DeflaterInputStream;

public class RedisContants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String ONE_LEVELLIMIT_KEY="limit:onelevel:";
    public static final String TWO_LEVELLIMIT_KEY="limit:twolevel:";

    public static final String SENDCODE_SENDTIME_KEY="limit:onelevel";

    //登出黑名单，JWT版：网关校验时命中即拒绝
    public static final String LOGIN_BLACKLIST_KEY = "login:blacklist:";

    public static final String USER_SIGN_KEY="sign:";

    public static final Long CACHE_NULL_TTL=2L;

    public static final String CACHE_SHOP_KEY="cache:shop:";
    public static final Long CACHE_SHOP_TTL=30L;

    public static final String LOCK_SHOP_KEY="lock:shop:";
    public static final Long LOCK_SHOP_TTL=10L;

    public static final String SHOP_GEO_KEY="shop:geo:";

    public static final String CACHE_SHOP_TYPE_KEY="cache:shoptype:";

    public static final String SECKILL_STOCK_KEY="seckill:stock:";
    public static final String SECKILL_ORDER_KEY = "seckill:order:";

    public static final String FEED_KEY="feed:";
    public static final String BLOG_LIKED_KEY="blog:liked:";

    public static final String COMMENT_LIKED_KEY = "comment:liked:";

    public static final String CACHE_BANNER_KEY="cache:banner:list";
    public static final Long CACHE_BANNER_TTL=30L;

    public static final String HOT_WORD_KEY="hot:word:zset";
}
