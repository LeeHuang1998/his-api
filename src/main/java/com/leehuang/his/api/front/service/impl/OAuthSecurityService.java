package com.leehuang.his.api.front.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuthSecurityService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 密码学安全随机数生成器
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成安全的随机字符串
     *      Base64.getUrlEncoder(): 获取 URL 安全的 Base64 编码器
     *      withoutPadding(): 移除 Base64 的填充字符 =
     *      encodeToString(bytes): 将字节数组编码为字符串
     * @param length    字符串长度
     * @return          生成的字符串
     */
    public String generateSecureRandomString(int length) {
        // 创建指定长度的字节数组
        byte[] bytes = new byte[length];
        // 使用 SecureRandom 生成随机字节，用 nextBytes(bytes) 随机字节填充满数组
        SECURE_RANDOM.nextBytes(bytes);
        // 生成在 URL 中传输的安全随机字符串
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 生成 PKCE code_verifier
     * @return  生成一个长度为 32 的安全随机字符串
     */
    public String generateCodeVerifier() {
        return generateSecureRandomString(32);
    }

    /**
     * 生成 PKCE code_challenge (S256 方式)
     * @param codeVerifier   生成的 code_verifier 字符串
     * @return
     */
    public String generateCodeChallenge(String codeVerifier) {
        try {
            // 使用 ASCII 字符集编码，将 codeVerifier 转换为字节数组
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
            // 使用 SHA-256 哈希算法，创建用于计算哈希值的对象（MessageDigest: Java 密码学消息摘要类）
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 将 codeVerifier 的字节数据提供给哈希算法计算，0 表示起始位置，即将传入的整个数据都进行计算
            md.update(bytes, 0, bytes.length);
            // 计算哈希值，返回 32 字节的 SHA-256 哈希值（不可逆，相同的输入总是产生相同的输出，32 字节编码后 43 个字符，PKCE 要求为 43-128 个字符的随机字符串，刚好为最小长度）
            byte[] digest = md.digest();
            // 将哈希结果转换为 URL 安全的 Base64 字符串（43 字符的 codeChallenge）
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 将生成的 code_verifier 保存到 Redis 中，并设置 10 分钟的过期时间
     * @param state              OAuth state 参数，用于 redis 的key
     * @param codeVerifier       PKCE code_verifier
     */
    public void storeOAuthState(String state, String codeVerifier) {
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("codeVerifier", codeVerifier);
        stateData.put("createdAt", System.currentTimeMillis());             // 记录状态创建时间，用于后续的过期验证
        
        String key = "oauth:state:" + state;
        redisTemplate.opsForValue().set(key, stateData, 10, TimeUnit.MINUTES);
    }

    /**
     * 验证并获取 OAuth 状态
     * @param state         要验证的 state 参数
     * @return              Map<String, Object> 状态数据或 null
     */
    public Map<String, Object> validateAndGetState(String state) {
        // 判断 state 是否为空，若为空则直接返回，防止空值攻击
        if (StringUtils.isEmpty(state)) {
            return null;
        }

        // 从 redis 中获取 PKCE 参数数据，key 为 state
        String key = "oauth:state:" + state;
        Map<String, Object> stateData = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        
        // 验证成功后立即删除，防止重放攻击（重放攻击是指攻击者截获合法的数据传输，然后在稍后时间重新发送相同的数据，让系统误以为是新的合法请求。）
        if (stateData != null) {
            redisTemplate.delete(key);
            
            // 验证是否过期（额外保护），如果当前时间减去创建时间大于 10 分钟，返回 null
            Long createdAt = (Long) stateData.get("createdAt");
            if (System.currentTimeMillis() - createdAt > 10 * 60 * 1000) {
                return null;
            }
        }
        // 所有验证都通过后返回有效数据
        return stateData;
    }
}