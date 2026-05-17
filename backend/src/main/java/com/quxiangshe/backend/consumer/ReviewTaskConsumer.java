package com.quxiangshe.backend.consumer;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.ReviewTaskMessage;
import com.quxiangshe.backend.entity.NoteReview;
import com.quxiangshe.backend.mapper.NoteReviewMapper;
import com.quxiangshe.backend.task.ReviewAsyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 审核任务消费者
 * 
 * <p>监听RabbitMQ审核队列，消费待审核的笔记任务，调用异步审核服务执行内容审核。
 * 具备幂等性保障：已审核通过的笔记不再重复审核，
 * 消息消费失败时由RabbitMQ重试机制保证最终一致性。</p>
 * 
 * @author 趣享社技术团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewTaskConsumer {

    private final NoteReviewMapper noteReviewMapper;
    private final ReviewAsyncTask reviewAsyncTask;

    /**
     * 消费审核任务消息
     * 
     * <p>处理流程：幂等检查 → 调用异步审核 → 审核完成后更新数据库状态。
     * 抛出异常时RabbitMQ会自动重新投递（需配置手动确认模式配合）。</p>
     *
     * @param message     审核任务消息体，包含笔记内容与作者信息
     * @param deliveryTag RabbitMQ投递标签，用于手动确认
     */
    @RabbitListener(queues = RabbitMQConfig.REVIEW_QUEUE)
    public void consumeReviewTask(ReviewTaskMessage message,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到审核任务: noteId={}, userId={}", message.getNoteId(), message.getUserId());

        try {
            // 幂等检查：已审核过的笔记直接跳过，不重复执行
            if (isAlreadyReviewed(message.getNoteId())) {
                log.info("笔记已审核，跳过: noteId={}", message.getNoteId());
                return;
            }

            // 调用异步审核服务执行实际审核逻辑
            reviewAsyncTask.asyncReview(
                    message.getNoteId(),
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent(),
                    message.getImageUrls()
            );

            log.info("审核任务完成: noteId={}", message.getNoteId());

        } catch (Exception e) {
            // 抛出异常触发RabbitMQ重试机制
            log.error("审核任务执行失败: noteId={}, error={}", message.getNoteId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 幂等性检查：判断笔记是否已经完成审核
     * 
     * <p>通过查询审核记录表判断：若存在状态不为"待审核(0)"的记录，
     * 说明该笔记已经审核过，无需重复处理。</p>
     *
     * @param noteId 笔记ID
     * @return true-已审核（应跳过），false-未审核（需处理）
     */
    private boolean isAlreadyReviewed(Long noteId) {
        if (noteId == null) {
            return false;
        }
        NoteReview existing = noteReviewMapper.selectByNoteId(noteId);
        // reviewStatus != 0 表示已通过审核或已拒绝
        return existing != null && existing.getReviewStatus() != 0;
    }
}
