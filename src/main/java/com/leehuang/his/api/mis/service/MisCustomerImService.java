package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.front.dto.customerIM.vo.CustomerIMAccountVO;
import com.leehuang.his.api.mis.dto.customerIm.vo.MisCustomerInfoVO;

public interface MisCustomerImService {

    // 获取 客服im 账号
    CustomerIMAccountVO searchServiceImAccount();

    // 获取 customerIm 页客户数据
    MisCustomerInfoVO searchCustomerSummary(Integer id);
}
