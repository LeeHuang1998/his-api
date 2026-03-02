package com.leehuang.his.api.mis.service.impl;

import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.enums.OrderStatusEnum;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.db.dao.OrderDao;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.service.AlipayService;
import com.leehuang.his.api.mis.dto.order.request.SearchOrderByPageRequest;
import com.leehuang.his.api.mis.dto.order.vo.MisOrderPageVO;
import com.leehuang.his.api.mis.service.MisOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("MisOrderService")
@RequiredArgsConstructor
@Slf4j
public class MisOrderServiceImpl implements MisOrderService {

    private final OrderDao orderDao;

    private final MinioProperties minioProperties;

    private final AlipayService alipayService;

    /**
     * mis 端订单管理分页数据
     * @param pageRequest
     * @return
     */
    @Override
    public PageUtils<MisOrderPageVO> searchOrderListByPage(SearchOrderByPageRequest pageRequest) {

        int page = pageRequest.getPage();
        int length = pageRequest.getLength();
        int start = (page - 1) * length;

        List<MisOrderPageVO> misOrderPageVOList = orderDao.searchMisOrderByPage(start, pageRequest);
        long totalCount = orderDao.searchMisOrderCountByPage(pageRequest);

        misOrderPageVOList = misOrderPageVOList.stream().peek(misOrderPageVO -> misOrderPageVO
                .setPhoto(
                        minioProperties.getEndpoint() + "/"
                        + minioProperties.getBucket() + "/"
                        + misOrderPageVO.getPhoto()))
        .collect(Collectors.toList());

        return new PageUtils<>(totalCount, length, page, misOrderPageVOList);
    }

    /**
     * 同步订单支付结果
     * @param outTradeNo
     * @return
     */
    @Override
    @Transactional
    public Integer checkPaymentResult(String outTradeNo) {
        // 1. 查询本地订单（要加锁防止并发）
        OrderEntity order = orderDao
                .selectOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOutTradeNo, outTradeNo));

        if (order == null) {
            log.warn("订单不存在或状态无需同步: {}", outTradeNo);
            return 0;
        }

        Integer orderStatus = order.getStatus();

        // 只处理需要同步的状态：1, 3, 7
        if (!Arrays.asList(OrderStatusEnum.UNPAID.getCode(), OrderStatusEnum.PAID.getCode(), OrderStatusEnum.REFUNDING.getCode()).contains(orderStatus)) {
            log.debug("订单状态无需同步: outTradeNo={}, status={}", outTradeNo, orderStatus);
            return orderStatus;
        }

        // 2. 查询支付宝订单
        AlipayTradeQueryResponse response = alipayService.queryOrder(outTradeNo);

        OrderEntity updateOrder = new OrderEntity();
        updateOrder.setId(order.getId());

        // 3. 支付宝接口查询错误
        if (!response.isSuccess()) {
            // 支付宝订单不存在 或 已关闭
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode()) ||
                    "ACQ.TRADE_HAS_CLOSE".equals(response.getSubCode())) {

                return orderStatus;
            }

            throw new HisException("支付宝订单查询失败："
                    + response.getCode() + " - " + response.getMsg()
                    + " / " + response.getSubCode() + " - " + response.getSubMsg());
        }

        // 4. 支付宝查询成功，解析 tradeStatus
        String tradeStatus = response.getTradeStatus();

        // 4.1 未付款
        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            if (!Objects.equals(orderStatus, OrderStatusEnum.UNPAID.getCode())) {
                log.warn("订单状态倒退，本地状态={}，支付宝返回未付款 outTradeNo={}", orderStatus, outTradeNo);
            }
            updateOrder.setStatus(OrderStatusEnum.UNPAID.getCode()); // 未付款
            orderDao.updateById(updateOrder);
            return OrderStatusEnum.UNPAID.getCode();
        }

        // 4.2 支付成功
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            if (!Objects.equals(orderStatus, OrderStatusEnum.PAID.getCode())) {
                updateOrder.setStatus(OrderStatusEnum.PAID.getCode()); // 已付款
                updateOrder.setTransactionId(response.getTradeNo()); // 支付宝交易号
                orderDao.updateById(updateOrder);
            }
            return OrderStatusEnum.PAID.getCode();
        }

        // 4.3 订单关闭 —— 可能是退款成功或未付款关单
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            // 若订单为已退款，则直接返回
            if (orderStatus.equals(OrderStatusEnum.REFUNDED.getCode())) {
                return orderStatus;
            } else if (OrderStatusEnum.REFUNDING.getCode().equals(orderStatus)) {
                // 若订单为退款中，查询退款状态（你业务需要同步退款中/已退款）
                AlipayTradeFastpayRefundQueryResponse refund = alipayService.queryRefund(outTradeNo, order.getOutRefundNo());

                if (!refund.isSuccess() &&
                        !"ACQ.TRADE_NOT_EXIST".equals(refund.getSubCode())) {
                    // 除了订单不存在，其他退款查询失败都抛异常
                    throw new HisException("支付宝退款查询失败：" + refund.getCode()
                            + " / " + refund.getSubCode());
                }

                // 退款成功
                if ("REFUND_SUCCESS".equals(refund.getRefundStatus())) {
                    updateOrder.setStatus(OrderStatusEnum.REFUNDED.getCode()); // 已退款
                    updateOrder.setRefundAmount(new BigDecimal(refund.getRefundAmount()));
                    orderDao.updateById(updateOrder);
                    return OrderStatusEnum.REFUNDED.getCode();
                }

                // 退款中
                if ("REFUND_PROCESSING".equals(refund.getRefundStatus())) {
                    return OrderStatusEnum.REFUNDING.getCode();
                }
            }

            // 若订单为未退款 —— 当作关闭
            updateOrder.setStatus(OrderStatusEnum.CLOSED.getCode()); // 已关闭
            orderDao.updateById(updateOrder);
            return OrderStatusEnum.CLOSED.getCode();
        }

        // 不存在的状态
        throw new HisException("未知的支付宝 trade_status=" + tradeStatus);
    }

    /**
     * 后台删除订单
     * @param id    订单id
     * @return
     */
    @Override
    @Transactional
    public int deleteOrderById(Integer id) {
        return orderDao.delete(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getId, id)
                .eq(OrderEntity::getStatus, OrderStatusEnum.CLOSED.getCode())
        );
    }

    /**
     * mis 端更新退款状态
     * @param id
     * @return
     */
    @Override
    public int updateRefundStatusById(Integer id) {
        OrderEntity update = new OrderEntity();
        update.setStatus(OrderStatusEnum.REFUNDED.getCode());

        return orderDao.update(update,
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getId, id)
                        .eq(OrderEntity::getStatus, OrderStatusEnum.PAID.getCode())
        );
    }
}
