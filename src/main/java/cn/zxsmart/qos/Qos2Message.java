package cn.zxsmart.qos;

import cn.zxsmart.MessageStatus;
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
public class Qos2Message extends AbstractMqttMessage {

    private EventLoop eventLoop;

    private ScheduledFuture scheduleRel;

    private ScheduledFuture scheduleCancle;

    public Qos2Message(MqttMessage oriMessage,
                       Integer messageId,
                       Channel channel,
                       EventLoop eventLoop,
                       MqttConfig config,
                       String topic,
                       boolean receive) {
        super(oriMessage, messageId, channel, eventLoop, config, topic);
        this.eventLoop = eventLoop;
        status = receive ? 1 : 0;
    }

    @Override
    public Future publish() {
        result = new DefaultPromise<>(eventLoop);
        //发送信息
        Future future = super.publish();
        //开始重发的定时任务
        scheduleRePublish(config.getRePublishFrequecy());

        //开始取消publish计时
        scheduleCancle(config.getPublishTimeout());
        //释放定时任务
        result.addListener(f -> {
            if (f.isDone()) {
                close();
            }
        });
        return result;
    }

    public Future publicRec() {
        //REC消息不需要重复发送，又发送方来确定
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC,
                false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, variableHeader);
        logger.debug("MQTT: send rec message. packetId = " +  messageId);
        status = MessageStatus.REC;
        return sendMessage(channel, mqttMessage);
    }

    public Future publishRel() {
        rePublish.cancel(true);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL,
                false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, variableHeader);
        Future future = sendMessage(channel, mqttMessage);
        logger.debug("MQTT: send rel message. packetId = " + messageId);
        this.status = MessageStatus.REL;
        //调用定时发送
        schduleReRel(config.getRePublishFrequecy());
        return future;
    }

    public static Future publishComp(Channel channel, int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,
                false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, variableHeader);
        if (channel != null && channel.isActive()) {
            return channel.writeAndFlush(mqttMessage);
        }
        return new DefaultPromise(channel.eventLoop()).setFailure(new MqttChannelCloseException());
    }

    private void schduleReRel(int frequence) {
        MqttFixedHeader fixedHeader = oriMessage.fixedHeader();
        MqttFixedHeader newFuxHeader = new MqttFixedHeader(fixedHeader.messageType(),
                true, fixedHeader.qosLevel(),
                fixedHeader.isRetain(),
                fixedHeader.remainingLength());
        MqttMessage mqttMessage = new MqttMessage(newFuxHeader, oriMessage.variableHeader());
        scheduleRel = eventLoop.scheduleAtFixedRate(() -> {
                         logger.debug("MQTT: resend rel message. packetId = "
                                 +  messageId);
                        sendMessage(channel, mqttMessage);
                    }, frequence, frequence, TimeUnit.SECONDS);
    }

    @Override
    public Future scheduleCancle(int publishTimeout) {
        if (Integer.MAX_VALUE == publishTimeout) {
            //如果没有设置就不取消
            return null;
        }
        scheduleCancle = eventLoop.schedule(() -> {
            if (!result.isDone()) {
                synchronized (result) {
                    logger.debug("MQTT: publish timeout packetId = " + messageId);
                    result.setFailure(new MqttTimeoutException("publish timeout", messageId));
                }
            }
        }, publishTimeout, TimeUnit.SECONDS);
        return scheduleCancle;
    }


    @Override
    public void cancle() {
        if (result.isDone()) {
            return;
        } else {
            synchronized (result) {
                if (!result.isDone()) {
                    logger.debug("MQTT: publish cancle packetId = " + messageId);
                    result.setFailure(new MqttException("MQTT: publish cancle packedId = "
                                +  messageId));
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
        if (scheduleRel != null) {
            scheduleRel.cancel(true);
            scheduleRel = null;
        }
    }
}
