package com.leehuang.his.api.front.service;

import com.leehuang.his.api.front.dto.customer.request.*;
import com.leehuang.his.api.front.dto.customer.vo.CustomerVO;
import com.leehuang.his.api.front.dto.customer.vo.LoginVO;
import com.leehuang.his.api.front.dto.customer.vo.SmsCodeVO;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerService {

    // 发送短信请求
    SmsCodeVO sendSmsCode(SmsCodeRequest request);

    // 登录请求
    LoginVO login(CustomerRequest loginRequest);

    // 注册请求
    LoginVO register(RegisterRequest request);

    // 获取用户信息
    CustomerVO getCustomerInfo();

    // 更新用户信息
    void updateCustomerInfo(CustomerInfoRequest request);

    // 更新手机号或密码
    void updateTelOrPassword(CustomerRequest request);

    // 第三方登录并注册
    LoginVO thirdPartyRegister(ThirdPartyRequest request);

    // 第三方绑定平台账号
    LoginVO thirdPartyBind(ThirdPartyRequest request);

    // 修改个人信息的照片
    void updateAvatar(MultipartFile file, String userId);
}
