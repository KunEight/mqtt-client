package cn.zxsmart.message.handler;

import cn.zxsmart.MqttHandler;
import cn.zxsmart.MqttPublish;
import cn.zxsmart.MqttSubscribe;
import cn.zxsmart.qos.Qos2Message;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;

import java.util.Set;

/**
 * @author zhengkun
 * @date 2021/7/20
 */
public class MqttPubRelHandler implements MessageHandler<MqttMessage> {

    private MqttPublish mqttPublish;

    private MqttSubscribe mqttSubscribe;

    public MqttPubRelHandler(MqttPublish mqttPublish) {
        this.mqttPublish = mqttPublish;
    }

    @Override
    public void handler(MqttMessage message, Channel channel) {
        int messageId = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
        Qos2Message qos2Message = (Qos2Message)mqttPublish.getPendingPublish().get(messageId);
        if (qos2Message != null) {
            Set<MqttHandler> handlers = mqttSubscribe.getHandler(qos2Message.getTopic());
            handlers.forEach(handler -> handler.handler(qos2Message.getTopic(), qos2Message.getOriMessage().payload()));
            qos2Message.success();
            mqttPublish.getPendingPublish().remove(messageId);
        }
        Qos2Message.publishComp(channel, messageId);
    }
}
