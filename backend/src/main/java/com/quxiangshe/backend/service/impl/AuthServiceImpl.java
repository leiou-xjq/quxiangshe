package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.dto.*;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.exception.BusinessException;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IActivityService;
import com.quxiangshe.backend.service.IAuthService;
import com.quxiangshe.backend.service.IEmailService;
import com.quxiangshe.backend.service.IRedisTokenService;
import com.quxiangshe.backend.service.ISearchService;
import com.quxiangshe.backend.service.IUserService;
import com.quxiangshe.backend.util.JwtUtil;
import com.quxiangshe.backend.util.PasswordUtil;
import com.quxiangshe.backend.vo.LoginVO;
import com.quxiangshe.backend.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 *
 * <p>核心职责：
 * <ul>
 *   <li>用户注册（用户名+密码 / 邮箱验证码）</li>
 *   <li>多模式登录（用户名+密码 / 邮箱验证码 / 微信授权）</li>
 *   <li>双令牌机制：accessToken（短期）+ refreshToken（长期存储在 Redis）</li>
 *   <li>退出登录、Token 刷新、密码重置等认证生命周期管理</li>
 * </ul>
 *
 * <p>双令牌机制说明：
 * <ul>
 *   <li>accessToken：默认 15 分钟有效期，存储在前端</li>
 *   <li>refreshToken：默认 7 天有效期，存储在 Redis 中，用于无感知续期</li>
 * </ul>
 *
 * <p>所属业务模块：用户认证管理
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {
    
    private final UserMapper userMapper;
    private final IUserService userService;
    private final JwtUtil jwtUtil;
    private final IRedisTokenService redisTokenService;
    private final ISearchService searchService;
    private final IEmailService emailService;
    private final IActivityService activityService;
    
    @Value("${jwt.access-token-validity:900}")
    private Integer accessTokenValidity;
    
    @Value("${jwt.refresh-token-validity:604800}")
    private Integer refreshTokenValidity;
    
    @Value("${wechat.enabled:false}")
    private boolean wechatEnabled;
    
    @Value("${wechat.app-id:}")
    private String wechatAppId;
    
    @Value("${wechat.app-secret:}")
    private String wechatAppSecret;
    
    /**
     * 用户注册（用户名+密码模式）
     *
     * <p>执行流程：验证两次密码一致性 → 检查用户名/邮箱/手机号唯一性 → 创建用户 → 生成双令牌。
     *
     * @param request   注册请求（含用户名、密码、邮箱、手机号、昵称）
     * @param ipAddress 客户端IP地址
     * @return 登录响应（含 accessToken、refreshToken、用户信息）
     * @throws BusinessException 当用户名校验失败、密码不一致时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request, String ipAddress) {
        // 验证密码和确认密码
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(1001005, "两次密码不一致");
        }

        // 检查用户名是否已存在
        if (userMapper.isUsernameExists(request.getUsername())) {
            throw new BusinessException(1001001, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userMapper.isEmailExists(request.getEmail())) {
            throw new BusinessException(1001003, "邮箱已被注册");
        }

        // 检查手机号是否已存在
        if (userMapper.isPhoneExists(request.getPhone())) {
            throw new BusinessException(1001002, "手机号已被注册");
        }

        // 创建用户 - 同时保存用户名、邮箱、电话
        User user = createUser(
            request.getUsername(),
            request.getPhone(),
            request.getEmail(),
            request.getPassword(),
            request.getNickname()
        );

        log.info("用户注册成功: username={}, email={}, phone={}, ip={}",
            user.getUsername(), user.getEmail(), user.getPhone(), ipAddress);

        // 生成Token并返回
        return generateLoginVO(user, ipAddress);
    }
    
    /**
     * 用户登录（支持密码登录和邮箱验证码登录两种模式）
     *
     * <p>由 loginType 参数区分登录模式：
     * <ul>
     *   <li>"emailCode"：邮箱验证码登录，先校验验证码再查用户</li>
     *   <li>默认：用户名密码登录，先查用户再验密码</li>
     * </ul>
     * 登录成功后会校验用户状态（禁用、待审核）、更新最后登录信息、记录活跃度。
     *
     * @param request   登录请求
     * @param ipAddress 客户端IP地址
     * @return 登录响应（含双令牌）
     * @throws BusinessException 当用户不存在、密码错误、账户被禁用时
     */
    @Override
    public LoginVO login(LoginRequest request, String ipAddress) {
        User user = null;
        String loginType = request.getLoginType();

        if ("emailCode".equals(loginType)) {
            // 邮箱验证码登录模式：先校验验证码，再查询用户
            String email = request.getEmail();
            String emailCode = request.getEmailCode();

            if (email == null || email.isEmpty()) {
                throw new BusinessException(1002005, "邮箱不能为空");
            }
            if (emailCode == null || emailCode.isEmpty()) {
                throw new BusinessException(1004001, "验证码不能为空");
            }

            // 校验验证码
            if (!emailService.verifyCode(email, emailCode)) {
                throw new BusinessException(1004002, "验证码错误或已过期");
            }

            // 查询用户
            user = userMapper.selectByEmail(email);
            if (user == null) {
                throw new BusinessException(1002001, "用户不存在");
            }

        } else {
            // 用户名密码登录模式（默认）：先查用户再验密码
            String username = request.getUsername();
            String password = request.getPassword();

            if (username == null || username.isEmpty()) {
                throw new BusinessException(1002005, "用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                throw new BusinessException(1002004, "密码不能为空");
            }

            // 查询用户
            user = userMapper.selectByUsername(username);
            if (user == null) {
                throw new BusinessException(1002001, "用户不存在");
            }

            // 验证密码
            if (!PasswordUtil.verify(password, user.getPassword())) {
                throw new BusinessException(1002002, "密码错误");
            }
        }

        // 验证用户状态：禁用(0) 或 待审核(2) 禁止登录
        if (user.getStatus() == 0) {
            throw new BusinessException(1002003, "账户已被禁用");
        }
        if (user.getStatus() == 2) {
            throw new BusinessException(1002006, "账户待审核");
        }

        // 更新最后登录信息
        user.setLastLoginIp(ipAddress);
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        // 记录用户登录活跃度
        activityService.recordLogin(user.getId());

        log.info("用户登录成功: username={}, ip={}", user.getUsername(), ipAddress);

        // 生成Token并返回
        return generateLoginVO(user, ipAddress);
    }
    
    /**
     * 用户退出登录
     *
     * <p>删除 Redis 中存储的 refreshToken，使该 token 失效。
     * accessToken 无法主动失效（无状态 JWT），需依赖其短期过期自然失效。
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId) {
        // 删除Redis中的refreshToken
        redisTokenService.removeRefreshToken(userId);
        log.info("用户退出登录: userId={}", userId);
    }
    
    /**
     * 刷新 accessToken
     *
     * <p>使用 refreshToken 换取新的双令牌。验证步骤：
     * <ol>
     *   <li>验证 refreshToken 的 JWT 签名和过期时间</li>
     *   <li>检查 token 类型是否为 "refresh"</li>
     *   <li>验证 Redis 中存储的 refreshToken 与此 token 是否一致</li>
     *   <li>检查用户是否存在且状态正常</li>
     * </ol>
     *
     * @param refreshToken 刷新型 Token
     * @param ipAddress    客户端IP地址
     * @return 新的登录响应（含新的双令牌）
     * @throws BusinessException 当 refreshToken 无效、已失效或用户状态异常时
     */
    @Override
    public LoginVO refreshToken(String refreshToken, String ipAddress) {
        // 验证refreshToken
        Map<String, Object> claims = jwtUtil.validateAndGetClaimsFromToken(refreshToken);
        if (claims == null) {
            throw new BusinessException(1003001, "refreshToken无效");
        }
        
        // 验证 refreshToken 类型必须为 "refresh"，防止 accessToken 被误用为 refreshToken
        String tokenType = (String) claims.get("type");
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(1003001, "refreshToken无效");
        }
        
        // 获取用户ID
        Long userId = Long.valueOf((String) claims.get("sub"));
        
        // 验证Redis中的refreshToken是否匹配
        if (!redisTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new BusinessException(1003001, "refreshToken已失效，请重新登录");
        }
        
        // 查询用户
        User user = userService.getById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(1003001, "refreshToken无效");
        }
        
        log.info("刷新Token成功: userId={}", userId);
        
        // 生成新Token（会同时生成新的refreshToken存储到Redis）
        return generateLoginVO(user, ipAddress);
    }
    
    /**
     * 创建用户实体并持久化
     *
     * <p>默认设置头像为系统默认值、状态为已启用、性别为保密。
     * 创建成功后同步用户数据到搜索引擎。
     *
     * @param username 用户名
     * @param phone    手机号（可选）
     * @param email    邮箱
     * @param password 明文密码（方法内会加密）
     * @param nickname 昵称
     * @return 新创建的用户实体
     */
    private User createUser(String username, String phone, String email, String password, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(PasswordUtil.encode(password));
        user.setNickname(nickname);
        user.setAvatar("/avatar/default.png");
        user.setStatus(1);
        user.setGender(0);
        
        userMapper.insert(user);
        
        // 同步到ES
        searchService.syncUser(user.getId());
        
        return user;
    }
    
    /**
     * 生成登录响应 VO（含双令牌）
     *
     * <p>生成 accessToken（短期）和 refreshToken（长期），
     * 将 refreshToken 存入 Redis 并由 Redis 管理其过期时间。
     *
     * @param user      用户实体
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     */
    private LoginVO generateLoginVO(User user, String ipAddress) {
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("sub", user.getId().toString());
        accessClaims.put("username", user.getUsername());
        accessClaims.put("type", "access");
        accessClaims.put("role", user.getRole() != null ? user.getRole() : User.ROLE_USER);
        String accessToken = jwtUtil.createToken(accessClaims, accessTokenValidity * 1000L);
        
        // 生成refreshToken（7天 = 604800秒）
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("sub", user.getId().toString());
        refreshClaims.put("type", "refresh");
        String refreshToken = jwtUtil.createToken(refreshClaims, refreshTokenValidity * 1000L);
        
        // 存储refreshToken到Redis（7天过期）
        redisTokenService.storeRefreshToken(user.getId(), refreshToken, refreshTokenValidity);
        
        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(accessTokenValidity);
        vo.setUser(userService.convertToVO(user));
        
        return vo;
    }
    
    /**
     * 发送邮箱验证码
     *
     * @param email 目标邮箱地址
     * @throws BusinessException 当验证码发送失败时
     */
    @Override
    public void sendVerifyCode(String email) {
        boolean success = emailService.sendVerifyCode(email);
        if (!success) {
            throw new BusinessException(1004001, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 邮箱验证码登录
     *
     * <p>先校验验证码有效性，再查询邮箱对应的用户，校验用户状态后生成令牌。
     *
     * @param email     邮箱地址
     * @param code      验证码
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     * @throws BusinessException 当验证码错误、用户不存在、账户被禁用时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO emailLogin(String email, String code, String ipAddress) {
        if (!emailService.verifyCode(email, code)) {
            throw new BusinessException(1004002, "验证码错误或已过期");
        }

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(1002001, "用户不存在，请先注册");
        }
        
        if (user.getStatus() == 0) {
            throw new BusinessException(1002003, "账户已被禁用");
        }
        if (user.getStatus() == 2) {
            throw new BusinessException(1002006, "账户待审核");
        }
        
        user.setLastLoginIp(ipAddress);
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);
        
        log.info("邮箱验证码登录成功: email={}, ip={}", email, ipAddress);
        
        return generateLoginVO(user, ipAddress);
    }
    
    /**
     * 邮箱验证码注册
     *
     * <p>先校验验证码，再检查邮箱唯一性，自动生成唯一用户名，创建用户后返回登录令牌。
     *
     * @param request   注册请求（含邮箱、密码、验证码、昵称）
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     * @throws BusinessException 当验证码错误或邮箱已注册时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO emailRegister(EmailRegisterRequest request, String ipAddress) {
        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            throw new BusinessException(1004002, "验证码错误或已过期");
        }

        User existUser = userMapper.selectByEmail(request.getEmail());
        if (existUser != null) {
            throw new BusinessException(1001003, "邮箱已被注册");
        }

        String username = "user" + System.currentTimeMillis();
        while (userMapper.isUsernameExists(username)) {
            username = "user" + System.currentTimeMillis();
        }

        User user = createUser(
            username,
            null,
            request.getEmail(),
            request.getPassword(),
            request.getNickname()
        );

        log.info("邮箱验证码注册成功: email={}, userId={}", request.getEmail(), user.getId());

        return generateLoginVO(user, ipAddress);
    }
    
    /**
     * 发送重置密码的验证码
     *
     * <p>先校验邮箱是否已注册，防止向未注册邮箱发送验证码。
     *
     * @param email 已注册的邮箱地址
     * @throws BusinessException 当邮箱未注册或发送失败时
     */
    @Override
    public void sendResetCode(String email) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(1002001, "该邮箱未注册");
        }
        boolean success = emailService.sendVerifyCode(email);
        if (!success) {
            throw new BusinessException(1004001, "验证码发送失败，请稍后重试");
        }
    }
    
    /**
     * 重置密码
     *
     * <p>校验验证码后更新用户密码，同时清除该用户的所有 refreshToken，
     * 强制所有已登录设备重新登录。
     *
     * @param request 重置密码请求（含邮箱、验证码、新密码）
     * @throws BusinessException 当验证码错误或邮箱未注册时
     */
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            throw new BusinessException(1004002, "验证码错误或已过期");
        }

        User user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException(1002001, "该邮箱未注册");
        }

        user.setPassword(PasswordUtil.encode(request.getPassword()));
        userMapper.updateById(user);

        redisTokenService.removeRefreshToken(user.getId());

        log.info("密码重置成功: email={}", request.getEmail());
    }
    
    /**
     * 微信授权登录
     *
     * <p>通过微信授权码换取 openId，若首次登录则自动注册新用户。
     *
     * @param request   微信登录请求（含授权码 code）
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     * @throws BusinessException 当微信登录未启用或授权失败时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO wechatLogin(WechatLoginRequest request, String ipAddress) {
        log.info("微信登录: code={}", request.getCode());
        
        if (!wechatEnabled) {
            throw new BusinessException(500, "微信登录未启用");
        }
        
        String openId = getWechatOpenId(request.getCode());
        if (openId == null) {
            throw new BusinessException(500, "微信授权失败");
        }
        
        User user = userMapper.selectByWechatOpenId(openId);
        
        if (user == null) {
            user = createWechatUser(openId, request.getNickname(), request.getAvatar());
            log.info("微信新用户注册: openId={}, userId={}", openId, user.getId());
        }
        
        if (user.getStatus() == 0) {
            throw new BusinessException(1002003, "账户已被禁用");
        }
        
        user.setLastLoginIp(ipAddress);
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);
        
        log.info("微信登录成功: userId={}, ip={}", user.getId(), ipAddress);
        
        return generateLoginVO(user, ipAddress);
    }
    
    private String getWechatOpenId(String code) {
        return "mock_openid_" + code.substring(0, 8);
    }
    
    private User createWechatUser(String openId, String nickname, String avatar) {
        String username = "wechat_" + System.currentTimeMillis();
        
        User user = new User();
        user.setUsername(username);
        user.setWechatOpenId(openId);
        user.setNickname(nickname != null ? nickname : "微信用户");
        user.setAvatar(avatar != null ? avatar : "/avatar/default.png");
        user.setPassword(PasswordUtil.encode("wechat_" + System.currentTimeMillis()));
        user.setStatus(1);
        user.setGender(0);
        
        userMapper.insert(user);
        
        return user;
    }
}
