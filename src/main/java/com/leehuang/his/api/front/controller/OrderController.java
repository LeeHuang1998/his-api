package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.order.request.OutTradeNoRequest;
import com.leehuang.his.api.front.dto.order.request.OrderRequest;
import com.leehuang.his.api.front.dto.order.request.RefundOrderRequest;
import com.leehuang.his.api.front.dto.order.vo.OrderDetailVO;
import com.leehuang.his.api.front.dto.order.vo.OrderListVO;
import com.leehuang.his.api.front.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/front/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    private final RedissonClient redissonClient;

    /**
     * 创建订单
     * @return
     */
    @PostMapping("/createOrder")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R createOrder(@RequestBody OrderRequest request) {
        // 创建订单，返回订单流水号
        String outTradeNo = orderService.createOrder(request);
        return R.OK().put("outTradeNo", outTradeNo);
    }

    /**
     *
     * @param outTradeNo 订单流水号
     * @return  订单详情信息
     */
    @GetMapping("/orderDetails/{outTradeNo}")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R getOrderDetail(@PathVariable String outTradeNo) {
        OrderDetailVO orderDetail = orderService.getOrderDetail(outTradeNo);
        return R.OK().put("orderDetailVO", orderDetail);
    }

    /**
     * 查询订单状态
     * @param outTradeNo
     * @return
     */
    @GetMapping("/checkOrderStatus/{outTradeNo}")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R checkOrderStatus(@PathVariable String outTradeNo) {
        Integer orderStatus = orderService.checkOrderStatus(outTradeNo);
        return R.OK().put("status", orderStatus);
    }

    /**
     * 条件分页查询订单列表
     * @param page           页码
     * @param length         每页数量
     * @param keyword        关键词
     * @param status         订单状态
     * @return
     */
    @GetMapping("/orderList")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R orderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer length,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        PageUtils<OrderListVO> orderData = orderService.searchOrderListByCustomerId(page, length, keyword, status, customerId);
        return R.OK().put("orderData", orderData);
    }

    /**
     * 关闭订单
     * @param request
     * @return
     */
    @PostMapping("/closeOrder")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R closeOrder(@RequestBody OutTradeNoRequest request) {
        String msg = orderService.closeOrder(request);
        return R.OK().put("msg", msg);
    }

    /**
     * 退款请求
     * @param request
     * @return
     */
    @PostMapping("/refund")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R refund(@RequestBody RefundOrderRequest request) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();

        // 分布式锁防重，锁 60 秒
        String lockKey = "refund:lock:" + request.getOutTradeNo();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，0秒等待，60秒自动释放
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return R.error("退款申请正在处理中，请稍后再试");
            }
            // 退款
            orderService.refundOrder(request, customerId);
            return R.OK().put("msg", "退款申请提交成功，预计1-3个工作日到账");

        } catch (HisException e) {
            log.warn("退款业务异常: {}", e.getMessage());
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("退款系统异常", e);
            return R.error("系统繁忙，请稍后重试");
        } finally {
            // 只有当前线程持有锁时才释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询退款结果
     * @param request   订单号
     * @return
     */
    @PostMapping("/checkRefund")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R checkRefund(@RequestBody OutTradeNoRequest request) {
        String refundResult = orderService.checkRefund(request);
        return R.OK().put("refundResult", refundResult);
    }



}
