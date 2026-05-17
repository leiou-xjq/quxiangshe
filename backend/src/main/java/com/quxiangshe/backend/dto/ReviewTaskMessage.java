package com.quxiangshe.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 审核任务消息DTO，用于通过消息队列（MQ）投递笔记审核任务。
 * <p>
 * 当用户发布笔记后，系统生成审核任务消息投递到MQ，
 * 审核服务消费消息后依次执行三层审核流程（敏感词→RAG相似度→大模型判定），
 * 保证异步审核的可靠性和解耦。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待审核的笔记ID */
    private Long noteId;

    /** 笔记发布者用户ID */
    private Long userId;

    /** 笔记标题 */
    private String title;

    /** 笔记文本内容 */
    private String content;

    /** 图片URL列表 */
    private List<String> imageUrls;

    /** 提交审核的时间戳 */
    private Long submitTime;
}
