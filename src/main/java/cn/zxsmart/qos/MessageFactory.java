package cn.zxsmart.qos;

import cn.zxsmart.config.MqttConfig;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * @author zhengkun
 * @date 2021/7/9
 */
public class MessageFactory {

    public static AbstractMqttMessage create(Channel channel,
                                      MqttMessage message,
                                      Integer messageId,
                                      EventLoop eventLoop,
                                      MqttConfig mqttConfig,
                                             MqttQoS qoS,
                                             String topic) {
        AbstractMqttMessage abstractMqttMessage;
        if (qoS == MqttQoS.AT_MOST_ONCE) {
            abstractMqttMessage = new Qos0Message(message,messageId, channel, eventLoop, mqttConfig,topic);
        } else if(qoS == MqttQoS.AT_LEAST_ONCE) {
            abstractMqttMessage = new Qos1Message(message, messageId, channel, eventLoop, mqttConfig,topic);
        } else {
            abstractMqttMessage = new Qos2Message(message, messageId, channel, eventLoop, mqttConfig, topic,false);
        }
        return abstractMqttMessage;
    }
}
