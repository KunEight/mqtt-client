package cn.zxsmart.message.handler;

import cn.zxsmart.MqttPublish;
import cn.zxsmart.qos.AbstractMqttMessage;
import cn.zxsmart.qos.Qos2Message;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;

/**
 * @author zhengkun
 * @date 2021/7/20
 */
public class MqttPubCompHandler implements MessageHandler<MqttMessage> {

    private MqttPublish mqttPublish;

    public MqttPubCompHandler(MqttPublish mqttPublish) {
        this.mqttPublish = mqttPublish;
    }

    @Override
    public void handler(MqttMessage message, Channel channel) {
        int messageId = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
        Qos2Message qos2Message = (Qos2Message)mqttPublish.getPendingPublish().get(messageId);
        if (qos2Message != null) {
            qos2Message.success();
        }
    }
}
