package com.leehuang.his.api.handler;

import com.leehuang.his.api.front.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderJobHandler {

    private final OrderService orderService;

    @Value("${app.order.overdue-minutes:30}")
    private Integer overdueMinutes;

    @Value("${app.order.batch-size:100}")
    private Integer batchSize;

    /**
     * 关闭超时未支付订单任务
     */
    @XxlJob("orderCloseJobHandler")
    public void closeOverdueOrders() {
        // 记录任务开始
        XxlJobHelper.log("开始执行批量关闭超时订单任务...");
        log.info("XXL-JOB: 开始执行批量关闭超时订单任务");

        try {
            // 记录任务参数
            XxlJobHelper.log("任务参数: overdueMinutes={}, batchSize={}", overdueMinutes, batchSize);

            // 执行订单关闭
            int successCount = orderService.batchCloseExpiredOrder(overdueMinutes, batchSize);

            // 记录任务结果
            String result = String.format("批量关闭订单任务执行完毕。成功关闭订单数: %d", successCount);
            XxlJobHelper.log(result);
            log.info("XXL-JOB: {}", result);

            // 标记任务成功
            XxlJobHelper.handleSuccess(result);

        } catch (Exception e) {
            String errorMsg = "关闭超时订单任务执行失败: " + e.getMessage();
            log.error("XXL-JOB: {}", errorMsg, e);
            XxlJobHelper.log(errorMsg);

            // 标记任务失败
            XxlJobHelper.handleFail(errorMsg);
            throw e;
        }
    }
}