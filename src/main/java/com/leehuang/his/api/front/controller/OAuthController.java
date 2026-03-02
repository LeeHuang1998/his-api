package com.leehuang.his.api.front.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.properties.GiteeProperties;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.db.dao.CustomerDao;
import com.leehuang.his.api.db.entity.CustomerEntity;
import com.leehuang.his.api.db.entity.CustomerThirdPartyEntity;
import com.leehuang.his.api.front.service.CustomerThirdPartyService;
import com.leehuang.his.api.front.service.impl.OAuthSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/OAuth2.0")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final CustomerDao customerDao;

    private final CustomerThirdPartyService thirdPartyService;

    private final GiteeProperties giteeProperties;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OAuthSecurityService oauthSecurityService;

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    /*
     * 传统 OAuth 2.0 的安全漏洞：
     *      授权码拦截攻击：攻击者截获授权码，在自己的客户端中使用该授权码获取访问令牌
     *      授权码注入攻击：攻击者将窃取的授权码注入到合法客户端的令牌请求中
     * 而 PKCE 通过密码学证明确保，请求授权码的客户端与兑换授权码的客户端是同一个，即使授权码被拦截，攻击者也无法使用它
     * 例如：攻击者截获授权码：code=xyz123，当攻击者尝试兑换令牌时，由于没有获取到没有 code_verifier，第三方平台验证失败，拒绝颁发令牌
     *
     * PKCE 参数：
     *      Code_Verifier：高熵加密随机字符串，客户端生成的秘密值（保密，只有客户端知道）
     *      Code_Challenge：Code Verifier 的变换值，在授权请求中发送的公开值
     *      Code_Challenge_Method：变换方法（plain / S256）,指定如何生成 Challenge
     * 流程：
     *      1. 前端发起初始化请求到接口，接口中生成并存储 PKCE 参数，将包含构建授权 URL 的参数和 Code_Challenge 返回到前端（或直接在后端构建好授权 URL 返回到前端）
     *      2. 前端根据返回的参数构建第三方平台授权 URL，并跳转到该 URL，用户登录第三方平台并授权，第三方平台会存储 code_challenge 和授权码的关联关系
     *      3. 授权成功后，第三方平台会重定向到指定的回调 URL，并携带授权码和 state 参数
     *      4. 在回调地址的后端接口中验证 state 参数，验证成功后，从存储中获取对应的 code_verifier，将构建令牌请求参数、授权码和 code_verifier 一起发送请求到第三方平台获取访问令牌
     *      5. 第三方平台根据授权码查找对应的 code_challenge，对请求中的 code_verifier 进行相同的哈希计算得到 computed_challenge，
     *         比较授权时存储的 code_challenge 和 computed_challenge 是否相同，相同则验证通过，颁发令牌
     */

    /**
     * Gitee OAuth 初始化接口，生成 state 和 PKCE 参数
     * @return      PKCE 参数和第三方平台授权回调参数
     */
    @GetMapping("/gitee/state")
    public R giteeOAuthInit() {
        // 生成安全的 state
        String state = oauthSecurityService.generateSecureRandomString(32);

        // 生成 PKCE 参数
        String codeVerifier = oauthSecurityService.generateCodeVerifier();
        String codeChallenge = oauthSecurityService.generateCodeChallenge(codeVerifier);

        // 存储 state 和 code_verifier 的关联关系
        oauthSecurityService.storeOAuthState(state, codeVerifier);

        // 返回给前端的参数
        Map<String, String> result = new HashMap<>();
        result.put("state", state);
        result.put("code_challenge", codeChallenge);
        result.put("code_challenge_method", "S256");
        result.put("client_id", giteeProperties.getClientId());
        result.put("redirect_uri", giteeProperties.getRedirectUri());

        return R.OK().put("result", result);
    }

    /**
     * gitee 第三方登录，从 redis 中取出 PKCE 参数，验证通过后，再进行授权登录
     * @param code                  第三方平台授权码
     * @param state                 PKCE 参数在 redis 中的 key
     * @param response              response 用于重定向
     * @throws IOException
     */
    @GetMapping("/giteeLogin/success")
    public void giteeLogin(String code, String state, HttpServletResponse response) throws IOException {
        // 1. PKCE state 验证
        Map<String, Object> stateData = oauthSecurityService.validateAndGetState(state);
        if (stateData == null) {
            // state 验证失败，重定向到错误页面
            response.sendRedirect("http://localhost:7600/front/index?msg=state验证失败");
            return;
        }

        // 2. 获取 PKCE code_verifier
        String codeVerifier = (String) stateData.get("codeVerifier");

        // 3. 用 code 获取 access_token
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", giteeProperties.getClientId());
        params.put("client_secret", giteeProperties.getClientSecret());
        params.put("redirect_uri", giteeProperties.getRedirectUri());
        params.put("code_verifier", codeVerifier);

        // 4. 发送请求到第三方平台获取授权登录
        String tokenResponse = HttpUtil.post(giteeProperties.getAccessTokenUrl(), params);
        JSONObject tokenJson = JSONUtil.parseObj(tokenResponse);
        String accessToken = tokenJson.getStr("access_token");

        if (accessToken == null) {
            // 授权码为空，获取失败信息
            String errorMsg = tokenJson.getStr("error_description", "token获取失败");
            logger.error("Gitee token获取失败: {}", tokenResponse);
            // 获取失败 → 将错误信息解码后一起重定向到前端首页
            response.sendRedirect("http://localhost:7600/front/index?msg=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
            return;
        }

        // 5. 成功授权后，获取用户信息
        String userInfoResponse = HttpUtil.get(giteeProperties.getUserInfoUrl() + "?access_token=" + accessToken);
        JSONObject userJson = JSONUtil.parseObj(userInfoResponse);

        // 用户信息获取失败处理
        if (userJson.containsKey("message")) {
            logger.error("Gitee 用户信息获取失败: {}", userInfoResponse);
            response.sendRedirect("http://localhost:7600/front/index?msg=用户信息获取失败");
            return;
        }

        // 获取的用户信息
        String openId = userJson.getStr("id");
        String nickname = userJson.getStr("name");
        String avatar = userJson.getStr("avatar_url");
        String email = userJson.getStr("email");

        // 6. 查询是否在本平台已有绑定
        CustomerThirdPartyEntity thirdParty = thirdPartyService.findByOpenIdAndPlatform(openId, "gitee");

        // 6.1 生成临时 token，用于登录后从 redis 中获取用户信息
        String redirectToken = UUID.randomUUID().toString().replace("-", "");

        if (thirdParty != null) {
            // 6.2 已绑定 → 登录成功 → 生成 token → 将用户信息保存到 redis 中
            CustomerEntity entity = customerDao.searchCustomerById(thirdParty.getCustomerId());
            StpCustomerUtil.login(thirdParty.getCustomerId(), "PC");
            String token = StpCustomerUtil.getTokenValue();

            // 6.3 将用户信息保存到 redis 中，用户信息有效期为 5 分钟
            Map<String, Object> loginInfo = new HashMap<>();
            loginInfo.put("isNew", false);
            loginInfo.put("token", token);
            loginInfo.put("username", entity.getUsername());

            redisTemplate.opsForValue().set("login:redirect:" + redirectToken, loginInfo, 5, TimeUnit.MINUTES);
        } else {

            // 6.4 新用户 → 将第三方平台信息保存到 redis 中，当注册成功时，一起保存到数据库中
            Map<String, Object> newUserInfo = new HashMap<>();
            newUserInfo.put("isNew", true);
            newUserInfo.put("platform", "gitee");
            newUserInfo.put("openId", openId);
            newUserInfo.put("nickname", nickname);
            newUserInfo.put("avatar", avatar);
            newUserInfo.put("email", email);

            redisTemplate.opsForValue().set("login:redirect:" + redirectToken, newUserInfo, 30, TimeUnit.MINUTES);
        }

        // 7. 重定向到前端登录页
        String redirectUrl = String.format(
                "http://localhost:7600/front?redirectToken=%s",
                URLEncoder.encode(redirectToken, StandardCharsets.UTF_8)
        );
        response.sendRedirect(redirectUrl);
    }
}
