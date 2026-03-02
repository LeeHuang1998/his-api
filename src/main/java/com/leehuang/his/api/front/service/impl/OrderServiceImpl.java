package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.alibaba.fastjson.JSON;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.enums.OrderStatusEnum;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.db.dao.AddressDao;
import com.leehuang.his.api.db.dao.GoodsDao;
import com.leehuang.his.api.db.dao.GoodsSnapshotDao;
import com.leehuang.his.api.db.dao.OrderDao;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.db.pojo.GoodsSnapShotDTO;
import com.leehuang.his.api.db.pojo.GoodsSnapshotEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.address.vo.AddressVO;
import com.leehuang.his.api.front.dto.order.request.OutTradeNoRequest;
import com.leehuang.his.api.front.dto.order.request.OrderRequest;
import com.leehuang.his.api.front.dto.order.request.RefundOrderRequest;
import com.leehuang.his.api.front.dto.order.vo.OrderGoodsVO;
import com.leehuang.his.api.front.dto.order.vo.OrderDetailVO;
import com.leehuang.his.api.front.dto.order.vo.OrderListVO;
import com.leehuang.his.api.front.service.AlipayService;
import com.leehuang.his.api.front.service.OrderService;

import com.leehuang.his.api.mis.dto.goods.vo.CheckupItemVo;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service("orderService")
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;

    private final GoodsDao goodsDao;

    private final Snowflake snowflake;

    private final GoodsSnapshotDao goodsSnapshotDao;

    private final AddressDao addressDao;

    private final AlipayService alipayService;

    private final MinioProperties minioProperties;

    private final StringRedisTemplate redisTemplate;

    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 创建订单
     * @param request
     * @return
     */
    @Override
    @Transactional
    public String createOrder(OrderRequest request) {
        Integer goodsId = request.getId();
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        Integer goodsNum = request.getGoodsNum();

        // 1. 判断当前用户是否满足下单条件：是否有 10 个以上未付款订单或者 5 个以上退款订单
        boolean illegal = orderDao.hasIllegalOrder(customerId);
        if (illegal) {
            throw new HisException("当前用户不满足下单条件，有超过 10 个未付款订单或本日有 5 个以上的退款订单");
        }

        // 2. 判断当前订单购买的商品是否有商品快照，若没有则创建商品快照，将客户购买订单时的商品信息保存到 MongoDB 中（注意时商品快照，而不是订单快照，只用于保存当前状态下的商品信息）
        //  使用 MongoDB 存储商品快照，无论商家如何修改商品，保证不会影响到用户购买时商品的信息，确保订单创建时的商品信息不会改变
        // 2.1 根据传入的商品 id 从数据库中查询商品信息（snapShotDTO 用于提供 md5 查询快照信息以及后续插入订单数据时提供商品信息）
        GoodsSnapShotDTO snapShotDTO = goodsDao.searchGoodsSnapshotById(goodsId);

        if (snapShotDTO == null) {
            throw new HisException("商品不存在");
        }

        try {
            // 商品快照信息和订单信息只需要一张封面图片
            String image = objectMapper.readValue(snapShotDTO.getImage(), String[].class)[0];
            snapShotDTO.setImage(image);

            // 2.2 根据查询出来的商品信息获取 MongoDB 中的数据，若不存在则存储到 MongoDB 中，若存在则使用该数据
            String snapshot_id = goodsSnapshotDao.findSnapshotIdByMd5(snapShotDTO.getMd5());
            // 2.3 商品快照不存在，将当前商品信息快照插入到 MongoDB 中
            if (snapshot_id == null) {
                // 创建商品快照对象
                GoodsSnapshotEntity snapshotEntity = new GoodsSnapshotEntity();

                String[] ignoreProperties = {"checkup1", "checkup2", "checkup3", "checkup4", "checkup", "image", "tag"};
                BeanUtil.copyProperties(snapShotDTO, snapshotEntity, ignoreProperties);

                snapshotEntity.setGoodsId(goodsId);
                snapshotEntity.setImage(image);

                // 转换类型
                if (snapShotDTO.getTag() != null && !snapShotDTO.getTag().isBlank()) {
                    snapshotEntity.setTag(objectMapper.readValue(snapShotDTO.getTag(), String[].class));
                }
                if (snapShotDTO.getCheckup1() != null && !snapShotDTO.getCheckup1().isBlank()) {
                    snapshotEntity.setCheckup1(objectMapper.readValue(snapShotDTO.getCheckup1(), new TypeReference<List<CheckupItemVo>>() {}));
                }
                if (snapShotDTO.getCheckup2() != null && !snapShotDTO.getCheckup2().isBlank()) {
                    snapshotEntity.setCheckup2(objectMapper.readValue(snapShotDTO.getCheckup2(), new TypeReference<List<CheckupItemVo>>() {}));
                }
                if (snapShotDTO.getCheckup3() != null && !snapShotDTO.getCheckup3().isBlank()) {
                    snapshotEntity.setCheckup3(objectMapper.readValue(snapShotDTO.getCheckup3(), new TypeReference<List<CheckupItemVo>>() {}));
                }
                if (snapShotDTO.getCheckup4() != null && !snapShotDTO.getCheckup4().isBlank()) {
                    snapshotEntity.setCheckup4(objectMapper.readValue(snapShotDTO.getCheckup4(), new TypeReference<List<CheckupItemVo>>() {}));
                }
                if (snapShotDTO.getCheckup() != null && !snapShotDTO.getCheckup().isBlank()) {
                    snapshotEntity.setCheckup(objectMapper.readValue(snapShotDTO.getCheckup(), new TypeReference<List<CheckupVO>>() {}));
                }

                // 类型转换完成后，插入到 MongoDB 中
                snapshot_id = goodsSnapshotDao.insert(snapshotEntity);
            }

            // 3. 创建订单，插入到数据库中，返回主键
            OrderEntity orderEntity = buildOrder(snapshot_id, goodsId, goodsNum, snapShotDTO);
            orderDao.insertOrder(orderEntity);

            // 4. 返回订单流水号
            return orderEntity.getOutTradeNo();
        } catch (JsonProcessingException e) {
            log.error("JSON 转换异常", e);
            throw new IllegalArgumentException("JSON 转换异常");
        }
    }

    /**
     * 构建订单
     * @param snapshot_id           快照 ID
     * @param goodsId               商品 ID
     * @param goodsNum              商品数量
     * @param snapShotDTO           快照信息
     * @return
     */
    private OrderEntity buildOrder(String snapshot_id, Integer goodsId, Integer goodsNum, GoodsSnapShotDTO snapShotDTO) {

        OrderEntity orderEntity = new OrderEntity();

        int customerId = StpCustomerUtil.getLoginIdAsInt();

        // 1. 雪花算法生成 订单流水号
        String outTradeNo =  snowflake.nextIdStr();

        orderEntity.setCustomerId(customerId);
        orderEntity.setGoodsId(goodsId);
        orderEntity.setSnapshotId(snapshot_id);

        // 2. 获取用户默认地址，若没有则不设置，到前端页面中再选择
        AddressVO defaultAddress = addressDao.getDefaultAddress(customerId);
        if (defaultAddress != null) {
            orderEntity.setAddressId(defaultAddress.getId());
        }

        // 3. 设置订单信息
        orderEntity.setOutTradeNo(outTradeNo);
        orderEntity.setGoodsTitle(snapShotDTO.getTitle());
        orderEntity.setGoodsPrice(snapShotDTO.getCurrentPrice());
        orderEntity.setNumber(goodsNum);
        orderEntity.setGoodsDescription(snapShotDTO.getDescription());
        orderEntity.setStatus(1);

        // 4. 设置订单金额
        BigDecimal payableAmount = calculateAmount(snapShotDTO.getRule(), goodsNum, snapShotDTO.getCurrentPrice());
        BigDecimal totalAmount = orderEntity.getGoodsPrice().multiply(BigDecimal.valueOf(orderEntity.getNumber())).setScale(2, RoundingMode.HALF_UP);

        orderEntity.setPayableAmount(payableAmount);
        orderEntity.setTotalAmount(totalAmount);
        orderEntity.setDiscountAmount(totalAmount.subtract(payableAmount));

        // 5. 设置商品图片
        orderEntity.setGoodsImage(snapShotDTO.getImage());

        return orderEntity;
    }

    /**
     * 规则引擎黑名单：
     *      编译一个正则表达式模式，用来检测字符串中是否包含任意一个敏感关键字（黑名单词），并且忽略大小写，只匹配完整单词。
     * Pattern：用于预编译正则表达式，生成一个可复用的匹配模板，避免每次匹配都重新解析正则，性能更高、线程安全。
     *      \b：表示单词边界，只检测当前位置前后是否是 “单词字符与非单词字符” 的交界处。例如：\bclass\b 只会匹配 class，而不会匹配 classroom
     *      (?: ... )：匹配任意一个敏感关键字。其中 ?: 的意思是 “只匹配不捕获”，捕获是指在匹配到结果后，会被分配一个编号组（从 1 开始）中，里面存放着捕获结果
     *                 由于这里只关心是否匹配而不关心是哪个关键词被匹配，所以不需要捕获组
     *      Pattern.CASE_INSENSITIVE：告诉正则引擎，不区分大小写
     */
    private static final Pattern BLACKWORD_PATTERN = Pattern.compile(
            "\\b(?:runtime|process|exec|system|reflect|class|thread|file|url|http|https|script|processbuilder)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 在不调用 addFunction()、addOperator()、loadDynamicLib()、addMacro() 等动态注册或修改方法时，ExpressRunner 的使用是线程安全的
    private static final ExpressRunner RUNNER = new ExpressRunner();

    private BigDecimal calculateAmount(String rule, Integer goodsNum, BigDecimal currentPrice) {
        // 参数校验
        if (goodsNum == null || goodsNum <= 0) {
            throw new HisException("商品数量不合法");
        }
        if (currentPrice == null) {
            throw new HisException("商品价格为空");
        }

        // 无规则直接返回总价
        if (StringUtils.isBlank(rule)) {
            return currentPrice
                    .multiply(BigDecimal.valueOf(goodsNum))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 黑名单检查
        if (BLACKWORD_PATTERN.matcher(rule).find()) {
            throw new HisException("规则含非法关键字，禁止使用系统/运行时相关调用");
        }

        log.debug("传入的商品数量: {}, 商品价格: {}", goodsNum, currentPrice);

        // 构建 QLExpress 上下文
        DefaultContext<String, Object> context = new DefaultContext<>();
        context.put("number", BigDecimal.valueOf(goodsNum));
        context.put("price", currentPrice);

        try {
            // 执行规则表达式
            Object result = RUNNER.execute(rule, context, null, true, false);

            if (result == null) {
                throw new HisException("规则计算结果为空");
            }

            // 转换结果为 BigDecimal
            BigDecimal amount = new BigDecimal(result.toString())
                    .setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new HisException("规则计算结果异常（非正数）");
            }

            return amount;
        } catch (Exception e) {
            log.error("规则计算失败, rule: {}, number={}, price={}", rule, goodsNum, currentPrice, e);
            throw new HisException("规则计算失败", e);
        }
    }

    /**
     * 获取订单详情
     * @param outTradeNo
     * @return
     */
    @Override
    public OrderDetailVO getOrderDetail(String outTradeNo) {
        // 获取订单信息
        OrderEntity orderEntity = orderDao.searchOrderByOutTradeNo(outTradeNo);

        if (orderEntity == null) {
            throw new HisException("订单不存在");
        }

        if (orderEntity.getCustomerId() != StpCustomerUtil.getLoginIdAsInt()) {
            throw new HisException("订单不属于当前用户，无权查看");
        }

        // 1. orderVO 中的商品信息 orderGoodsVO
        OrderGoodsVO orderGoodsVO = new OrderGoodsVO();

        // 1.1 从 mongoDB 中获取商品快照信息
        GoodsSnapshotEntity goodsSnapshot = goodsSnapshotDao.getGoodsSnapshotById(orderEntity.getSnapshotId());

        // 1.2 从商品快照信息中设置数据到 orderGoodsVO 中
        orderGoodsVO.setTitle(goodsSnapshot.getTitle());
        orderGoodsVO.setDescription(goodsSnapshot.getDescription());
        orderGoodsVO.setRuleName(goodsSnapshot.getRuleName());

        orderGoodsVO.setImages(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + goodsSnapshot.getImage());

        // 2. 构建 orderVO
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setNumber(orderEntity.getNumber());
        orderDetailVO.setStatus(orderEntity.getStatus());

        // 2.1 订单商品信息
        orderDetailVO.setGoodsVO(orderGoodsVO);

        // 2.2 订单地址信息，若为空则使用默认地址，若默认地址也为空则让用户自己更新订单地址
        if (orderEntity.getAddressId() != null) {
            orderDetailVO.setAddressVO(addressDao.getAddressById(orderEntity.getAddressId(), orderEntity.getCustomerId()));
        } else {
            AddressVO defaultAddress = addressDao.getDefaultAddress(orderEntity.getCustomerId());
            if (defaultAddress != null) {
                orderDetailVO.setAddressVO(defaultAddress);
            }
        }

        // 2.3 订单支付方式信息，若为空则让用户手动选择
        String payment = orderEntity.getPaymentType();
        if ( payment != null) {
            orderDetailVO.setPaymentType(payment);
        }
        // 同上
        String orderNotes = orderEntity.getOrderNotes();
        if (orderNotes != null) {
            orderDetailVO.setOrderNotes(orderNotes);
        }

        // 2.4 订单金额信息（应该要使用订单表中的数据而不是快照中的数据）
        orderGoodsVO.setCurrentPrice(orderEntity.getGoodsPrice());

        orderDetailVO.setOrderTotalAmount(orderEntity.getTotalAmount());
        orderDetailVO.setPayableAmount(orderEntity.getPayableAmount());
        orderDetailVO.setDiscountAmount(orderEntity.getDiscountAmount());

        // 2.5 设置订单时间
        orderDetailVO.setCreateTime(orderEntity.getCreateTime());

        // 2.6 设置订单流水号
        orderDetailVO.setOutTradeNo(outTradeNo);

        System.out.println("订单详细数据展示：" + orderDetailVO);

        return orderDetailVO;
    }

    /**
     * 更新订单状态
     * @param outTradeNo        订单号
     * @param tradeNo           交易号
     * @param expectStatus      原来的订单状态
     * @param targetStatus      修改后的订单状态
     * @return
     */
    @Override
    public Boolean updateOrderStatus(String outTradeNo, String tradeNo, Integer expectStatus, Integer targetStatus) {
        int i = orderDao.updateOrderStatus(outTradeNo, tradeNo, expectStatus, targetStatus);
        return i > 0;
    }

    /**
     *
     * @param outTradeNo
     * @return
     */
    @Override
    public Integer checkOrderStatus(String outTradeNo) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        OrderEntity orderEntity = orderDao.searchOrderPayInfo(outTradeNo);

        if (orderEntity == null) {
            throw new HisException("订单不存在");
        }

        if (orderEntity.getCustomerId() != customerId) {
            throw new HisException("无权访问该订单");
        }

        return orderEntity.getStatus();
    }

    /**
     * 根据订单流水号获取客户 id
     * @param outTradeNo
     * @return
     */
    @Override
    public Integer searchCustomerId(String outTradeNo) {
        OrderEntity orderEntity = orderDao.searchOrderPayInfo(outTradeNo);
        if (orderEntity == null) {
            throw new HisException("订单不存在");
        }
        return orderEntity.getCustomerId();
    }

    /**
     * 根据客户 id 条件分页查询订单列表
     * @param page           页码
     * @param length         每页条数
     * @param keyword        关键字
     * @param status         订单状态
     * @param customerId     客户 id
     * @return               分页数据
     */
    @Override
    public PageUtils<OrderListVO> searchOrderListByCustomerId(
            Integer page,
            Integer length,
            String keyword,
            Integer status,
            Integer customerId
    ) {
        // 参数校验
        if (page == null || page < 1) page = 1;
        if (length == null || length < 1) length = 5;
        if (length > 20) length = 20;                               // 防止过度查询
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("无效的 customerId");
        }

        // 获取分页起始记录 id
        int start = (page - 1) * length;

        // 根据条件分页查询总数
        int total = orderDao.searchCountByPage(keyword, status, customerId);

        if (total == 0) {
            return new PageUtils<>(0, length, page, Collections.emptyList());
        }

        // 数据不为空，则条件分页查询数据
        List<OrderListVO> orderListVO = orderDao.searchOrderListByPage(start, length, keyword, status, customerId);

        // 设置商品图片
        orderListVO.forEach(order -> {
            order.setGoodsImage(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + order.getGoodsImage());
            order.setDisabled(order.getStatus() == 1 &&
                    Duration.between(order.getCreateTime(), LocalDateTime.now()).toMinutes() > 20);
        });

    // 创建并返回分页结果对象
        return new PageUtils<>(total, length, page, orderListVO);
    }

    /**
     * 更新订单的支付方式和地址 id
     * @param outTradeNo        订单流水号
     * @param addressId         地址 id
     * @param paymentType       支付方式
     */
    @Override
    public void updatePaymentType(String outTradeNo, Integer addressId, String paymentType) {
        int rows = orderDao.updatePaymentType(outTradeNo, addressId, paymentType);
        if (rows != 1) {
            throw new HisException("更新订单支付失败，订单不存在或订单不可用");
        }
    }

    /**
     * 获取订单的实际支付金额
     * @param outTradeNo
     * @return
     */
    @Override
    public BigDecimal getPayableAmount(String outTradeNo) {
        return orderDao.getPayableAmount(outTradeNo);
    }

    /**
     * 获取订单支付信息
     * @param outTradeNo    订单流水号
     * @return              订单信息
     */
    @Override
    public OrderEntity getOrderPayInfo(String outTradeNo) {
        return orderDao.searchOrderPayInfo(outTradeNo);
    }

    /**
     * 关闭超时未支付订单
     * @param overdueMinutes                超时分钟数
     * @param batchSize                     每次处理数量
     * @return                              成功关闭的订单数量
     */
    @Transactional
    @Override
    public int batchCloseExpiredOrder(int overdueMinutes, int batchSize) {
        // 计算截止时间
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(overdueMinutes);

        // 查询超时订单ID
        List<Integer> overdueOrderIds = orderDao.selectOverdueOrderIds(
                deadline, OrderStatusEnum.UNPAID.getCode(), batchSize);

        if (overdueOrderIds.isEmpty()) {
            log.info("未找到超时未支付订单");
            return 0;
        }

        log.info("找到 {} 个超时未支付订单，开始批量关闭", overdueOrderIds.size());

        // 批量更新订单状态
        int successCount = orderDao.batchUpdateOrderStatus(
                overdueOrderIds,
                OrderStatusEnum.CLOSED.getCode(),
                OrderStatusEnum.UNPAID.getCode());

        log.info("成功关闭 {} 个超时订单", successCount);
        return successCount;
    }

    /**
     *  关闭订单
     * @param request       订单流水号以及是否为自动关闭
     */
    @Override
    @Transactional
    public String closeOrder(OutTradeNoRequest request) {

        String outTradeNo = request.getOutTradeNo();

        // 1. 查询本系统订单
        OrderEntity order = orderDao.searchOrderPayInfo(outTradeNo);
        if (order == null) {
            throw new HisException("订单不存在");
        }

        // 2. 若订单已不是待支付状态，说明：已支付 or 已关闭
        if (!Objects.equals(order.getStatus(), OrderStatusEnum.UNPAID.getCode())) {
            // 返回订单状态
            return "关闭失败，订单状态：" + OrderStatusEnum.getMsgByCode(order.getStatus());
        }

        // 3. 获取支付方式
        String paymentType = order.getPaymentType();

        try {
            // 4. 关闭订单
            closeOrderByType(outTradeNo, paymentType);
            return "订单关闭成功";
        } catch (Exception e) {
            throw new HisException("关闭订单异常，请联系技术人员", e);
        }
    }

    /**
     * 订单退款
     * @param request   退款请求参数
     */
    @Override
    public void refundOrder(RefundOrderRequest request, Integer customerId) {
        // 添加请求日志
        log.info("用户申请退款, customerId: {}, outTradeNo: {}, refundAmount: {}",
                customerId, request.getOutTradeNo(), request.getRefundAmount());

        // 1. 先获取本系统订单核对数据
        OrderEntity orderEntity = orderDao.selectOne(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getOutTradeNo, request.getOutTradeNo())
        );

        if (orderEntity == null) {
            throw new HisException("订单不存在");
        }

        // 2. 校验订单是否可以退款
        validateRefundOrder(orderEntity, request, customerId);

        // 3. 生成退款单号
        String outRefundNo = "REFUND_" + snowflake.nextIdStr();

        // 4. 更新订单状态为退款中
        boolean updated = updateOrderStatusWithVersion(
                orderEntity.getOutTradeNo(),
                OrderStatusEnum.PAID.getCode(),             // 期望原状态
                OrderStatusEnum.REFUNDING.getCode(),        // 目标状态
                outRefundNo,
                null,                           // refundAmount
                buildOrderNotes(orderEntity, request)
        );
        if (!updated) {
            throw new HisException("订单状态已被其他操作更改，请刷新后重试");
        }

        // 4. 支付宝退款
        try {
            AlipayTradeRefundResponse refundResp = alipayService.refund(
                    request.getOutTradeNo(),
                    request.getRefundAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    request.getRefundReason(),
                    outRefundNo
            );

            // 5. 更新订单状态为已退款
            handleRefundResult(orderEntity.getOutTradeNo(), request, refundResp);

            log.info("【退款成功】outTradeNo={}, refundNo={}, amount={}",
                    orderEntity.getOutTradeNo(), outRefundNo, request.getRefundAmount());
        } catch (Exception e) {
            // 记录退款失败状态
            log.error("退款失败, outTradeNo: {}", request.getOutTradeNo(), e);
            throw new HisException("退款处理中，请稍后查询结果");
        }
    }

    /**
     * 查询退款订单
     * @param request
     * @return
     */
    @Override
    public String checkRefund(OutTradeNoRequest request) {
        OrderEntity orderEntity = orderDao.selectOne(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getOutTradeNo, request.getOutTradeNo())
        );

        if (orderEntity == null) {
            throw new HisException("订单不存在");
        }

        if (orderEntity.getOutRefundNo() == null) {
            return "该订单没有发起退款请求";
        }

        // 退款失败，允许重试
        if (OrderStatusEnum.REFUND_FAILED.getCode().equals(orderEntity.getStatus())) {
            return String.format("退款失败，原因：%s。请重新申请退款",
                    StringUtils.substringAfter(orderEntity.getOrderNotes(), "退款失败："));
        }

        // 退款中
        if (OrderStatusEnum.REFUNDING.getCode().equals(orderEntity.getStatus())) {
            log.info("订单仍在退款中，主动查询支付宝状态，订单号: {}", request.getOutTradeNo());

            // 发送请求到支付宝，同步支付宝状态到本地
            boolean synced = syncRefundStatusWithVersionInTx(request.getOutTradeNo());
            // 同步订单状态到数据库成功
            if (synced) {
                // 重新查询获取最新状态
                orderEntity = orderDao.selectOne(
                        new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOutTradeNo, request.getOutTradeNo())
                );
            }
        }

        // 格式化返回结果
        return formatRefundResult(orderEntity);
    }

    /**
     * 根据支付方式关闭订单
     * @param outTradeNo    订单流水号
     * @param paymentType   支付方式
     * @return  关闭结果
     */
    private void closeOrderByType(String outTradeNo, String paymentType) {
        if ("alipay".equalsIgnoreCase(paymentType)) {
            alipayCloseOrder(outTradeNo);
            log.info("支付宝关单成功: {}", outTradeNo);
        } else {
            try {
                log.info("订单 {} 使用其他支付方式（{}），直接关闭本地订单", outTradeNo, paymentType);
                // 其他支付方式：无需调第三方，直接关本地
                updateStatusToCloseOrder(outTradeNo, paymentType);
            } catch (Exception e) {
                log.error("关闭其他支付方式订单异常，订单号: {}, 支付方式: {}", outTradeNo, paymentType, e);
                throw new HisException("关闭其他支付方式订单异常", e);
            }
        }
    }

    /**
     * 本地关闭订单
     * @param outTradeNo        订单流水号
     * @param paymentType       支付平台
     */
    private void updateStatusToCloseOrder(String outTradeNo, String paymentType) {
        // 本系统关闭订单
        int rows = orderDao.updateStatusToClosed(outTradeNo, paymentType);
        if (rows != 1) {
            throw new HisException("更新订单状态失败：" + outTradeNo);
        }
    }

    /**
     * 支付宝支付的订单关单
     * @param outTradeNo        订单流水号
     * @return                  关闭结果
     */
    private void alipayCloseOrder(String outTradeNo) {
        try {
            // 查询支付宝订单，用户扫码后会生成待付款订单，判断订单的状态是否为待支付
            AlipayTradeQueryResponse queryResp = alipayService.queryOrder(outTradeNo);
            // 关闭支付宝订单
            if ("WAIT_BUYER_PAY".equals(queryResp.getTradeStatus())) {
                alipayService.closeOrder(outTradeNo);
            }
            // 本系统关闭订单
            updateStatusToCloseOrder(outTradeNo, "alipay");
            // redis 中删除 qrcode 的数据
            String redisKey = String.format("alipay:qrcode:%s", outTradeNo);
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("支付宝关单流程异常，订单号: {}", outTradeNo, e);
            throw new HisException("支付宝关单失败", e);
        }
    }

    /**
     * TODO 微信支付的订单关单（未完成）
     * @param outTradeNo
     * @return
     */
    private boolean wechatPayCloseOrder(String outTradeNo) {
        updateStatusToCloseOrder(outTradeNo, "wechat");
        return true;
    }

    /**
     * 校验订单是否符合退款要求
     * @param entity            数据库中的订单数据
     * @param request            请求参数
     * @param customerId         用户 id
     * @return
     */
    private void validateRefundOrder(OrderEntity entity, RefundOrderRequest request, Integer customerId) {
        // 1. 校验订单状态
        // 允许 PAID 和 REFUND_FAILED 状态退款
        if (!Objects.equals(entity.getStatus(), OrderStatusEnum.PAID.getCode()) &&
                !Objects.equals(entity.getStatus(), OrderStatusEnum.REFUND_FAILED.getCode())) {
            String msgByCode = OrderStatusEnum.getMsgByCode(entity.getStatus());
            throw new HisException("订单状态为【" + msgByCode + "】，无法退款");
        }

        // 2. 校验退款用户和订单用户是否一致
        if (!Objects.equals(entity.getCustomerId(), customerId)) {
            throw new HisException("订单不属于当前用户，无法操作");
        }

        // 3. 校验退款商品
        if (!Objects.equals(entity.getGoodsId(), request.getGoodsId())) {
            throw new HisException("请求退款的商品与订单商品不一致");
        }

        // 4. 校验退款数量
        if (!Objects.equals(entity.getNumber(), request.getGoodsCount())) {
            throw new HisException("请求退款的商品数量与订单不符（不支持部分退款）");
        }

        // 5. 校验退款金额
        if (request.getRefundAmount().compareTo(entity.getPayableAmount()) != 0) {
            throw new HisException("请求退款的金额与订单已付款金额不符");
        }

        // 6. 幂等性校验（防止重复退款）
        if (StringUtils.isNotBlank(entity.getOutRefundNo())) {
            throw new HisException("退款申请已提交，请勿重复操作");
        }
    }

    /**
     * 乐观锁事务更新订单状态
     * @param outTradeNo            订单号
     * @param expectedStatus        期望状态，只有订单在期望状态下才可更新
     * @param targetStatus          目标状态，订单更新的目标状态
     * @param outRefundNo           退款单号
     * @param refundAmount          退款金额
     * @param orderNotes            订单备注
     * @return                      订单状态更新结果
     */
    private boolean updateOrderStatusWithVersion (String outTradeNo, Integer expectedStatus,
                                                  Integer targetStatus, String outRefundNo,
                                                  BigDecimal refundAmount, String orderNotes) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 1. 必须在事务内重新查询，获取最新 version 和数据，防其他代码路径修改，确保version最新
            OrderEntity currentOrder =
                    orderDao.selectOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOutTradeNo, outTradeNo));

            // 2. CAS校验，确保状态未被其他线程修改
            if (!Objects.equals(currentOrder.getStatus(), expectedStatus)) {
                log.warn("CAS校验失败，订单{}期望状态{}，实际状态{}",
                        outTradeNo, expectedStatus, currentOrder.getStatus());
                return false;
            }

            // 3. 更新数据库
            OrderEntity update = new OrderEntity();
            update.setId(currentOrder.getId());
            update.setStatus(targetStatus);
            update.setVersion(currentOrder.getVersion());

            if (outRefundNo != null) {
                update.setOutRefundNo(outRefundNo);
            }
            if (refundAmount != null) {
                update.setRefundAmount(refundAmount);
                update.setRefundTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (orderNotes != null) {
                update.setOrderNotes(orderNotes);
            }

            return orderDao.updateById(update) > 0;
        }));
    }

    /**
     * 处理退款结果
     * @param outTradeNo         订单号
     * @param request            退款请求参数
     * @param refundResp         支付宝退款请求响应
     */
    private void handleRefundResult(String outTradeNo, RefundOrderRequest request,
                                    AlipayTradeRefundResponse refundResp) {
        // Y 为资金变动，N 为资金未变动
        if (!"Y".equals(refundResp.getFundChange())) {
            // 退款未成功，更新为失败状态
            log.warn("退款未发生资金变动, outTradeNo={}, msg={}",
                    request.getOutTradeNo(), refundResp.getSubMsg());

            boolean updated = updateOrderStatusWithVersion(
                    outTradeNo,
                    OrderStatusEnum.REFUNDING.getCode(),
                    OrderStatusEnum.REFUND_FAILED.getCode(),
                    null, null,
                    "退款失败：" + refundResp.getSubMsg()
            );
            if (!updated) {
                log.error("更新退款失败状态失败，outTradeNo: {}", outTradeNo);
            }
            throw new HisException("退款未执行：" + refundResp.getSubMsg());
        }

        // 退款成功，更新状态
        boolean updated = updateOrderStatusWithVersion(
                outTradeNo,
                OrderStatusEnum.REFUNDING.getCode(),
                OrderStatusEnum.REFUNDED.getCode(),
                null,
                request.getRefundAmount(),
                null
        );

        if (!updated) {
            // 资金已退但状态更新失败
            log.error("【资金安全风险】退款成功但状态更新失败，outTradeNo: {}", outTradeNo);
            throw new HisException("退款处理中，请稍后查询结果");
        }

        log.info("【退款成功】outTradeNo={}, amount={}, refundTime={}",
                request.getOutTradeNo(),
                request.getRefundAmount(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(refundResp.getGmtRefundPay())
        );
    }

    /**
     * 构建订单备注
     */
    private String buildOrderNotes(OrderEntity order, RefundOrderRequest request) {
        String originalNotes = order.getOrderNotes() != null ? order.getOrderNotes() : "";

        // 预留 50 字符给退款信息，原备注超过 250 字符则截断
        int maxOriginalLength = 250;
        if (originalNotes.length() > maxOriginalLength) {
            originalNotes = originalNotes.substring(0, maxOriginalLength) + "...";
        }

        String refundInfo = String.format("\n【退款申请】%s 金额:¥%s 原因:%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                request.getRefundAmount(),
                request.getRefundReason());

        String result = originalNotes + refundInfo;

        return result.length() > 300 ? result.substring(0, 300) : result;
    }


    /**
     * 同步支付宝退款状态
     * @param outTradeNo     订单号
     * @return               订单更新结果
     */
    private boolean syncRefundStatusWithVersionInTx(String outTradeNo) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 1. 重新查询获取最新 version
            OrderEntity currentOrder = orderDao.selectOne(
                    new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOutTradeNo, outTradeNo)
            );

            // 若订单状态不是退款中，则说明退款请求已结束（可能为成功或失败）
            if (!OrderStatusEnum.REFUNDING.getCode().equals(currentOrder.getStatus())) {
                return false; // 已被更新
            }

            // 2. 查询支付宝
            AlipayTradeFastpayRefundQueryResponse response =
                    alipayService.queryRefund(outTradeNo, currentOrder.getOutRefundNo());

            // 3. 带版本号更新
            OrderEntity update = new OrderEntity();
            update.setId(currentOrder.getId());
            update.setVersion(currentOrder.getVersion());


            if ("REFUND_SUCCESS".equals(response.getRefundStatus())) {
                // 退款成功
                update.setStatus(OrderStatusEnum.REFUNDED.getCode());
                update.setRefundAmount(new BigDecimal(response.getRefundAmount()));
                update.setRefundDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                update.setRefundTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else if ("REFUND_CLOSED".equals(response.getRefundStatus())) {
                // 退款请求关闭，重新申请退款
                update.setStatus(OrderStatusEnum.REFUND_FAILED.getCode());
                update.setOrderNotes("退款关闭：" + response.getSubMsg());
            } else {
                return false;
            }

            return orderDao.updateById(update) > 0;
        }));
    }

    /**
     * 格式化退款结果
     * @param orderEntity
     * @return
     */
    private String formatRefundResult(OrderEntity orderEntity) {
        String statusMsg = OrderStatusEnum.getMsgByCode(orderEntity.getStatus());
        String refundTime = orderEntity.getRefundTime() != null ?
                orderEntity.getRefundTime() : "暂无";

        return String.format("【退款申请】\n退款时间：%s\n退款金额：%s\n退款结果：¥%s",
                refundTime,
                orderEntity.getRefundAmount() != null ? orderEntity.getRefundAmount() : "0.00",
                statusMsg);
    }
}
