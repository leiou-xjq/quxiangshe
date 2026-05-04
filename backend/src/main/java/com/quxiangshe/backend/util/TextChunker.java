package com.quxiangshe.backend.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TextChunker {
    
    private static final int DEFAULT_CHUNK_SIZE = 200;
    private static final int DEFAULT_OVERLAP = 20;
    
    @Data
    public static class Chunk {
        private String text;
        private int index;
        private String sourceId;
        
        public Chunk() {}
        
        public Chunk(String text, int index) {
            this.text = text;
            this.index = index;
        }
        
        public Chunk(String text, int index, String sourceId) {
            this.text = text;
            this.index = index;
            this.sourceId = sourceId;
        }
    }
    
    public static List<Chunk> chunkBySentence(String text) {
        return chunkBySentence(text, null);
    }
    
    public static List<Chunk> chunkBySentence(String text, String sourceId) {
        List<Chunk> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        text = text.trim();
        
        if (text.length() <= DEFAULT_CHUNK_SIZE) {
            chunks.add(new Chunk(text, 0, sourceId));
            return chunks;
        }
        
        String[] sentences = splitBySentence(text);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > DEFAULT_CHUNK_SIZE) {
                if (currentChunk.length() > 0) {
                    chunks.add(new Chunk(currentChunk.toString().trim(), chunkIndex++, sourceId));
                    
                    String overlapText = getLastNChars(currentChunk.toString(), DEFAULT_OVERLAP);
                    currentChunk = new StringBuilder(overlapText);
                }
            }
            currentChunk.append(sentence).append(" ");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(new Chunk(currentChunk.toString().trim(), chunkIndex, sourceId));
        }
        
        return chunks;
    }
    
    public static List<Chunk> chunkByLength(String text, int chunkSize, int overlap) {
        return chunkByLength(text, chunkSize, overlap, null);
    }
    
    public static List<Chunk> chunkByLength(String text, int chunkSize, int overlap, String sourceId) {
        List<Chunk> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        text = text.trim();
        
        if (text.length() <= chunkSize) {
            chunks.add(new Chunk(text, 0, sourceId));
            return chunks;
        }
        
        int step = chunkSize - overlap;
        int index = 0;
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunkText = text.substring(start, end);
            
            if (!chunkText.isEmpty()) {
                chunks.add(new Chunk(chunkText.trim(), index++, sourceId));
            }
            
            start += step;
        }
        
        return chunks;
    }
    
    private static String[] splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            
            if (isSentenceEnd(c, i, text)) {
                String sentence = sb.toString().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                sb = new StringBuilder();
            }
        }
        
        String remaining = sb.toString().trim();
        if (!remaining.isEmpty()) {
            sentences.add(remaining);
        }
        
        return sentences.toArray(new String[0]);
    }
    
    private static boolean isSentenceEnd(char c, int index, String text) {
        if (c != '.' && c != '。' && c != '!' && c != '！' && c != '?' && c != '？' && c != ';') {
            return false;
        }
        
        if (index + 1 >= text.length()) {
            return true;
        }
        
        char next = text.charAt(index + 1);
        return !Character.isDigit(next) && !Character.isLetter(next);
    }
    
    private static String getLastNChars(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        return text.substring(text.length() - n);
    }
    
    public static String joinChunks(List<Chunk> chunks) {
        return chunks.stream()
            .map(Chunk::getText)
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    }
}