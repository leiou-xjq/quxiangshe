package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quxiangshe.backend.entity.Blacklist;
import com.quxiangshe.backend.mapper.BlacklistMapper;
import com.quxiangshe.backend.service.IBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 黑名单服务实现类
 * 
 * @author 趣享社技术团队
 */
@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements IBlacklistService {
    
    private final BlacklistMapper blacklistMapper;
    
    @Override
    public boolean blockUser(Long userId, Long blockedId) {
        if (userId.equals(blockedId)) {
            throw new RuntimeException("不能拉黑自己");
        }
        
        if (isBlocked(userId, blockedId)) {
            return true;
        }
        
        Blacklist blacklist = new Blacklist();
        blacklist.setUserId(userId);
        blacklist.setBlockedId(blockedId);
        
        return blacklistMapper.insert(blacklist) > 0;
    }
    
    @Override
    public boolean unblockUser(Long userId, Long blockedId) {
        QueryWrapper<Blacklist> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("blocked_id", blockedId);
        
        return blacklistMapper.delete(wrapper) > 0;
    }
    
    @Override
    public boolean isBlocked(Long userId, Long blockedId) {
        return blacklistMapper.isBlocked(userId, blockedId);
    }
    
    @Override
    public List<Long> getBlockedUserIds(Long userId) {
        QueryWrapper<Blacklist> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        
        List<Blacklist> blacklists = blacklistMapper.selectList(wrapper);
        return blacklists.stream()
                .map(Blacklist::getBlockedId)
                .collect(Collectors.toList());
    }
}