package com.leehuang.his.api.front.service;

import com.leehuang.his.api.front.dto.customerIM.vo.CustomerIMAccountVO;

public interface CustomerIMService {

    // 用户创建 im 账户
    CustomerIMAccountVO createCustomerIMAccount(Integer customerId);
}
