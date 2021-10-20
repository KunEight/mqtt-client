package cn.zxsmart.client;

import cn.zxsmart.MessageIdUtill;
import cn.zxsmart.MqttPublish;
import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.qos.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.Future;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhengkun
 * @date 2021/7/9
 */
public class DefaultMqttPublish implements MqttPublish {

    private Channel channel;

    private EventLoopGroup eventLoopGroup;

    private MqttConfig mqttConfig;

    private Map<Integer, AbstractMqttMessage> pendingPublish = new ConcurrentHashMap<>();

    public DefaultMqttPublish(Channel channel, EventLoopGroup eventLoopGroup, MqttConfig mqttConfig) {
        this.channel = channel;
        this.eventLoopGroup = eventLoopGroup;
        this.mqttConfig = mqttConfig;
    }

    @Override
    public Future<Void> publish(String topic, ByteBuf message, MqttQoS qoS, boolean retain) {
        //创建mqtt消息
        final int messageId = MessageIdUtill.getNewMessageId();
        MqttPublishMessage publishMessage = MqttMessageBuilders.publish().topicName(topic)
                .messageId(messageId).qos(qoS)
                .retained(retain).payload(message).build();
        //创建缓存消息
        AbstractMqttMessage oriMessage = MessageFactory.create(channel, publishMessage, messageId,
                eventLoopGroup.next(), mqttConfig, qoS, topic);
        pendingPublish.put(messageId, oriMessage);
        //开始发送
        Future publish = oriMessage.publish();
        publish.addListener(f -> {
            if (publish.isDone()) {
                //发送完成后要移除缓存;
                pendingPublish.remove(messageId);
            }
        });
        return oriMessage.getFuture();
    }

    @Override
    public Map<Integer, AbstractMqttMessage> getPendingPublish() {
        return pendingPublish;
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
    @Override
    public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }
}
