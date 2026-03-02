package com.leehuang.his.api.front.service;

import com.leehuang.his.api.db.entity.CustomerThirdPartyEntity;
import com.leehuang.his.api.front.dto.customer.CustomerBindDTO;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public interface CustomerThirdPartyService {

    // 获取第三方登录用户在本系统中是否有数据
    CustomerThirdPartyEntity findByOpenIdAndPlatform(String openId, String gitee);

    // 插入第三方登录用户信息
    void insertThirdPartyCustomer(CustomerThirdPartyEntity thirdPartyEntity);

    // 根据手机号和平台查询用户是否已绑定
    CustomerBindDTO searchByTelAndPlatform(String phoneNum, String platform);
}
