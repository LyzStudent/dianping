package com.dianping.trade.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Slf4j
@Configuration
public class RabbitTemplateConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate template=new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        //发布确认:Broker是否收到
        template.setConfirmCallback((correlationData, ack, cause) ->{
            if(!ack){
                log.error("MQ 消息发送失败,id= {},cause= {}",correlationData,cause);
            }
        } );

        //消息不可路由（交换机没匹配到队列）
        template.setReturnsCallback(returnedMessage ->
            log.error("MQ消息不可路由:exchange= {},routingKey= {},body= {}",
                    returnedMessage.getMessage(),returnedMessage.getRoutingKey(),
                    new String(returnedMessage.getMessage().getBody())));
        return template;
    }
}
