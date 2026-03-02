package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.front.dto.customer.request.*;
import com.leehuang.his.api.front.dto.customer.request.validator.ThirdPartyBind;
import com.leehuang.his.api.front.dto.customer.request.validator.ThirdPartyRegister;
import com.leehuang.his.api.front.dto.customer.vo.CustomerVO;
import com.leehuang.his.api.front.dto.customer.vo.LoginVO;
import com.leehuang.his.api.front.dto.customer.vo.SmsCodeVO;
import com.leehuang.his.api.front.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/front/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 发送验证码
     * @param request
     * @return
     */
    @PostMapping("/sendSmsCode")
    public R sendSmsCode(@RequestBody @Valid SmsCodeRequest request){
        // 登录验证
        SmsCodeVO smsCodeVO = customerService.sendSmsCode(request);
        return R.OK(smsCodeVO.getMessage()).put("result", smsCodeVO.isResult());
    }

    /**
     * 注册并登录
     * @param request   注册请求
     * @return
     */
    @PostMapping("/register")
    public R register(@RequestBody @Valid RegisterRequest request) {
        LoginVO loginVO = customerService.register(request);
        return R.OK("注册登录成功").put("result", loginVO);
    }

    /**
     * 第三方登录并注册
     * @param request
     * @return
     */
    @PostMapping("/thirdParty/register")
    public R thirdPartyRegister(@RequestBody @Validated(ThirdPartyRegister.class) ThirdPartyRequest request) {
        LoginVO loginVO = customerService.thirdPartyRegister(request);
        return R.OK("注册登录成功").put("result", loginVO);
    }


    /**
     * 登录
     * @param loginRequest
     * @return
     */
    @PostMapping("/login")
    public R login(@RequestBody @Valid CustomerRequest loginRequest) {
        LoginVO loginVO = customerService.login(loginRequest);
        return R.OK("登录成功").put("result", loginVO);
    }

    @PostMapping("/thirdParty/bind")
    public R thirdPartyBind(@RequestBody @Validated(ThirdPartyBind.class) ThirdPartyRequest request) {
        LoginVO loginVO = customerService.thirdPartyBind(request);
        return R.OK("绑定并登录成功").put("result",loginVO);
    }

    /**
     * 退出登录
     * @return
     */
    @GetMapping("/logout")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R logout() {
        int id = StpCustomerUtil.getLoginIdAsInt();
        StpCustomerUtil.logout(id, "PC");
        return R.OK("退出登录成功");
    }

    /**
     * 检查是否登录，token 是否有效
     * @return
     */
    @GetMapping("/checkLogin")
    public R checkLogin() {
        boolean bool = StpCustomerUtil.isLogin();
        return R.OK().put("result", bool);
    }

    /**
     * 获取用户信息
     * @return
     */
    @GetMapping("/getCustomerInfo")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R getCustomerInfo() {
        CustomerVO customerVO = customerService.getCustomerInfo();
        return R.OK().put("customerInfo", customerVO);
    }

    /**
     * 更新个人信息
     * @param request
     * @return
     */
    @PostMapping("/updateCustomerInfo")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R updateCustomerInfo(@RequestBody @Valid CustomerInfoRequest request) {
        customerService.updateCustomerInfo(request);
        return R.OK("更新个人信息成功");
    }

    /**
     * 更新手机号或密码
     * @param request
     * @return
     */
    @PostMapping("/updateTelOrPassword")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R updateTelOrPassword(@RequestBody @Valid CustomerRequest request) {
        customerService.updateTelOrPassword(request);
        return R.OK("修改成功");
    }

    @PostMapping("/updateAvatar")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R updateAvatar(@RequestParam("file") MultipartFile file, @RequestParam("oldPath") String oldPath) {
        customerService.updateAvatar(file, oldPath);
        return R.OK("修改照片成功");
    }


    /**
     * 根据临时 token 获取第三方登录用户信息
     * @param redirectToken
     * @return
     */
    @GetMapping("/getLoginInfoByToken")
    public R getLoginInfoByToken(@RequestParam String redirectToken) {
        // 1. 从 redis 中获取用户信息
        String redisKey = "login:redirect:" + redirectToken;

        Map<String, Object> loginInfo = (Map<String, Object>)redisTemplate.opsForValue().get(redisKey);

        // 2. 删除 redis 中的用户信息
        if (loginInfo != null) {
            // 获取后删除Redis中的数据
            redisTemplate.delete(redisKey);
        }

        // 3. 返回用户信息
        return R.OK().put("loginInfo", loginInfo);
    }
}
