import request from './request'

/**
 * 举报目标类型
 */
export const TargetType = {
  NOTE: 1,
  COMMENT: 2,
  USER: 3
}

/**
 * 举报原因
 */
export const ReportReason = {
  SPAM: 1,        // 垃圾广告
  PORN: 2,        // 涉黄
  PLAGIARISM: 3,  // 抄袭
  OTHER: 4        // 其他
}

/**
 * 提交举报
 * @param {number} targetType - 目标类型 (1-笔记 2-评论 3-用户)
 * @param {number} targetId - 目标ID
 * @param {number} reason - 举报原因
 * @param {string} description - 详细描述
 */
export function submitReport(targetType, targetId, reason, description = '') {
  return request({
    url: '/report',
    method: 'post',
    params: { targetType, targetId, reason, description }
  })
}