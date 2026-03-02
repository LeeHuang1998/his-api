package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.mis.dto.order.request.SearchOrderByPageRequest;
import com.leehuang.his.api.mis.dto.order.vo.MisOrderPageVO;

import javax.validation.Valid;

public interface MisOrderService {
    // mis 端订单管理页面分页数据
    PageUtils<MisOrderPageVO> searchOrderListByPage(@Valid SearchOrderByPageRequest pageRequest);

    // 同步订单支付结果
    Integer checkPaymentResult(String outTradeNo);

    // 后台删除订单
    int deleteOrderById(Integer id);

    // 更新退款状态
    int updateRefundStatusById(Integer id);
}
