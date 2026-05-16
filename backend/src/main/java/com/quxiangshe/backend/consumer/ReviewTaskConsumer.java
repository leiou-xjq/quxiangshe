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
 * 通过MQ消费审核任务，保证可靠性和幂等性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewTaskConsumer {

    private final NoteReviewMapper noteReviewMapper;
    private final ReviewAsyncTask reviewAsyncTask;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_QUEUE)
    public void consumeReviewTask(ReviewTaskMessage message,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到审核任务: noteId={}, userId={}", message.getNoteId(), message.getUserId());

        try {
            // 幂等检查：已审核过的笔记直接跳过
            if (isAlreadyReviewed(message.getNoteId())) {
                log.info("笔记已审核，跳过: noteId={}", message.getNoteId());
                return;
            }

            // 执行审核
            reviewAsyncTask.asyncReview(
                    message.getNoteId(),
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent(),
                    message.getImageUrls()
            );

            log.info("审核任务完成: noteId={}", message.getNoteId());

        } catch (Exception e) {
            log.error("审核任务执行失败: noteId={}, error={}", message.getNoteId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 幂等检查：检查笔记是否已审核
     */
    private boolean isAlreadyReviewed(Long noteId) {
        if (noteId == null) {
            return false;
        }
        NoteReview existing = noteReviewMapper.selectByNoteId(noteId);
        return existing != null && existing.getReviewStatus() != 0;
    }
}
