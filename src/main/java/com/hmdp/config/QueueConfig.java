package com.hmdp.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
@Configuration
public class QueueConfig {

    //普通交换机名称
    public static final String X_EXCHANGE = "X";
    //死信交换机名称
    public static final String Y_DEAD_LETTER_EXCHANGE = "Y";
    //普通队列名称
    public static final String QUEUE_A = "QA";
    //死信队列名称
    public static final String DEAD_LETTER_QUEUE_D = "QD";
    //普通队列绑定路由键
    public static final String QUEUE_A_BINDING_KEY = "XA";
    //死信路由键
    public static final String DEAD_LETTER_ROUTING_KEY = "YD";


    /**
     * 声明x交换机
     * @return
     */
    @Bean("xExchange")//别名和方法名取一样
    public DirectExchange xExchange(){
        return new DirectExchange(X_EXCHANGE);
    }

    /**
     * 声明y交换机
     * @return
     */
    @Bean("yExchange")//别名和方法名取一样
    public DirectExchange yExchange(){
        return new DirectExchange(Y_DEAD_LETTER_EXCHANGE);
    }

    /**
     * 声明队列A
     * @return
     */
    @Bean("queueA")
    public Queue queueA(){
        final HashMap<String, Object> arguments
                = new HashMap<>();
        //设置死信交换机
        arguments.put("x-dead-letter-exchange",Y_DEAD_LETTER_EXCHANGE);
        //设置死信RoutingKey
        arguments.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        //设置TTL设置10秒过期
        arguments.put("x-message-ttl",10000);

        return QueueBuilder.durable(QUEUE_A)
                .withArguments(arguments)
                .build();
    }

    /**
     * 声明死信队列D
     * @return
     */
    @Bean("queueD")
    public Queue queueD(){
        return QueueBuilder.durable(DEAD_LETTER_QUEUE_D)
                .build();
    }

    /**
     * A队列绑定X交换机
     * @param queueA
     * @return
     */
    @Bean
    public Binding queueABindingX(@Qualifier("queueA")Queue queueA,
                                  @Qualifier("xExchange") DirectExchange xExchange){
        return BindingBuilder.bind(queueA).to(xExchange).with(QUEUE_A_BINDING_KEY);
    }

    /**
     * D队列绑定Y交换机
     * @param queueD
     * @return
     */
    @Bean
    public  Binding queueDBindingY(@Qualifier("queueD")Queue queueD,
                                   @Qualifier("yExchange") DirectExchange yExchange
    ){
        return BindingBuilder.bind(queueD).to(yExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * 统一 JSON 消息转换器：生产端/消费端共用，正确处理 LocalDateTime 等 Java 时间类型，
     * 替代手写 JSON 字符串，避免时间格式不一致导致的解析失败。
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }


}

