package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.EmailMessage;
import com.quxiangshe.backend.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 邮件服务实现类
 * 使用QQ邮箱SMTP发送验证码
 *
 * @author 理享技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${mail.expire-seconds:300}")
    private int expireSeconds;

    private static final String VERIFY_CODE_KEY_PREFIX = "email:verify:";
    private static final int CODE_LENGTH = 6;

    @Override
    public boolean sendVerifyCode(String email) {
        try {
            if (!isValidEmail(email)) {
                log.warn("邮箱格式不正确: {}", email);
                return false;
            }

            String code = generateCode();

            // 先存储验证码到Redis
            storeCode(email, code);

            // 异步发送邮件 (MQ)
            if (rabbitTemplate != null) {
                EmailMessage emailMsg = EmailMessage.builder()
                        .email(email)
                        .code(code)
                        .subject("【趣享社】验证码")
                        .content(buildEmailContent(code))
                        .timestamp(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE,
                        RabbitMQConfig.EMAIL_ROUTING_KEY, emailMsg);
                log.info("邮件发送消息已投递到MQ: {}", email);
            } else {
                // 如果RabbitMQ不可用，同步发送
                sendEmailDirect(email, code);
            }

            return true;
        } catch (Exception e) {
            log.error("发送验证码失败: {}", email, e);
            return false;
        }
    }

    private void sendEmailDirect(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("【趣享社】验证码");
        message.setText(buildEmailContent(code));
        mailSender.send(message);
        log.info("验证码已发送到邮箱: {}", email);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String key = VERIFY_CODE_KEY_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("验证码已过期或不存在: {}", email);
            return false;
        }

        if (storedCode.equals(code)) {
            redisTemplate.delete(key);
            log.info("邮箱验证码验证成功: {}", email);
            return true;
        }

        log.warn("邮箱验证码错误: {}, 输入: {}, 存储: {}", email, code, storedCode);
        return false;
    }

    private boolean isValidEmail(String email) {
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(regex);
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void storeCode(String email, String code) {
        String key = VERIFY_CODE_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
    }

    private String buildEmailContent(String code) {
        return "您好！\n\n" +
                "您的验证码是：" + code + "\n\n" +
                "验证码有效期为5分钟，请勿泄露给他人。\n\n" +
                "如果这不是您的操作，请忽略此邮件。\n\n" +
                "--- 理享团队";
    }
}