package com.quxiangshe.comment.service.impl;

import com.quxiangshe.comment.entity.SensitiveWordEntity;
import com.quxiangshe.comment.mapper.SensitiveWordMapper;
import com.quxiangshe.comment.service.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 敏感词服务实现类
 * 使用DFA（确定有限自动机）算法实现敏感词高效匹配
 * 
 * DFA算法原理：
 * - 预构建敏感词字典树，每个节点代表一个字符
 * - 匹配时沿字符串遍历字典树，时间复杂度O(n)
 * - 支持同时匹配多个敏感词
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    /**
     * 敏感词Mapper
     */
    private final SensitiveWordMapper sensitiveWordMapper;

    /**
     * DFA字典树根节点
     */
    private Map<Character, Object> sensitiveWordMap;

    /**
     * 替换字符
     */
    private static final char REPLACE_CHAR = '*';

    /**
     * 敏感词数量缓存
     */
    private volatile int sensitiveWordCount = 0;

    /**
     * 初始化时加载敏感词库
     */
    @PostConstruct
    public void initSensitiveWords() {
        log.info("开始加载敏感词库...");
        try {
            List<SensitiveWordEntity> words = sensitiveWordMapper.selectList(null);
            if (words == null || words.isEmpty()) {
                log.warn("敏感词表为空，使用空词库");
                sensitiveWordMap = new HashMap<>();
                sensitiveWordCount = 0;
            } else {
                buildSensitiveWordTree(words);
                sensitiveWordCount = words.size();
                log.info("敏感词库加载完成，共加载 {} 个敏感词", sensitiveWordCount);
            }
        } catch (Exception e) {
            log.error("加载敏感词库失败，使用空词库: {}", e.getMessage());
            sensitiveWordMap = new HashMap<>();
            sensitiveWordCount = 0;
        }
    }

    /**
     * 构建敏感词DFA字典树
     *
     * @param words 敏感词列表
     */
    private void buildSensitiveWordTree(List<SensitiveWordEntity> words) {
        // 创建新的字典树
        Map<Character, Object> newMap = new HashMap<>();

        for (SensitiveWordEntity entity : words) {
            String word = entity.getWord();
            if (word == null || word.isEmpty()) {
                continue;
            }

            // 将敏感词转换为字符数组
            char[] chars = word.toCharArray();
            Map<Character, Object> currentMap = newMap;

            // 遍历每个字符，构建字典树
            for (char c : chars) {
                // 将字符转换为小写（不区分大小写）
                c = Character.toLowerCase(c);

                // 获取当前节点
                Object nextMap = currentMap.get(c);

                if (nextMap == null) {
                    // 创建新节点
                    Map<Character, Object> tempMap = new HashMap<>();
                    // 在最后一个字符节点标记为敏感词结束
                    tempMap.put((char) 0, null);
                    currentMap.put(c, tempMap);
                    currentMap = tempMap;
                } else {
                    currentMap = (Map<Character, Object>) nextMap;
                }
            }
        }

        this.sensitiveWordMap = newMap;
    }

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true表示包含敏感词
     */
    @Override
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty() || sensitiveWordCount == 0) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            if (checkSensitiveWord(text, i) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * 替换敏感词为指定字符
     *
     * @param text 待处理文本
     * @return 替换后的文本
     */
    @Override
    public String replaceSensitiveWord(String text) {
        if (text == null || text.isEmpty() || sensitiveWordCount == 0) {
            return text;
        }

        StringBuilder result = new StringBuilder(text);
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                // 替换敏感词为 * 号
                for (int j = 0; j < length; j++) {
                    if (i + j < result.length()) {
                        result.setCharAt(i + j, REPLACE_CHAR);
                    }
                }
                // 跳过敏感词长度
                i += length - 1;
            }
        }

        return result.toString();
    }

    /**
     * 获取文本中所有敏感词
     *
     * @param text 待检测文本
     * @return 敏感词集合
     */
    @Override
    public Set<String> findSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();

        if (text == null || text.isEmpty() || sensitiveWordCount == 0) {
            return sensitiveWords;
        }

        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                sensitiveWords.add(text.substring(i, i + length));
                // 跳过敏感词长度
                i += length - 1;
            }
        }

        return sensitiveWords;
    }

    /**
     * 检查指定位置开始的敏感词长度
     * 使用DFA算法进行匹配
     *
     * @param text 文本
     * @param beginIndex 开始位置
     * @return 敏感词长度，0表示不是敏感词
     */
    private int checkSensitiveWord(String text, int beginIndex) {
        // 敏感词结束标记
        char endFlag = (char) 0;

        Map<Character, Object> currentMap = sensitiveWordMap;
        int matchLength = 0;
        int tempLength = 0;

        for (int i = beginIndex; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            Object nextMap = currentMap.get(c);

            if (nextMap == null) {
                break;
            }

            currentMap = (Map<Character, Object>) nextMap;
            tempLength++;

            // 检查是否是敏感词结束
            if (currentMap.get(endFlag) != null) {
                matchLength = tempLength;
            }
        }

        return matchLength;
    }

    /**
     * 重新加载敏感词库
     */
    @Override
    public void reloadSensitiveWords() {
        try {
            List<SensitiveWordEntity> words = sensitiveWordMapper.selectList(null);
            buildSensitiveWordTree(words);
            sensitiveWordCount = words.size();
            log.info("敏感词库重新加载完成，共加载 {} 个敏感词", sensitiveWordCount);
        } catch (Exception e) {
            log.error("重新加载敏感词库失败", e);
        }
    }

    /**
     * 获取敏感词数量
     *
     * @return 敏感词总数
     */
    @Override
    public int getSensitiveWordCount() {
        return sensitiveWordCount;
    }
}
