package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.po.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌服务
 * 负责JWT令牌的生成、解析和验证
 *
 * @author charmingdaidai
 */
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    /**
     * 从JWT令牌中提取用户名
     *
     * 实现思路：
     * 1. 使用extractClaim方法提取Subject声明
     * 2. Subject字段存储的是用户名信息
     * 3. 委托给通用的声明提取方法处理
     *
     * @param token JWT令牌字符串
     * @return 令牌中存储的用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从JWT令牌中提取特定声明
     *
     * 实现思路：
     * 1. 首先解析JWT令牌获取所有声明（Claims）
     * 2. 使用提供的claimsResolver函数从Claims中提取特定字段
     * 3. 支持泛型返回，可以提取不同类型的声明值
     * 4. 提供统一的声明提取接口，便于扩展
     *
     * @param <T> 声明值的类型
     * @param token JWT令牌字符串
     * @param claimsResolver 声明解析函数，用于从Claims中提取特定字段
     * @return 提取的声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 为用户生成JWT访问令牌
     *
     * 实现思路：
     * 1. 创建额外声明Map用于存储用户相关信息
     * 2. 检查userDetails是否为User类型实例
     * 3. 如果是User实例，提取并添加额外声明：
     *    - userId：用户唯一标识
     *    - email：用户邮箱地址
     *    - vip：VIP用户标识
     * 4. 委托给generateToken重载方法生成最终令牌
     * 5. 增强令牌信息，便于客户端使用
     *
     * @param userDetails Spring Security用户详情对象
     * @return 包含用户信息的JWT访问令牌
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            extraClaims.put("userId", user.getId());
            extraClaims.put("email", user.getEmail());
            extraClaims.put("vip", user.isVip());
        }
        return generateToken(extraClaims, userDetails);
    }

    /**
     * 生成包含额外声明的JWT令牌
     *
     * 实现思路：
     * 1. 接收自定义的额外声明Map
     * 2. 使用标准的JWT访问令牌过期时间
     * 3. 委托给buildToken方法构建最终令牌
     * 4. 支持在令牌中添加业务相关的自定义字段
     *
     * @param extraClaims 额外的声明信息Map
     * @param userDetails Spring Security用户详情对象
     * @return 包含额外声明的JWT令牌
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * 生成JWT刷新令牌
     *
     * 实现思路：
     * 1. 使用空的额外声明Map，刷新令牌不需要额外信息
     * 2. 使用较长的刷新令牌过期时间（refreshExpiration）
     * 3. 委托给buildToken方法构建令牌
     * 4. 刷新令牌用于获取新的访问令牌，生命周期较长
     *
     * @param userDetails Spring Security用户详情对象
     * @return JWT刷新令牌
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    /**
     * 构建JWT令牌的核心方法
     *
     * 实现思路：
     * 1. 使用JJWT库的Builder模式构建令牌
     * 2. 设置自定义声明（extraClaims）
     * 3. 设置标准声明：
     *    - Subject：用户名作为令牌主题
     *    - IssuedAt：令牌签发时间
     *    - Expiration：令牌过期时间
     * 4. 使用HMAC SHA256算法和密钥进行签名
     * 5. 压缩并返回最终的JWT字符串
     * 6. 确保令牌的安全性和完整性
     *
     * @param extraClaims 额外的声明信息
     * @param userDetails 用户详情对象
     * @param expiration 令牌过期时间（毫秒）
     * @return 构建完成的JWT令牌字符串
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * 验证JWT令牌的有效性
     *
     * 实现思路：
     * 1. 从令牌中提取用户名
     * 2. 检查提取的用户名是否与提供的用户详情匹配
     * 3. 检查令牌是否已过期
     * 4. 只有用户名匹配且令牌未过期才认为有效
     * 5. 提供统一的令牌验证逻辑
     *
     * @param token 要验证的JWT令牌
     * @param userDetails 用户详情对象，用于比较用户名
     * @return true表示令牌有效，false表示无效
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 检查JWT令牌是否已过期
     *
     * 实现思路：
     * 1. 从令牌中提取过期时间
     * 2. 与当前时间进行比较
     * 3. 如果过期时间早于当前时间则认为已过期
     * 4. 返回过期状态的布尔值
     *
     * @param token JWT令牌
     * @return true表示已过期，false表示未过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 从JWT令牌中提取过期时间
     *
     * 实现思路：
     * 1. 使用extractClaim方法提取Expiration声明
     * 2. Claims::getExpiration方法引用获取过期时间
     * 3. 返回Date类型的过期时间对象
     *
     * @param token JWT令牌
     * @return 令牌的过期时间
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从JWT令牌中提取所有声明信息
     *
     * 实现思路：
     * 1. 使用JJWT库的解析器构建器
     * 2. 设置签名验证密钥以确保令牌完整性
     * 3. 解析JWT字符串并验证签名
     * 4. 返回包含所有声明的Claims对象
     * 5. 如果签名验证失败或令牌格式错误会抛出异常
     *
     * @param token JWT令牌字符串
     * @return 包含所有声明的Claims对象
     */
    private Claims extractAllClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(getSignInKey()).build();
        return parser.parseSignedClaims(token).getPayload();
    }

    /**
     * 获取JWT签名密钥
     *
     * 实现思路：
     * 1. 使用Base64解码器解码配置的密钥字符串
     * 2. 将解码后的字节数组转换为HMAC密钥
     * 3. 使用Keys.hmacShaKeyFor确保密钥格式正确
     * 4. 返回用于JWT签名和验证的Key对象
     * 5. 确保密钥的安全性和一致性
     *
     * @return JWT签名和验证使用的密钥对象
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}