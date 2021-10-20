package cn.zxsmart.message.handler;

import cn.zxsmart.MqttSubscribe;
import cn.zxsmart.qos.AbstractMqttMessage;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;

import java.util.Map;
import java.util.Optional;

/**
 * @author zhengkun
 * @date 2021/7/8
 */
public class MqttSubAckHandler implements MessageHandler<MqttSubAckMessage> {

    private MqttSubscribe mqttSubscribe;

    public MqttSubAckHandler(MqttSubscribe mqttSubscribe) {
        this.mqttSubscribe = mqttSubscribe;
    }

    @Override
    public void handler(MqttSubAckMessage message, Channel channel) {
        Map<String, AbstractMqttMessage> pendingTopic = mqttSubscribe.getSubscribeingTopic();
        int messageId = message.variableHeader().messageId();
        Optional<Map.Entry<String, AbstractMqttMessage>> optional =
                pendingTopic.entrySet().stream().filter(entry -> messageId == entry.getValue().getMessageId()).findFirst();
        if (optional.isPresent()) {
            mqttSubscribe.successe(optional.get().getKey());
        }

    }
}
