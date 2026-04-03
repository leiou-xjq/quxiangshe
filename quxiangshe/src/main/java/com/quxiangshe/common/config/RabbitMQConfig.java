package com.quxiangshe.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置队列、交换机、绑定关系
 */
@Configuration
public class RabbitMQConfig {

    // ==================== Feed流相关配置 ====================

    /**
     * Feed推送交换机（fanout类型）
     */
    public static final String FEED_EXCHANGE = "feed.exchange";

    /**
     * Feed推送队列
     */
    public static final String FEED_PUSH_QUEUE = "feed.push.queue";

    /**
     * ==================== 评论相关配置 ====================
     */

    /**
     * 评论队列
     */
    public static final String COMMENT_QUEUE = "comment.queue";

    /**
     * 评论交换机
     */
    public static final String COMMENT_EXCHANGE = "comment.exchange";

    /**
     * 评论路由键
     */
    public static final String COMMENT_ROUTING_KEY = "comment.create";

    /**
     * ==================== 笔记相关配置 ====================
     */

    /**
     * 笔记队列
     */
    public static final String NOTE_QUEUE = "note.queue";

    /**
     * 笔记交换机
     */
    public static final String NOTE_EXCHANGE = "note.exchange";

    /**
     * 笔记索引路由键
     */
    public static final String NOTE_ROUTING_KEY = "note.index";

    // ==================== Feed流配置 ====================

    /**
     * 创建Feed推送交换机
     */
    @Bean
    public FanoutExchange feedExchange() {
        return ExchangeBuilder.fanoutExchange(FEED_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建Feed推送队列
     */
    @Bean
    public Queue feedPushQueue() {
        return QueueBuilder.durable(FEED_PUSH_QUEUE)
                .withArgument("x-message-ttl", 604800000) // 7天过期
                .build();
    }

    /**
     * 绑定Feed队列到交换机
     */
    @Bean
    public Binding feedPushBinding(Queue feedPushQueue, FanoutExchange feedExchange) {
        return BindingBuilder.bind(feedPushQueue).to(feedExchange);
    }

    // ==================== 评论配置 ====================

    /**
     * 创建评论交换机
     */
    @Bean
    public DirectExchange commentExchange() {
        return ExchangeBuilder.directExchange(COMMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建评论队列
     */
    @Bean
    public Queue commentQueue() {
        return QueueBuilder.durable(COMMENT_QUEUE)
                .withArgument("x-message-ttl", 1800000) // 30分钟过期
                .build();
    }

    /**
     * 绑定评论队列到交换机
     */
    @Bean
    public Binding commentBinding(Queue commentQueue, DirectExchange commentExchange) {
        return BindingBuilder.bind(commentQueue).to(commentExchange).with(COMMENT_ROUTING_KEY);
    }

    // ==================== AI摘要配置 ====================

    // ==================== 笔记配置 ====================

    /**
     * 创建笔记交换机
     */
    @Bean
    public DirectExchange noteExchange() {
        return ExchangeBuilder.directExchange(NOTE_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建笔记队列
     */
    @Bean
    public Queue noteQueue() {
        return QueueBuilder.durable(NOTE_QUEUE)
                .withArgument("x-message-ttl", 1800000) // 30分钟过期
                .build();
    }

    /**
     * 绑定笔记队列到交换机
     */
    @Bean
    public Binding noteBinding(Queue noteQueue, DirectExchange noteExchange) {
        return BindingBuilder.bind(noteQueue).to(noteExchange).with(NOTE_ROUTING_KEY);
    }

    // ==================== 通用配置 ====================

    /**
     * 消息序列化器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
