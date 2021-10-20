package cn.zxsmart;

import cn.zxsmart.qos.AbstractMqttMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;

import java.util.Map;

/**
 * @author zhengkun
 * @date 2021/7/8
 */
public interface MqttPublish {

    /**
     * 推送消息
     * @param topic topic
     * @param message message
     * @param qoS qoS
     * @param retain retain
     * @return Future
     */
    Future<Void> publish(String topic, ByteBuf message, MqttQoS qoS, boolean retain);

    /**
     * 获取未收到响应的消息
     * @return map
     */
    Map<Integer, AbstractMqttMessage> getPendingPublish();

    /**
     * 设置通道
     * @param  channel channel
     */
    void setChannel(Channel channel);

    /**
     * 设置线程池
     * @param eventLoopGroup eventLoopGroup
     */
    void setEventLoopGroup(EventLoopGroup eventLoopGroup);
}
