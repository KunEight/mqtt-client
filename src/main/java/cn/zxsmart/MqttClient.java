package cn.zxsmart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public interface MqttClient {

    /**
     * 连接服务端
     * @return 连接结果
     */
    Future<MqttConnectResult> connect();

    /**
     * 推送消息
     * @param topic topic
     * @param message message
     * @return Future
     */
    Future<Void> publish(String topic, ByteBuf message);

    /**
     * 推送消息
     * @param topic topic
     * @param message message
     * @param qos qos
     * @return Future
     */
    Future<Void> publish(String topic, ByteBuf message, MqttQoS qos);

    /**
     * 推送消息
     * @param topic topic
     * @param message message
     * @param qoS qos
     * @param retain retain
     * @return Future
     */
    Future<Void> publish(String topic, ByteBuf message, MqttQoS qoS, boolean retain);

    /**
     * 订阅消息
     * @param topic topic
     * @param handler handler
     * @return Future
     */
    Future<Void> subscribe(String topic, MqttHandler handler);

    /**
     * 订阅消息
     * @param topic topic
     * @param handler handler
     * @param qoS qoS
     * @return Future
     */
    Future<Void> subscribe(String topic, MqttHandler handler, MqttQoS qoS);

    /**
     * 订阅消息
     * @param topic topic
     * @param handler handler
     * @param qoS qoS
     * @param once once
     * @return Future
     */
    Future<Void> subscribe(String topic, MqttHandler handler, MqttQoS qoS, boolean once);

    /**
     * 取消订阅
     * @param topic topic
     * @return Future
     */
    Future<Void> offSubscribe(String topic);

    /**
     * 是否连接
     * @return boolean
     */
    boolean isConnect();

    /**
     * 断开连接
     */
    void disConnect();

    /**
     * 获取订阅信息
     * @return MqttSubscribe
     */
    MqttSubscribe getMqttSubscribe();

    /**
     * 获取通道
     * @return channel
     */
    Channel getChannel();
}
