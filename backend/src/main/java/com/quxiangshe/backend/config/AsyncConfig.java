package com.quxiangshe.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>为不同业务场景定义独立的线程池，实现任务隔离和资源管控：
 * <ul>
 *   <li><b>pushExecutor</b>：Feed流推送，核心线程5，最大10</li>
 *   <li><b>reviewExecutor</b>：内容审核，核心线程3，最大8（支持更大队列）</li>
 *   <li><b>feedDistributeExecutor</b>：Feed智能分发，核心线程2，最大5</li>
 * </ul>
 * 拒绝策略统一采用CallerRunsPolicy，任务量过大时由调用线程执行，保证不丢失任务。</p>
 * 
 * @author 趣享社技术团队
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Feed推送任务线程池
     * 用于发布笔记后向粉丝Feed流异步推送，核心线程5，队列容量200。
     * 
     * @return 推送专用线程池
     */
    @Bean("pushExecutor")
    public Executor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 内容审核任务线程池
     * 用于笔记/评论敏感词审核，独立于推送线程池，队列容量500（应对突发审核）。
     * 
     * @return 审核专用线程池
     */
    @Bean("reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("review-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Feed智能分发线程池
     * 用于按粉丝活跃度优先级分批推送，核心线程2保证顺序性，队列容量100。
     * 
     * @return Feed分发专用线程池
     */
    @Bean("feedDistributeExecutor")
    public Executor feedDistributeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("feed-dist-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}