package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.db.dao.OrderDao;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.order.request.OutTradeNoRequest;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.mis.dto.order.request.SearchOrderByPageRequest;
import com.leehuang.his.api.mis.dto.order.vo.MisOrderPageVO;
import com.leehuang.his.api.mis.service.MisOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/mis/order")
@RequiredArgsConstructor
public class MisOrderController {

    private final MisOrderService misOrderService;
    private final OrderDao orderDao;

    @PostMapping("/searchOrderListByPage")
    @SaCheckPermission(value = {"ROOT", "ORDER:SELECT"}, mode = SaMode.OR)
    public R searchByPage(@RequestBody @Valid SearchOrderByPageRequest pageRequest) {
        
        if ((pageRequest.getStartDate() != null && pageRequest.getEndDate() == null) || (pageRequest.getStartDate() == null && pageRequest.getEndDate() != null)) {
            throw new HisException("startDate 和 endDate 不允许一个为空，另一个不为空");
        } else if (pageRequest.getStartDate() != null && pageRequest.getEndDate() != null) {

            LocalDateTime startDate = LocalDateTime.parse(pageRequest.getStartDate());
            LocalDateTime endDate = LocalDateTime.parse(pageRequest.getEndDate());

            if (endDate.isBefore(startDate)) {
                throw new HisException("endDate 不能早于 startDate");
            }
        }
        PageUtils<MisOrderPageVO> pageData = misOrderService.searchOrderListByPage(pageRequest);
        return R.OK().put("pageData", pageData);

    }

    /**
     * 查询订单支付结果
     * @param request
     * @return
     */
    @PostMapping("/checkPaymentResult")
    @SaCheckLogin
    public R checkPaymentResult(@RequestBody @Valid OutTradeNoRequest request) {
        Integer paymentResult = misOrderService.checkPaymentResult(request.getOutTradeNo());
        return R.OK().put("paymentResult", paymentResult);
    }

    /**
     * 后台删除订单
     * @param form
     * @return
     */
    @PostMapping("/deleteOrderById")
    @SaCheckPermission(value = {"ROOT", "ORDER:DELETE"}, mode = SaMode.OR)
    public R deleteById(@RequestBody @Valid IdRequest form) {
        int rows = misOrderService.deleteOrderById(form.getId());
        return R.OK().put("rows", rows);
    }

    /**
     * 线下退款接口
     * @param request
     * @return
     */
    @PostMapping("/updateRefundOrderById")
    @SaCheckPermission(value = {"ROOT", "ORDER:UPDATE"}, mode = SaMode.OR)
    public R updateRefundStatusById(@RequestBody @Valid IdRequest request) {
        int rows = misOrderService.updateRefundStatusById(request.getId());
        return R.OK().put("rows", rows);
    }
}
