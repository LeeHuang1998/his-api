package com.leehuang.his.api.front.service.impl;

import com.alipay.api.internal.util.AlipaySignature;
import com.leehuang.his.api.common.enums.OrderStatusEnum;
import com.leehuang.his.api.config.AlipayConfig;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.pay.request.PayRequest;
import com.leehuang.his.api.front.service.AlipayService;
import com.leehuang.his.api.front.service.OrderService;
import com.leehuang.his.api.front.service.PaymentNotificationService;
import com.leehuang.his.api.front.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service("paymentService")
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final StringRedisTemplate redisTemplate;

    private final AlipayConfig alipayConfig;

    private final OrderService orderService;

    private final RedissonClient redissonClient;

    private final AlipayService alipayService;

    private final PaymentNotificationService paymentNotificationService;

    /**
     * 生成支付二维码
     * @param payRequest   支付请求，包括支付宝所需的：
     *        outTradeNo: 订单流水号   totalAmount: 订单总金额   subject: 订单描述
     *        paymentType: 支付方式 (支付宝、微信等，以后扩展)
     * @return
     */
    @Override
    public String createQRCode(PayRequest payRequest) {
        String outTradeNo = payRequest.getOutTradeNo();
        String totalAmount = payRequest.getTotalAmount();
        String subject = payRequest.getSubject();

        String paymentType = payRequest.getPaymentType();
        Integer addressId = payRequest.getAddressId();

        // 1. 获取订单支付前需要校验的信息
        OrderEntity entity = orderService.getOrderPayInfo(outTradeNo);
        if (entity == null) {
            throw new HisException("订单不存在");
        }
        // 2. 校验订单支付数据
        validateOrderPayInfo(entity, payRequest);
        // 校验通过后，支付方式和地址更新到数据库中
        orderService.updatePaymentType(outTradeNo, addressId, paymentType);

        // redis 中的 key
        // TODO 其他支付方式
        String redisKey = String.format("alipay:qrcode:%s", outTradeNo);

        // 3. 从 redis 中获取 qrcode 的 String，若存在则直接返回，若不存在则发送请求到支付宝中获取，并存储到 redis 中
        // 是否为刷新操作，若非刷新操作（如点击提交订单时，第一次生成二维码，再点击时返回第一次生成的二维码），则看看 redis 中是否有二维码缓存数据
        boolean isRefresh = payRequest.getIsRefresh() != null && payRequest.getIsRefresh();
        if (!isRefresh) {
            String cacheQrCode = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotBlank(cacheQrCode)) {
                return cacheQrCode;
            }
        }
        log.info("生成支付二维码中，订单号: {}, 金额: {}, 商品描述: {}", outTradeNo, totalAmount, subject);
        return getRedisLockAndQrCode(redisKey, entity, isRefresh);
    }

    /**
     * 校验订单支付前的信息
     * @param orderEntity       数据库中的订单数据
     * @param payRequest        发送的支付请求
     */
    private void validateOrderPayInfo(OrderEntity orderEntity, PayRequest payRequest) {
        // 校验订单是否属于当前用户
        if (orderEntity.getCustomerId() != StpCustomerUtil.getLoginIdAsInt()){
            throw new HisException("订单不属于当前用户，无权限操作");
        }
        // 校验订单商品名称
        if (!StringUtils.equals(orderEntity.getGoodsTitle(), payRequest.getSubject())) {
            throw new HisException("订单商品名称不匹配");
        }
        // 校验订单状态是否为待支付
        if (!Objects.equals(orderEntity.getStatus(), OrderStatusEnum.UNPAID.getCode())) {
            throw new HisException("订单状态错误，无法支付");
        }
        // 校验订单金额是否一致
        BigDecimal payableAmount = orderEntity.getPayableAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal requestAmount = new BigDecimal(payRequest.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
        if (payableAmount.compareTo(requestAmount) != 0) {
            log.error("支付金额不匹配，订单金额：{}，请求金额：{}", payableAmount, requestAmount);
            throw new HisException("支付金额不匹配");
        }
        // 支付方式校验
        if (!"alipay".equals(payRequest.getPaymentType())) {
            throw new HisException("不支持的支付方式");
        }
    }

    /**
     * 获取分布式锁
     * @param redisKey
     * @return
     */
    private String getRedisLockAndQrCode(String redisKey, OrderEntity entity, Boolean isRefresh) {
        String lockKey = redisKey + ":lock";
        // 获取 Redisson 分布式锁对象
        RLock lock = redissonClient.getLock(lockKey);

        // 重试次数
        int retry = 5;
        // tryLock 等待获取锁的最长时间（秒）
        long waitTimeSeconds = 5;
        // 锁的自动释放时间（秒）
        long leaseTimeSeconds = 30;

        while (retry-- > 0) {
            try {
                // 尝试获取锁，最多等待 waitTimeSeconds 秒，持有锁后 leaseTimeSeconds 秒自动释放
                boolean locked = lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);
                if (locked) {
                    try {
                        // 双重检查：若不是刷新操作，且 redis 缓存中了数据（其他线程可能已经生成并缓存了 qrcode），则直接返回
                        // 若是刷新操作，即 isRefresh 为 true，则不管 redis 中是否有数据，都重新生成 qrcode
                        String cacheQrCode = redisTemplate.opsForValue().get(redisKey);
                        if (StringUtils.isNotBlank(cacheQrCode) && !isRefresh) {
                            return cacheQrCode;
                        }

                        // TODO 其他支付方式
                        // 调用支付宝接口获取二维码
                        String qrCode = alipayService.alipayQrcode(entity.getOutTradeNo(),
                                entity.getPayableAmount().toString(),
                                entity.getGoodsTitle());

                        // 将二维码存储到 redis 中，过期时间 110 分钟
                        redisTemplate.opsForValue().set(redisKey, qrCode, 110, TimeUnit.MINUTES);

                        return qrCode;
                    } finally {
                        // 仅当当前线程持有锁时才解锁（防止误解锁），所以使用 try-finally 释放锁
                        if (lock.isHeldByCurrentThread()) {
                            try {
                                lock.unlock();
                            } catch (Exception e) {
                                log.warn("释放二维码锁失败: {}", lockKey, e);
                            }
                        }
                    }
                } else {
                    // 未获取到锁，休眠短暂时间再重试
                    Thread.sleep(100);
                    log.info("未获取到锁，重试第 {} 次，等待获取锁", 5 - retry);
                }
            } catch (InterruptedException ie) {
                // tryLock 在等待锁时可能被中断，若线程在等待期间被中断，就会抛出 InterruptedException
                // JVM 在抛出 InterruptedException 时会清除线程的中断标记，即 Thread.currentThread().isInterrupted() 会被置为 false
                // 上层调用者或线程池 无法知道 这个线程曾经被中断过，业务逻辑可能错误地认为线程正常执行，继续做一些错误的执行
                // 但上层线程或线程池仍然可以通过 Thread.currentThread().isInterrupted() 得到线程被中断的状态
                // 恢复中断状态的目的主要是让上层调用者能正确响应线程被中断的情况，比如终止任务、取消定时任务等。
                Thread.currentThread().interrupt();
                throw new HisException("获取二维码锁被中断", ie);
            } catch (Exception ex) {
                // 记录异常并包装抛出，避免掩盖问题
                log.error("生成二维码过程中发生异常，redisKey={}", redisKey, ex);
                throw new HisException("生成二维码失败", ex);
            }
        }
        // 重试结束仍未获取到或未成功生成二维码
        throw new HisException("获取支付二维码失败，请稍后重试");
    }

    /**
     * 支付宝支付结果回调处理
     * @param request
     * @return  支付结果
     */
    @Override
    @Transactional
    public String handleNotify(HttpServletRequest request) {

        log.info("接收到支付宝异步通知");

        Map<String, String> params = new HashMap<>();
        // 1. 遍历参数，添加到 params 中
        request.getParameterMap().forEach((name, values) -> {
            params.put(name, values[0]);
            log.debug("通知参数 - {}: {}", name, values[0]);
        });

        // 订单号和交易状态
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");

        try {
            // 2. 验签：防止伪造通知
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(), // 使用支付宝公钥验签
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );

            if (signVerified) {
                log.info("支付宝异步通知验签成功，订单号: {}, 交易状态: {}", outTradeNo, tradeStatus);

                // 3. 判断支付是否成功
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 支付成功，更新订单状态
                    Boolean updateResult = orderService.updateOrderStatus(outTradeNo, tradeNo, OrderStatusEnum.UNPAID.getCode(), OrderStatusEnum.PAID.getCode());

                    if (updateResult) {
                        // 获取订单客户 id，推送消息到前端
                        log.debug("订单付款成功，已更新订单状态");
                        // WebSocket 推送成功支付消息到前端页面
                        paymentNotificationService.sendPaymentResult(outTradeNo, "success");
                    } else {
                        log.error("订单付款成功，但更新订单状态失败，订单号: {}", outTradeNo);
                    }
                    // 支付成功后，删除 redis 中缓存的二维码数据
                    String redisKey = String.format("alipay:qrcode:%s", outTradeNo);
                    redisTemplate.delete(redisKey);
                    log.info("订单支付成功: {}, 支付宝交易号: {}", outTradeNo, params.get("trade_no"));
                    // 必须返回纯文本 "success"，且大小写敏感
                    return "success";
                } else {
                    log.info("支付未成功，订单号: {}, 状态: {}", outTradeNo, tradeStatus);
                    paymentNotificationService.sendPaymentResult(outTradeNo, "failure");
                    return "failure";
                }
            } else {
                log.error("支付宝异步通知验签失败，订单号: {}", outTradeNo);
                return "failure";
            }
        } catch (Exception e) {
            log.error("处理支付宝异步通知异常，订单号: {}", outTradeNo, e);
            return "failure";
        }
    }
}
