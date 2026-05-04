package com.quxiangshe.comment.service;

import java.util.Set;

/**
 * 敏感词服务接口
 * 提供敏感词检测和替换功能
 */
public interface SensitiveWordService {

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true表示包含敏感词
     */
    boolean containsSensitiveWord(String text);

    /**
     * 替换敏感词为指定字符
     * 默认替换为 * 号
     *
     * @param text 待处理文本
     * @return 替换后的文本
     */
    String replaceSensitiveWord(String text);

    /**
     * 获取文本中所有敏感词
     *
     * @param text 待检测文本
     * @return 敏感词集合
     */
    Set<String> findSensitiveWords(String text);

    /**
     * 重新加载敏感词库
     * 用于动态更新敏感词
     */
    void reloadSensitiveWords();

    /**
     * 获取敏感词数量
     *
     * @return 敏感词总数
     */
    int getSensitiveWordCount();
}
