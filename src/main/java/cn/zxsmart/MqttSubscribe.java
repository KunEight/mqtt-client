package cn.zxsmart;

import cn.zxsmart.qos.AbstractMqttMessage;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhengkun
 * @date 2021/7/8
 */
public interface MqttSubscribe {

    /**
     * 订阅消息
     * @param topic topic
     * @param handlers handlers
     * @param  qoS qoS
     * @param once once
     * @return Future
     */
    Future subscribe(String topic, Set<MqttHandler> handlers, MqttQoS qoS, boolean once);

    /**
     * 取消订阅
     * @param topic topic
     * @return Future
     */
    Future unSubscribe(String topic);

    /**
     * 取消订阅成功
     * @param messageId messageId
     */
    void unSubSuccess(Integer messageId);

    /**
     * 获取已经订阅完成的topic
     * @return Map
     */
    Map<String, Set<MqttHandler>> getSubscribedTopic();

    /**
     * 获取已经订阅未完成的topic
     * @return Map Map
     */
    Map<String, AbstractMqttMessage> getSubscribeingTopic();

    /**
     * 订阅成功回调
     * @param  topic topic
     * @return boolean
     */
    boolean successe(String topic);

    /**
     * 获取订阅设置的处理器
     * @param  topic topic
     * @return MqttHandler
     */
    Set<MqttHandler> getHandler(String topic);

    /**
     * 设置channel
     * @param channel channel
     */
    void setChannel(Channel channel);

    /**
     * 设置线程池
     * @param eventLoopGroup eventLoopGroup
     */
    void setEventLoopGroup(EventLoopGroup eventLoopGroup);

    /**
     * 清除订阅
     */
    void clear();


}
