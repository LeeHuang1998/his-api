package com.leehuang.his.api.front.dto.customer.request.validator;

import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.customer.request.CustomerRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class LoginRequestValidator {

    // 手机号正则
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    // 用户名正则（3-12位字母数字下划线）
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,12}$";
    // 密码正则（6-18位）
    private static final String PASSWORD_REGEX = "^.{6,18}$";
    // 验证码正则（6位数字）
    private static final String SMS_CODE_REGEX = "^\\d{6}$";

    /**
     * 短信登录验证
     * @param request
     */
    public void validateSmsLogin(CustomerRequest request) {
        // 验证区号
        if (StringUtils.isBlank(request.getAreaCode())) {
            throw new HisException("短信登录需要区号");
        }

        // 验证手机号格式
        if (!request.getIdentity().matches(PHONE_REGEX)) {
            throw new HisException("手机号格式不正确");
        }

        // 验证验证码格式
        if (!request.getCredential().matches(SMS_CODE_REGEX)) {
            throw new HisException("验证码必须是 6 位数字");
        }

        // 验证区号格式
        if (!request.getAreaCode().startsWith("+")) {
            throw new HisException("区号格式不正确");
        }
    }

    /**
     * 密码登录验证
     * @param request
     */
    public void validatePasswordLogin(CustomerRequest request) {
        // 验证用户名格式
        if (!request.getIdentity().matches(USERNAME_REGEX)) {
            throw new HisException("用户名必须是3-12位字母、数字或下划线");
        }

        // 验证密码格式
        if (!request.getCredential().matches(PASSWORD_REGEX)) {
            throw new HisException("密码长度必须是6-18位");
        }
    }
}
