package com.demo.university.scheduler;

import com.demo.university.service.OrderService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 超时关单：每 5 分钟扫描一次，30 分钟未付款的订单自动取消并回滚库存。
 * 用 JDK 自带的 ScheduledExecutorService，不额外引入 Quartz。
 */
public class OrderTimeoutJob {

    private static final Logger LOG = Logger.getLogger(OrderTimeoutJob.class.getName());
    private static final int TIMEOUT_MINUTES = 30;
    private static final int INTERVAL_MINUTES = 5;

    private final OrderService orderService = new OrderService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "order-timeout-job");
        thread.setDaemon(true);
        return thread;
    });

    public void start() {
        scheduler.scheduleWithFixedDelay(this::run, 1, INTERVAL_MINUTES, TimeUnit.MINUTES);
        LOG.info("超时关单任务已启动：每 " + INTERVAL_MINUTES + " 分钟扫描一次，超时阈值 "
                + TIMEOUT_MINUTES + " 分钟");
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void run() {
        try {
            int cancelled = orderService.cancelTimeoutOrders(TIMEOUT_MINUTES);
            if (cancelled > 0) {
                LOG.info("超时关单完成，本次取消 " + cancelled + " 笔订单");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "超时关单任务执行失败", e);
        }
    }
}
