package cn.zxsmart.message.handler;

import cn.zxsmart.MqttSubscribe;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;

/**
 * @author zhengkun
 * @date 2021/7/20
 */
public class MqttUnSubAckHandler implements MessageHandler<MqttUnsubAckMessage> {

    private MqttSubscribe mqttSubscribe;

    public MqttUnSubAckHandler(MqttSubscribe mqttSubscribe) {
        this.mqttSubscribe = mqttSubscribe;
    }

    @Override
    public void handler(MqttUnsubAckMessage message, Channel channel) {
        int messageId = message.variableHeader().messageId();
        mqttSubscribe.unSubSuccess(messageId);
    }
}
