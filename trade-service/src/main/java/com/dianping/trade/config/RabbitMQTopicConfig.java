package com.dianping.trade.config;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQTopicConfig {
    public static final String QUEUE = "seckillQueue";
    public static final String EXCHANGE = "seckillExchange";
    public static final String ROUTINGKEY = "seckill.#";

    //死信交换机/队列
    public static final String DLX = "seckillDlx";
    public static final String DLQ = "seckillDlq";
    public static final String DLQ_ROUTINGKEY = "seckill.dlq";

    @Bean
    public Queue queue() {
        //持久化
        return QueueBuilder.durable(QUEUE)
                //绑定死信交换机
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_ROUTINGKEY)
                .build();
    }

    @Bean
    public Queue dlq(){
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange dlx(){
        return new TopicExchange(DLX);
    }

    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(topicExchange()).with(ROUTINGKEY);
    }

    @Bean
    public Binding dlqBinding(){
        return BindingBuilder.bind(dlq()).to(dlx()).with(DLQ_ROUTINGKEY);
    }
}
