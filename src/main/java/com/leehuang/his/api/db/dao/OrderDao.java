package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.front.dto.order.vo.OrderListVO;
import com.leehuang.his.api.mis.dto.customerIm.dto.OrderStatisticDTO;
import com.leehuang.his.api.mis.dto.order.dto.OrderAppointmentFinishedDTO;
import com.leehuang.his.api.mis.dto.order.request.SearchOrderByPageRequest;
import com.leehuang.his.api.mis.dto.order.vo.MisOrderPageVO;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_order(订单表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.OrderEntity
*/
public interface OrderDao extends BaseMapper<OrderEntity> {

    // 查询当前用户一天内未付款或退款的订单数量
    boolean hasIllegalOrder(@Param("customerId") int customerId);

    // 插入新订单数据
    void insertOrder(OrderEntity orderEntity);

    // 根据外部订单号查询订单信息
    OrderEntity searchOrderByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    // 更新订单状态
    int updateOrderStatus(
            @Param("outTradeNo") String outTradeNo,
            @Param("tradeNo") String tradeNo,
            @Param("expectStatus") Integer expectStatus,
            @Param("targetStatus") Integer targetStatus
    );

    // 查询订单状态
    Integer checkOrderStatus(@Param("outTradeNo") String outTradeNo, @Param("customerId") int customerId);

    // 获取分页查询的数据总数
    int searchCountByPage(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("customerId") Integer customerId
    );

    // 查询当前用户的所有订单
    List<OrderListVO> searchOrderListByPage(
            @Param("start") Integer start,
            @Param("length") Integer length,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("customerId") Integer customerId
    );

    // 更新支付方式和地址 id
    int updatePaymentType(
            @Param("outTradeNo") String outTradeNo,
            @Param("addressId") Integer addressId,
            @Param("paymentType") String paymentType
    );

    // 获取订单的实际支付金额
    BigDecimal getPayableAmount(@Param("outTradeNo") String outTradeNo);

    // 获取订单支付信息
    OrderEntity searchOrderPayInfo(String outTradeNo);

    // 本系统自动关闭订单
    int autoCloseOrder(@Param("outTradeNo") String outTradeNo);

    // 手动关闭订单
    int updateStatusToClosed(@Param("outTradeNo") String outTradeNo, @Param("paymentType") String paymentType);

    /**
     * 查询超时未支付订单ID列表
     * @param deadline 截止时间
     * @param status 订单状态
     * @param limit 每次处理数量
     * @return 订单ID列表
     */
    List<Integer> selectOverdueOrderIds(@Param("deadline") LocalDateTime deadline,
                                     @Param("status") Integer status,
                                     @Param("limit") Integer limit);

    /**
     * 批量更新订单状态
     * @param orderIds 订单ID列表
     * @param newStatus 新状态
     * @param oldStatus 旧状态（用于乐观锁）
     * @return 更新数量
     */
    int batchUpdateOrderStatus(@Param("orderIds") List<Integer> orderIds,
                               @Param("newStatus") Integer newStatus,
                               @Param("oldStatus") Integer oldStatus);

    // mis 端订单管理页面分页数据
    List<MisOrderPageVO> searchMisOrderByPage(@Param("start") int start, @Param("pageRequest") SearchOrderByPageRequest pageRequest);

    // mis 端订单管理页面分页数据总数
    long searchMisOrderCountByPage(SearchOrderByPageRequest pageRequest);

    // 获取用户的订单统计数据
    OrderStatisticDTO searchOrderStatistic(@Param("customerId") Integer customerId);

    // 统计订单已完成的预约
    OrderAppointmentFinishedDTO searchOrderFinished(@Param("uuid") String uuid);
}




