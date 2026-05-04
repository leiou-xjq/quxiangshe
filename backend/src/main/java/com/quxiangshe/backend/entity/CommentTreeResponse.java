package com.quxiangshe.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentTreeResponse {
    
    @Builder.Default
    private List<CommentTreeVO> roots = new ArrayList<>();
    
    private int totalRoots;
    
    private String cursor;
}