package cn.zxsmart.qos;

import cn.zxsmart.MessageStatus;
import cn.zxsmart.SendMessage;
import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.exception.MqttException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import org.apache.log4j.Logger;

/**
 * @author zhengkun
 * @date 2021/6/15
 */
public abstract class AbstractMqttMessage implements SendMessage,ScheduleMessage {

    protected final Logger logger = Logger.getLogger(Qos0Message.class);

    protected int status;

    protected MqttMessage oriMessage;

    protected Object payload;

    protected Channel channel;

    protected MqttConfig config;

    protected Promise<Void> result;

    protected ScheduledFuture rePublish;

    protected EventLoop eventLoop;

    protected Integer messageId;

    private String topic;

    public AbstractMqttMessage(MqttMessage oriMessage,
                               Integer messageId,
                               Channel channel,
                               EventLoop eventLoop,
                               MqttConfig config,
                               String topic) {
        this.oriMessage = oriMessage;
        this.payload = oriMessage.payload();
        this.messageId = messageId;
        this.channel = channel;
        this.eventLoop = eventLoop;
        this.config = config;
        this.topic = topic;
        status = MessageStatus.UNSEND;
    }

    public Future publish() {
        if (status != MessageStatus.UNSEND) {
            throw new MqttException("message was sended");
        }
        //发送信息,netty默认发送成功后会自动释放掉消息，但是要重发就必须保存消息
        if (payload instanceof ByteBuf) {
            ((ByteBuf)payload).retain();
        }
        ChannelFuture future = this.sendMessage(channel, oriMessage);
        status = MessageStatus.SEND;
        logger.debug("MQTT: publish meessage qos = " + oriMessage.fixedHeader().qosLevel().value()
                + ",packedId = " + messageId);
        return future;
    }

    protected void scheduleRePublish(int timeout) {
        MqttFixedHeader fixedHeader = oriMessage.fixedHeader();
        MqttFixedHeader newFuxHeader = new MqttFixedHeader(fixedHeader.messageType(),
                true, fixedHeader.qosLevel(),
                fixedHeader.isRetain(),
                fixedHeader.remainingLength());
        Class<? extends MqttMessage> clazz = oriMessage.getClass();
        if (clazz.equals(MqttSubscribeMessage.class)) {
            MqttSubscribeMessage temp = (MqttSubscribeMessage) this.oriMessage;
            MqttSubscribeMessage subscribeMessage = new MqttSubscribeMessage(newFuxHeader,
                    temp.variableHeader(), temp.payload());
            rePublish = this.schedule(eventLoop, channel, timeout, subscribeMessage, messageId);
        } else {
            MqttPublishMessage temp = (MqttPublishMessage) this.oriMessage;
            MqttPublishMessage mqttMessage = new MqttPublishMessage(newFuxHeader, temp.variableHeader(), temp.payload());
            rePublish = this.schedule(eventLoop, channel, timeout, mqttMessage, messageId);
        }


    }

    /**
     * 取消消息发送，如果没有发送完成
     */
    public abstract void cancle();

    /**
     * 定时取消发送
     * @param publishTimeout 取消发送的时间
     * @return Future
     */
    public abstract Future scheduleCancle(int publishTimeout);

    public MqttMessage getOriMessage() {
        return oriMessage;
    }

    public Promise getFuture() {
        return result;
    }

    public int getMessageId() {
        return  messageId;
    }

    public void success() {
        logger.debug("public success packedId:" + messageId);
        if (!result.isDone()) {
            result.setSuccess(null);
        }
    }

    public String getTopic() {
        return topic;
    }
}
