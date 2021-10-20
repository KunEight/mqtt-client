package cn.zxsmart.message.handler;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;


/**
 * @author zhengkun
 * @date 2021/6/25
 */
public interface MessageHandler<T extends MqttMessage> {

    /**
     * 根据消息类型处理消息
     * @param message 消息
     * @param channel channel
     */
    void handler(T message, Channel channel);
}
