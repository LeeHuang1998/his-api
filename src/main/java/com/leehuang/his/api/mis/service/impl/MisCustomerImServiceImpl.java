package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.db.dao.CustomerDao;
import com.leehuang.his.api.db.dao.OrderDao;
import com.leehuang.his.api.db.entity.CustomerEntity;
import com.leehuang.his.api.front.dto.customerIM.vo.CustomerIMAccountVO;
import com.leehuang.his.api.mis.dto.customerIm.dto.OrderStatisticDTO;
import com.leehuang.his.api.mis.dto.customerIm.vo.MisCustomerInfoVO;
import com.leehuang.his.api.mis.service.MisCustomerImService;
import com.tencentyun.TLSSigAPIv2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;

@Service("misCustomerImService")
@RequiredArgsConstructor
public class MisCustomerImServiceImpl implements MisCustomerImService {

    @Value("${tencent.im.sdkAppId}")
    private Long sdkAppId;

    @Value("${tencent.im.secretKey}")
    private String secretKey;

    @Value("${tencent.im.customerServiceId}")
    private String customerServiceId;

    private final OrderDao orderDao;

    private final CustomerDao customerDao;

    private final MinioProperties minioProperties;

    /**
     * 获取 客服im 账号
     * @return
     */
    @Override
    public CustomerIMAccountVO searchServiceImAccount() {

        TLSSigAPIv2 api = new TLSSigAPIv2(sdkAppId, secretKey);

        String userSig = api.genUserSig(customerServiceId, 180 * 86400);

        CustomerIMAccountVO imAccountVO = new CustomerIMAccountVO();
        imAccountVO.setAccount(customerServiceId);
        imAccountVO.setUserSig(userSig);
        imAccountVO.setSdkAppId(sdkAppId);

        return imAccountVO;
    }

    /**
     * 获取 customerIm 页客户数据
     * @param id
     * @return
     */
    @Override
    public MisCustomerInfoVO searchCustomerSummary(Integer id) {
        CustomerEntity entity = customerDao.searchCustomerBaseInfoById(id);
        MisCustomerInfoVO customerVO = new MisCustomerInfoVO();

        BeanUtil.copyProperties(entity, customerVO);
        customerVO.setPhoto(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + customerVO.getPhoto());

        OrderStatisticDTO statisticDTO = orderDao.searchOrderStatistic(id);
        customerVO.setTotalAmount(statisticDTO.getTotalAmount() == null ? BigDecimal.ZERO : statisticDTO.getTotalAmount());
        customerVO.setTotalCount(statisticDTO.getTotalCount());
        customerVO.setNumber(statisticDTO.getNumber() == null ? 0 : statisticDTO.getNumber());

        return customerVO;
    }
}
