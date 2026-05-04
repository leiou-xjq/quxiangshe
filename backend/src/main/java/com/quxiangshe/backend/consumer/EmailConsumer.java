package com.quxiangshe.backend.consumer;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.EmailMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmail(EmailMessage message, Message amqpMessage, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到邮件发送消息: email={}, subject={}", message.getEmail(), message.getSubject());

        int retryCount = getRetryCount(amqpMessage);

        try {
            sendEmail(message);

            channel.basicAck(deliveryTag, false);
            log.info("邮件发送成功: email={}", message.getEmail());

        } catch (Exception e) {
            log.error("邮件发送失败: email={}, retryCount={}, error={}",
                    message.getEmail(), retryCount, e.getMessage(), e);

            handleFailure(channel, amqpMessage, deliveryTag, retryCount, e);
        }
    }

    private void sendEmail(EmailMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(message.getEmail());
        mailMessage.setSubject(message.getSubject());
        mailMessage.setText(message.getContent());

        mailSender.send(mailMessage);
    }

    private void handleFailure(Channel channel, Message amqpMessage, long deliveryTag,
                               int retryCount, Exception e) {
        try {
            if (retryCount < MAX_RETRY_COUNT) {
                String retryHeader = String.valueOf(retryCount + 1);
                amqpMessage.getMessageProperties().setHeader("x-retry-count", retryHeader);

                channel.basicNack(deliveryTag, false, true);
                log.warn("邮件发送重试: retryCount={}", retryCount + 1);
            } else {
                channel.basicNack(deliveryTag, false, false);
                log.error("邮件发送超过最大重试次数，丢弃消息: retryCount={}", retryCount);
            }
        } catch (Exception ackEx) {
            log.error("确认消息失败时出现异常: {}", ackEx.getMessage(), ackEx);
        }
    }

    private int getRetryCount(Message message) {
        Object retryHeader = message.getMessageProperties().getHeader("x-retry-count");
        if (retryHeader != null) {
            try {
                return Integer.parseInt(retryHeader.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}