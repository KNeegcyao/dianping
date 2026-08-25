package com.hmdp.listener;

import com.hmdp.config.QueueConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class SeckillVoucherListener {

    private final TransactionTemplate transactionTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private MessageConverter messageConverter;

    public SeckillVoucherListener(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 正常队列消费者
     */
    @RabbitListener(queues = QueueConfig.QUEUE_A)
    public void receivedA(Message message, Channel channel) {
        handleOrder(message, channel);
    }

    /**
     * 死信队列消费者（延迟重试后的补偿处理）
     */
    @RabbitListener(queues = QueueConfig.DEAD_LETTER_QUEUE_D)
    public void receivedD(Message message, Channel channel) {
        handleOrder(message, channel);
    }

    /**
     * 统一的订单落库 + 库存扣减逻辑。
     * 采用手动确认：处理成功 basicAck；任何异常 basicNack(requeue=false) 丢弃，
     * 避免毒消息被无限重投阻塞队列。
     */
    private void handleOrder(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            VoucherOrder voucherOrder = (VoucherOrder) messageConverter.fromMessage(message);
            Long voucherId = voucherOrder.getVoucherId();

            // 幂等校验 + 落库与扣减库存在同一事务：重复消费时按订单ID去重
            Boolean duplicated = transactionTemplate.execute(status -> {
                if (voucherOrderService.getById(voucherOrder.getId()) != null) {
                    return true;
                }
                voucherOrderService.save(voucherOrder);
                boolean success = seckillVoucherService.update()
                        .setSql("stock = stock - 1") // set stock = stock - 1
                        .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                        .update();
                if (!success) {
                    log.warn("库存扣减失败，可能已无库存，voucherId: {}", voucherId);
                }
                return false;
            });

            if (Boolean.TRUE.equals(duplicated)) {
                log.info("订单已存在，跳过重复处理，orderId: {}", voucherOrder.getId());
            }
            // 处理成功，手动确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费异常，拒绝并丢弃消息，deliveryTag: {}", deliveryTag, e);
            try {
                // 拒绝且不再重新投递，防止未确认消息无限重投
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioEx) {
                log.error("basicNack 失败，deliveryTag: {}", deliveryTag, ioEx);
            }
        }
    }
}