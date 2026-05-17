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

/**
 * RabbitMQ消息队列配置
 * <p>定义所有业务消息队列的Exchange、Queue、Binding及死信队列（DLX）。
 * 采用DirectExchange路由模式，每个业务域一组（Exchange + Queue + DLX），
 * 通过 @ConditionalOnProperty 支持条件加载（rabbitmq.enabled=true）。</p>
 * <p>业务域划分：
 * <ul>
 *   <li>feed：Feed流推送队列，笔记发布后异步分发</li>
 *   <li>private-message：私信聊天消息队列</li>
 *   <li>notification：通知消息队列（点赞/评论/关注）</li>
 *   <li>email：邮件发送队列</li>
 *   <li>video：视频转码任务队列</li>
 *   <li>review：内容审核任务队列</li>
 * </ul>
 * 消息格式：JSON（Jackson2JsonMessageConverter），消费确认：手动ACK（MANUAL模式）。</p>
 * 
 * @author 趣享社技术团队
 */
@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {
    
    // ========== Feed流推送队列（发布笔记 -> 粉丝Feed）==========
    
    /** Feed推送交换机，笔记发布事件入口 */
    public static final String FEED_PUSH_EXCHANGE = "feed.push.exchange";
    /** Feed推送主队列，消费者拉取后写入粉丝Feed收件箱 */
    public static final String FEED_PUSH_QUEUE = "feed.push.queue.v2";
    /** Feed推送路由键 */
    public static final String FEED_PUSH_ROUTING_KEY = "feed.push";
    
    /** Feed死信交换机，处理消费失败的消息 */
    public static final String FEED_DLX_EXCHANGE = "feed.push.exchange.dlx";
    /** Feed死信队列，收集失败消息用于排查 */
    public static final String FEED_DLX_QUEUE = "feed.push.queue.dlx";
    
    // ========== 私信队列 ==========
    
    /** 私信交换机 */
    public static final String PRIVATE_MESSAGE_EXCHANGE = "quxiangshe.private-message.exchange";
    /** 私信主队列 */
    public static final String PRIVATE_MESSAGE_QUEUE = "quxiangshe.private-message.queue";
    /** 私信路由键 */
    public static final String PRIVATE_MESSAGE_ROUTING_KEY = "private.message";

    // ========== 通知队列（点赞/评论/关注通知）==========
    
    /** 通知交换机 */
    public static final String NOTIFICATION_EXCHANGE = "quxiangshe.notification.exchange";
    /** 通知主队列 */
    public static final String NOTIFICATION_QUEUE = "quxiangshe.notification.queue";
    /** 通知路由键 */
    public static final String NOTIFICATION_ROUTING_KEY = "notification.create";
    /** 通知死信交换机 */
    public static final String NOTIFICATION_DLX_EXCHANGE = "quxiangshe.notification.exchange.dlx";
    /** 通知死信队列 */
    public static final String NOTIFICATION_DLX_QUEUE = "quxiangshe.notification.queue.dlx";

    // ========== 邮件队列 ==========
    
    /** 邮件交换机 */
    public static final String EMAIL_EXCHANGE = "quxiangshe.email.exchange";
    /** 邮件主队列 */
    public static final String EMAIL_QUEUE = "quxiangshe.email.queue";
    /** 邮件路由键 */
    public static final String EMAIL_ROUTING_KEY = "email.send";
    /** 邮件死信交换机 */
    public static final String EMAIL_DLX_EXCHANGE = "quxiangshe.email.exchange.dlx";
    /** 邮件死信队列 */
    public static final String EMAIL_DLX_QUEUE = "quxiangshe.email.queue.dlx";

    // ========== 视频转码队列 ==========
    
    /** 视频交换机 */
    public static final String VIDEO_EXCHANGE = "quxiangshe.video.exchange";
    /** 视频转码主队列 */
    public static final String VIDEO_TRANSCODE_QUEUE = "quxiangshe.video.transcode.queue";
    /** 视频路由键 */
    public static final String VIDEO_ROUTING_KEY = "video.transcode";
    /** 视频死信交换机 */
    public static final String VIDEO_DLX_EXCHANGE = "quxiangshe.video.exchange.dlx";
    /** 视频死信队列 */
    public static final String VIDEO_DLX_QUEUE = "quxiangshe.video.queue.dlx";

    // ========== 内容审核队列 ==========
    
    /** 审核交换机 */
    public static final String REVIEW_EXCHANGE = "quxiangshe.review.exchange";
    /** 审核主队列 */
    public static final String REVIEW_QUEUE = "quxiangshe.review.queue";
    /** 审核路由键 */
    public static final String REVIEW_ROUTING_KEY = "review.task";
    /** 审核死信交换机 */
    public static final String REVIEW_DLX_EXCHANGE = "quxiangshe.review.exchange.dlx";
    /** 审核死信队列 */
    public static final String REVIEW_DLX_QUEUE = "quxiangshe.review.queue.dlx";
    
    // ========== Feed推送 Exchange/Queue/Binding ==========
    
    @Bean
    public DirectExchange feedPushExchange() {
        // durable=true（持久化交换机，服务重启不丢失），autoDelete=false
        return new DirectExchange(FEED_PUSH_EXCHANGE, true, false);
    }
    
    @Bean
    public DirectExchange feedDlxExchange() {
        return new DirectExchange(FEED_DLX_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue feedPushQueue() {
        // 绑定死信交换机：消息消费失败/超时自动转发到DLX
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
    
    // ========== 私信 Exchange/Queue/Binding ==========
    
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

    // ========== 通知 Exchange/Queue/Binding ==========

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

    // ========== 邮件 Exchange/Queue/Binding ==========

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

    // ========== 视频转码 Exchange/Queue/Binding ==========

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

    // ========== 审核任务 Exchange/Queue/Binding ==========
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

    // ========== 消息转换器与RabbitTemplate ==========

    /**
     * JSON消息转换器
     * 将Java对象序列化为JSON字节流，兼容各语言消费端。
     * 
     * @return Jackson2JsonMessageConverter实例
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * 生产者RabbitTemplate
     * 设置JSON转换器和mandatory模式（消息无法路由时返回给生产者）。
     * 
     * @param connectionFactory RabbitMQ连接工厂
     * @return 配置后的RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // mandatory=true：消息无法投递时触发ReturnCallback而非静默丢弃
        template.setMandatory(true);
        return template;
    }

    /**
     * 消费者监听容器工厂
     * 配置并发消费者（10-20）、预取数量（10）和手动ACK模式。
     * 
     * @param connectionFactory RabbitMQ连接工厂
     * @return SimpleRabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // 并发消费者数：最少10个，峰值20个
        factory.setConcurrentConsumers(10);
        factory.setMaxConcurrentConsumers(20);
        // 预取数量：每个消费者一次预取10条消息
        factory.setPrefetchCount(10);
        // 消费失败不回退到队尾（避免死循环），直接进入DLX
        factory.setDefaultRequeueRejected(false);
        // 手动ACK模式：业务处理成功后显式确认
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}