package cn.zxsmart.message.handler;

import cn.zxsmart.MqttPublish;
import cn.zxsmart.qos.Qos1Message;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;

/**
 * @author zhengkun
 * @date 2021/7/20
 */
public class MqttPublishAckHandler implements MessageHandler<MqttPubAckMessage> {

    private MqttPublish mqttPublish;

    public MqttPublishAckHandler(MqttPublish mqttPublish) {
        this.mqttPublish = mqttPublish;
    }

    @Override
    public void handler(MqttPubAckMessage message, Channel channel) {
        Qos1Message qos1Message = (Qos1Message)mqttPublish.getPendingPublish().get(message.variableHeader().messageId());
        if (qos1Message != null) {
            qos1Message.success();
            mqttPublish.getPendingPublish().remove(message.variableHeader().messageId());
        }
    }
}
