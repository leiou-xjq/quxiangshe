package com.quxiangshe.auth.service.impl;

import com.quxiangshe.auth.dto.*;
import com.quxiangshe.auth.entity.UserEntity;
import com.quxiangshe.auth.mapper.AuthUserMapper;
import com.quxiangshe.auth.service.AuthService;
import com.quxiangshe.common.exception.BusinessException;
import com.quxiangshe.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * 实现用户注册、登录、验证码、令牌刷新等核心功能
 * 
 * 主要功能:
 * 1. 用户注册 - 密码加密存储，唯一性校验
 * 2. 密码登录 - 支持用户名/手机号/邮箱登录
 * 3. 验证码登录 - 手机号/邮箱验证码登录
 * 4. 令牌管理 - JWT双令牌，Redis存储RefreshToken
 * 5. 登出 - 清除RefreshToken
 * 
 * @author quxiangshe
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis中RefreshToken的Key前缀
     * 完整Key格式: refresh:{userId}
     * 用于存储用户RefreshToken，实现登出失效和有效期管理
     */
    private static final String REFRESH_TOKEN_KEY = "refresh:";

    /**
     * 验证码Key前缀
     * 手机验证码: verifycode:{phone}
     * 邮箱验证码: verifycode:email:{email}
     */
    private static final String VERIFY_CODE_KEY = "verifycode:";

    /**
     * 验证码有效期（秒）
     * 5分钟 = 300秒
     */
    private static final long VERIFY_CODE_TTL = 300;

    /**
     * 验证码长度
     * 6位数字验证码
     */
    private static final int CODE_LENGTH = 6;

    /**
     * 用户注册
     * 
     * 处理流程:
     * 1. 校验用户名唯一性
     * 2. 校验手机号唯一性（如提供）
     * 3. 校验邮箱唯一性（如提供）
     * 4. 创建用户记录（密码使用BCrypt加密）
     * 5. 返回用户ID和用户名
     * 
     * @param request 注册请求（用户名、密码、手机号、邮箱、昵称）
     * @return RegisterResponse (userId, username)
     * @throws BusinessException 用户名/手机号/邮箱已存在，或注册失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequestDTO request) {
        try {
            // 1. 校验用户名唯一性
            Long count = userMapper.countByUsername(request.getUsername());
            if (count != null && count > 0) {
                throw new BusinessException("用户名已存在");
            }

            // 2. 校验手机号唯一性（如果提供了手机号）
            if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                Long phoneCount = userMapper.countByPhone(request.getPhone());
                if (phoneCount != null && phoneCount > 0) {
                    throw new BusinessException("手机号已被注册");
                }
            }

            // 3. 校验邮箱唯一性（如果提供了邮箱）
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                Long emailCount = userMapper.countByEmail(request.getEmail());
                if (emailCount != null && emailCount > 0) {
                    throw new BusinessException("邮箱已被注册");
                }
            }

            // 4. 创建用户（密码使用BCrypt加密存储）
            UserEntity user = UserEntity.builder()
                    .username(request.getUsername())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                    .status(UserEntity.STATUS_NORMAL)
                    .build();

            userMapper.insert(user);

            log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

            return new RegisterResponse(user.getId(), user.getUsername());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage(), e);
            throw new BusinessException("注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户密码登录
     * 
     * 处理流程:
     * 1. 根据用户名/手机号/邮箱查询用户
     * 2. 校验用户存在性
     * 3. 校验用户状态（是否被禁用）
     * 4. 校验密码
     * 5. 更新最后登录时间
     * 6. 生成JWT双令牌
     * 7. 存储RefreshToken到Redis
     * 
     * @param request 登录请求（用户名/手机号/邮箱、密码）
     * @return LoginResponseDTO (accessToken, refreshToken, expiresIn, userId, username)
     * @throws BusinessException 用户不存在、密码错误、用户已被禁用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. 根据用户名查询用户
        UserEntity user = userMapper.selectByUsername(request.getUsername());
        
        // 如果不是用户名，尝试手机号查询
        if (user == null) {
            user = userMapper.selectByPhone(request.getUsername());
        }
        // 如果不是手机号，尝试邮箱查询
        if (user == null) {
            user = userMapper.selectByEmail(request.getUsername());
        }

        // 2. 校验用户存在性
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 校验用户状态
        if (user.getStatus() == UserEntity.STATUS_DISABLED) {
            throw new BusinessException("用户已被禁用");
        }

        // 4. 校验密码（BCrypt加密比对）
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 5. 更新最后登录时间
        userMapper.updateLastLoginTime(user.getId());

        // 6. 生成JWT令牌
        // AccessToken: 包含用户ID、用户名、角色信息，30分钟有效期
        // RefreshToken: 仅包含用户ID，7天有效期
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 7. 存储RefreshToken到Redis
        // Key: refresh:{userId}
        // Value: RefreshToken
        // TTL: 7天（604800秒）
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + user.getId(),
                refreshToken,
                jwtUtil.getRefreshTokenValidity(),
                TimeUnit.SECONDS
        );

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenValidity())
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    /**
     * 刷新AccessToken
     * 
     * 处理流程:
     * 1. 验证RefreshToken格式和签名
     * 2. 验证Token类型（必须是refresh类型）
     * 3. 提取用户ID
     * 4. 验证Redis中存储的RefreshToken（防止Token泄露后被滥用）
     * 5. 查询用户状态
     * 6. 生成新的AccessToken
     * 
     * @param request 刷新请求（refreshToken）
     * @return RefreshTokenResponse (accessToken, expiresIn)
     * @throws BusinessException Token无效、已失效、用户不存在或已被禁用
     */
    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequestDTO request) {
        // 1. 验证RefreshToken（格式、签名、过期时间）
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new BusinessException("刷新令牌无效");
        }

        // 2. 验证Token类型（必须是refresh类型）
        String tokenType = jwtUtil.extractTokenType(request.getRefreshToken());
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException("刷新令牌无效");
        }

        // 3. 获取用户ID
        String userIdStr = jwtUtil.extractUserId(request.getRefreshToken());
        if (userIdStr == null) {
            throw new BusinessException("刷新令牌无效");
        }
        Long userId = Long.parseLong(userIdStr);

        // 4. 验证Redis中的RefreshToken
        // 防止Token泄露后从其他设备刷新
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + userId);
        if (storedToken == null || !storedToken.equals(request.getRefreshToken())) {
            throw new BusinessException("刷新令牌已失效");
        }

        // 5. 查询用户信息
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == UserEntity.STATUS_DISABLED) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        // 6. 生成新的AccessToken
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), "USER");

        log.info("刷新AccessToken: userId={}", user.getId());

        return new RefreshTokenResponse(accessToken, jwtUtil.getAccessTokenValidity());
    }

    /**
     * 用户登出
     * 
     * 删除Redis中存储的RefreshToken，使RefreshToken失效
     * 注意: 不删除AccessToken，因为AccessToken会自然过期
     * 
     * @param userId 用户ID
     */
    @Override
    public void logout(Long userId) {
        // 删除Redis中的RefreshToken
        redisTemplate.delete(REFRESH_TOKEN_KEY + userId);
        log.info("用户登出: userId={}", userId);
    }

    /**
     * 手机号验证码登录
     * 
     * 处理流程:
     * 1. 校验验证码（从Redis获取并比对）
     * 2. 验证成功后删除验证码（防止重复使用）
     * 3. 查询用户
     * 4. 校验用户状态
     * 5. 更新最后登录时间
     * 6. 生成JWT双令牌
     * 7. 存储RefreshToken到Redis
     * 
     * @param request 手机号登录请求（phone, code）
     * @return LoginResponseDTO
     * @throws BusinessException 验证码错误/过期、用户不存在/已被禁用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDTO phoneLogin(PhoneLoginRequestDTO request) {
        // 1. 校验验证码
        String storedCode = redisTemplate.opsForValue().get(VERIFY_CODE_KEY + request.getPhone());
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        
        // 2. 验证通过后删除验证码（防重复使用）
        redisTemplate.delete(VERIFY_CODE_KEY + request.getPhone());
        
        // 3. 查询用户
        UserEntity user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
        
        // 4. 校验用户状态
        if (user.getStatus() == UserEntity.STATUS_DISABLED) {
            throw new BusinessException("用户已被禁用");
        }
        
        // 5. 更新最后登录时间
        userMapper.updateLastLoginTime(user.getId());
        
        // 6. 生成JWT令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // 7. 存储RefreshToken到Redis
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + user.getId(),
                refreshToken,
                jwtUtil.getRefreshTokenValidity(),
                TimeUnit.SECONDS
        );
        
        log.info("手机号登录成功: userId={}, phone={}", user.getId(), request.getPhone());
        
        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenValidity())
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    /**
     * 发送手机验证码
     * 
     * 生成6位数字验证码，存储到Redis
     * 验证码有效期5分钟，过期后自动失效
     * 
     * 注意: 实际项目中应调用短信服务发送验证码
     * 当前仅打印验证码用于测试
     * 
     * @param request 发送验证码请求（phone）
     * @return "验证码已发送"
     * @throws BusinessException 发送失败
     */
    @Override
    public String sendVerifyCode(SendCodeRequestDTO request) {
        String phone = request.getPhone();
        
        // 生成6位验证码（000000-999999）
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // 存储验证码到Redis，有效期5分钟
        redisTemplate.opsForValue().set(
                VERIFY_CODE_KEY + phone,
                code,
                VERIFY_CODE_TTL,
                TimeUnit.SECONDS
        );
        
        log.info("验证码已发送: phone={}, code={}", phone, code);
        
        // TODO: 实际项目中应该调用短信服务发送验证码
        // 这里仅打印验证码，实际环境需要对接短信平台
        
        return "验证码已发送";
    }

    /**
     * 检查手机号是否已注册
     * 
     * @param phone 手机号
     * @return true-已注册, false-未注册
     */
    @Override
    public boolean checkPhoneExists(String phone) {
        Long count = userMapper.countByPhone(phone);
        return count != null && count > 0;
    }

    /**
     * 检查邮箱是否已注册
     * 
     * @param email 邮箱
     * @return true-已注册, false-未注册
     */
    @Override
    public boolean checkEmailExists(String email) {
        Long count = userMapper.countByEmail(email);
        return count != null && count > 0;
    }

    /**
     * 检查用户名是否已注册
     * 
     * @param username 用户名
     * @return true-已注册, false-未注册
     */
    @Override
    public boolean checkUsernameExists(String username) {
        Long count = userMapper.countByUsername(username);
        return count != null && count > 0;
    }

    /**
     * 发送邮箱验证码
     * 
     * 生成6位数字验证码，存储到Redis
     * 验证码有效期5分钟
     * 
     * @param request 发送邮箱验证码请求（email）
     * @return "验证码已发送"
     */
    @Override
    public String sendEmailCode(SendEmailCodeRequestDTO request) {
        String email = request.getEmail();
        
        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // 存储验证码到Redis（key添加email:前缀区分）
        redisTemplate.opsForValue().set(
                VERIFY_CODE_KEY + "email:" + email,
                code,
                VERIFY_CODE_TTL,
                TimeUnit.SECONDS
        );
        
        log.info("邮箱验证码已发送: email={}, code={}", email, code);
        
        return "验证码已发送";
    }

    /**
     * 邮箱验证码登录
     * 
     * 处理流程与手机号登录类似
     * 
     * @param request 邮箱登录请求（email, code）
     * @return LoginResponseDTO
     * @throws BusinessException 验证码错误/过期、用户不存在/已被禁用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDTO emailLogin(EmailLoginRequestDTO request) {
        // 1. 校验验证码
        String storedCode = redisTemplate.opsForValue().get(VERIFY_CODE_KEY + "email:" + request.getEmail());
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(request.getCode())) {
            throw new BusinessException("验证码错误");
        }
        
        // 2. 删除验证码
        redisTemplate.delete(VERIFY_CODE_KEY + "email:" + request.getEmail());
        
        // 3. 查询用户
        UserEntity user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }
        
        // 4. 校验用户状态
        if (user.getStatus() == UserEntity.STATUS_DISABLED) {
            throw new BusinessException("用户已被禁用");
        }
        
        // 5. 更新最后登录时间
        userMapper.updateLastLoginTime(user.getId());
        
        // 6. 生成JWT令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // 7. 存储RefreshToken到Redis
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + user.getId(),
                refreshToken,
                jwtUtil.getRefreshTokenValidity(),
                TimeUnit.SECONDS
        );
        
        log.info("邮箱登录成功: userId={}, email={}", user.getId(), request.getEmail());
        
        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenValidity())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
