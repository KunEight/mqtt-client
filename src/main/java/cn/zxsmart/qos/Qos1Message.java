package cn.zxsmart.qos;

import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.exception.MqttChannelCloseException;
import cn.zxsmart.exception.MqttException;
import cn.zxsmart.exception.MqttTimeoutException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

/**
 * @author zhengkun
 * @date 2021/6/15
 */
public class Qos1Message extends AbstractMqttMessage {

    private ScheduledFuture scheduleCancle;

    public Qos1Message(MqttMessage mqttPublishMessage,
                       Integer messageId,
                       Channel channel,
                       EventLoop eventLoop,
                       MqttConfig config,
                       String topic) {
        super(mqttPublishMessage, messageId, channel, eventLoop, config, topic);
    }

    @Override
    public Future publish() {
        result = new DefaultPromise<>(eventLoop);
        //发送信息
        Future future = super.publish();
        //开始重发的定时任务
        scheduleRePublish(config.getRePublishFrequecy());

        //开始取消publish并设置失败计时
        scheduleCancle(config.getPublishTimeout());
        //释放定时任务
        result.addListener(f -> {
            if (f.isDone()) {
                close();
            }
        });
        return result;
    }

    @Override
    public void cancle() {
        if (result.isDone()) {
            return;
        } else {
            synchronized (result) {
                if (!result.isDone()) {
                    logger.debug("public cancle packedId:" + messageId);
                    result.setFailure(new MqttException("MQTT: publish cancle packetId = "
                            + messageId));
                }
            }
        }
    }

    private void close() {
        if (oriMessage.payload() instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) oriMessage.payload();
            byteBuf.release();
        }

        if (scheduleCancle != null) {
            scheduleCancle.cancel(true);
            scheduleCancle = null;
        }
        if (rePublish != null) {
            rePublish.cancel(true);
            rePublish = null;
        }
    }

    @Override
    public Future scheduleCancle(int publishTimeout) {
        if (Integer.MAX_VALUE == publishTimeout) {
            //如果没有设置就不取消
            return null;
        }
        scheduleCancle = eventLoop.schedule(() -> {
            if (result.isDone()) {
                synchronized (result) {
                    logger.debug("public timeout packedId:" + messageId);
                    result.setFailure(new MqttTimeoutException("publish timeout", messageId));
                }
            }
        }, publishTimeout, TimeUnit.SECONDS);
        return scheduleCancle;
    }



    public static Future pubAck(MqttMessage message, Channel channel) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK,
                false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(
                ((MqttPublishVariableHeader)message.variableHeader()).packetId());
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, variableHeader);
        if (channel.isActive()) {
            return channel.writeAndFlush(mqttMessage);
        } else {
            throw new MqttChannelCloseException();
        }
    }

}
