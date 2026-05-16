package com.quxiangshe.backend.config;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {
    
    public static final String FEED_PUSH_EXCHANGE = "feed.push.exchange";
    public static final String FEED_PUSH_QUEUE = "feed.push.queue.v2";
    public static final String FEED_PUSH_ROUTING_KEY = "feed.push";
    
    public static final String FEED_DLX_EXCHANGE = "feed.push.exchange.dlx";
    public static final String FEED_DLX_QUEUE = "feed.push.queue.dlx";
    
    public static final String PRIVATE_MESSAGE_EXCHANGE = "quxiangshe.private-message.exchange";
    public static final String PRIVATE_MESSAGE_QUEUE = "quxiangshe.private-message.queue";
    public static final String PRIVATE_MESSAGE_ROUTING_KEY = "private.message";

    public static final String NOTIFICATION_EXCHANGE = "quxiangshe.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "quxiangshe.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.create";
    public static final String NOTIFICATION_DLX_EXCHANGE = "quxiangshe.notification.exchange.dlx";
    public static final String NOTIFICATION_DLX_QUEUE = "quxiangshe.notification.queue.dlx";

    public static final String EMAIL_EXCHANGE = "quxiangshe.email.exchange";
    public static final String EMAIL_QUEUE = "quxiangshe.email.queue";
    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String EMAIL_DLX_EXCHANGE = "quxiangshe.email.exchange.dlx";
    public static final String EMAIL_DLX_QUEUE = "quxiangshe.email.queue.dlx";

    public static final String VIDEO_EXCHANGE = "quxiangshe.video.exchange";
    public static final String VIDEO_TRANSCODE_QUEUE = "quxiangshe.video.transcode.queue";
    public static final String VIDEO_ROUTING_KEY = "video.transcode";
    public static final String VIDEO_DLX_EXCHANGE = "quxiangshe.video.exchange.dlx";
    public static final String VIDEO_DLX_QUEUE = "quxiangshe.video.queue.dlx";

    // 审核任务队列
    public static final String REVIEW_EXCHANGE = "quxiangshe.review.exchange";
    public static final String REVIEW_QUEUE = "quxiangshe.review.queue";
    public static final String REVIEW_ROUTING_KEY = "review.task";
    public static final String REVIEW_DLX_EXCHANGE = "quxiangshe.review.exchange.dlx";
    public static final String REVIEW_DLX_QUEUE = "quxiangshe.review.queue.dlx";
    
    @Bean
    public DirectExchange feedPushExchange() {
        return new DirectExchange(FEED_PUSH_EXCHANGE, true, false);
    }
    
    @Bean
    public DirectExchange feedDlxExchange() {
        return new DirectExchange(FEED_DLX_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue feedPushQueue() {
        return QueueBuilder.durable(FEED_PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", FEED_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "feed.push.dlq")
                .build();
    }
    
    @Bean
    public Queue feedDlxQueue() {
        return QueueBuilder.durable(FEED_DLX_QUEUE).build();
    }
    
    @Bean
    public Binding feedPushBinding(Queue feedPushQueue, DirectExchange feedPushExchange) {
        return BindingBuilder.bind(feedPushQueue)
                .to(feedPushExchange)
                .with(FEED_PUSH_ROUTING_KEY);
    }
    
    @Bean
    public Binding feedDlxBinding(Queue feedDlxQueue, DirectExchange feedDlxExchange) {
        return BindingBuilder.bind(feedDlxQueue)
                .to(feedDlxExchange)
                .with("feed.push.dlq");
    }
    
    @Bean
    public DirectExchange privateMessageExchange() {
        return new DirectExchange(PRIVATE_MESSAGE_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue privateMessageQueue() {
        return QueueBuilder.durable(PRIVATE_MESSAGE_QUEUE).build();
    }
    
    @Bean
    public Binding privateMessageBinding(Queue privateMessageQueue, DirectExchange privateMessageExchange) {
        return BindingBuilder.bind(privateMessageQueue)
                .to(privateMessageExchange)
                .with(PRIVATE_MESSAGE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDlxExchange() {
        return new DirectExchange(NOTIFICATION_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationDlxQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLX_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationDlxBinding(Queue notificationDlxQueue, DirectExchange notificationDlxExchange) {
        return BindingBuilder.bind(notificationDlxQueue)
                .to(notificationDlxExchange)
                .with("notification.dlq");
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange emailDlxExchange() {
        return new DirectExchange(EMAIL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailDlxQueue() {
        return QueueBuilder.durable(EMAIL_DLX_QUEUE).build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailDlxBinding(Queue emailDlxQueue, DirectExchange emailDlxExchange) {
        return BindingBuilder.bind(emailDlxQueue)
                .to(emailDlxExchange)
                .with("email.dlq");
    }

    @Bean
    public DirectExchange videoExchange() {
        return new DirectExchange(VIDEO_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange videoDlxExchange() {
        return new DirectExchange(VIDEO_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue videoTranscodeQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODE_QUEUE)
                .withArgument("x-dead-letter-exchange", VIDEO_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "video.dlq")
                .build();
    }

    @Bean
    public Queue videoDlxQueue() {
        return QueueBuilder.durable(VIDEO_DLX_QUEUE).build();
    }

    @Bean
    public Binding videoTranscodeBinding(Queue videoTranscodeQueue, DirectExchange videoExchange) {
        return BindingBuilder.bind(videoTranscodeQueue)
                .to(videoExchange)
                .with(VIDEO_ROUTING_KEY);
    }

    @Bean
    public Binding videoDlxBinding(Queue videoDlxQueue, DirectExchange videoDlxExchange) {
        return BindingBuilder.bind(videoDlxQueue)
                .to(videoDlxExchange)
                .with("video.dlq");
    }

    // 审核队列
    @Bean
    public DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange reviewDlxExchange() {
        return new DirectExchange(REVIEW_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewQueue() {
        return QueueBuilder.durable(REVIEW_QUEUE)
                .withArgument("x-dead-letter-exchange", REVIEW_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "review.dlq")
                .build();
    }

    @Bean
    public Queue reviewDlxQueue() {
        return QueueBuilder.durable(REVIEW_DLX_QUEUE).build();
    }

    @Bean
    public Binding reviewBinding(Queue reviewQueue, DirectExchange reviewExchange) {
        return BindingBuilder.bind(reviewQueue)
                .to(reviewExchange)
                .with(REVIEW_ROUTING_KEY);
    }

    @Bean
    public Binding reviewDlxBinding(Queue reviewDlxQueue, DirectExchange reviewDlxExchange) {
        return BindingBuilder.bind(reviewDlxQueue)
                .to(reviewDlxExchange)
                .with("review.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(10);
        factory.setMaxConcurrentConsumers(20);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}