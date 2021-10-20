package cn.zxsmart.message.handler;

import cn.zxsmart.MqttHandler;
import cn.zxsmart.MqttPublish;
import cn.zxsmart.MqttSubscribe;
import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.qos.AbstractMqttMessage;
import cn.zxsmart.qos.Qos1Message;
import cn.zxsmart.qos.Qos2Message;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.Future;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhengkun
 * @date 2021/7/9
 */
public class MqttPublishHandler implements MessageHandler<MqttPublishMessage> {

    private MqttSubscribe mqttSubscribe;

    private MqttPublish mqttPublish;

    private MqttConfig mqttConfig;

    private EventLoopGroup group;

    public MqttPublishHandler(MqttSubscribe mqttSubscribe, MqttPublish mqttPublish, MqttConfig mqttConfig, EventLoopGroup group) {
        this.mqttSubscribe = mqttSubscribe;
        this.mqttPublish = mqttPublish;
        this.mqttConfig = mqttConfig;
        this.group = group;
    }

    @Override
    public void handler(MqttPublishMessage message, Channel channel) {
        MqttQoS mqttQoS = message.fixedHeader().qosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                handlerPublish(message);
                break;
            case AT_LEAST_ONCE:
                handlerPublish(message);
                createPublishAck(message, channel);
                break;
            case EXACTLY_ONCE:
                createPubrec(message, channel);
                break;
        }


    }

    private Future createPubrec(MqttPublishMessage message, Channel channel) {
        String topicName = message.variableHeader().topicName();
        int messageId = message.variableHeader().packetId();
        Qos2Message qos2Message = new Qos2Message(message, messageId, channel,
                group.next(), mqttConfig, topicName,true);
        Map<Integer, AbstractMqttMessage> pendingPublish = mqttPublish.getPendingPublish();
        //定时取消任务
        Future future = qos2Message.scheduleCancle(mqttConfig.getPublishTimeout());
        //定时取消任务只有在任务完成会失败后才会取消
        future.addListener(f -> {
            pendingPublish.remove(messageId);
        });
        pendingPublish.put(messageId, qos2Message);
        return qos2Message.publicRec();
    }

    private Future createPublishAck(MqttPublishMessage message, Channel channel) {
        return Qos1Message.pubAck(message, channel);
    }

    private void handlerPublish(MqttPublishMessage message) {
        String topicName = message.variableHeader().topicName();
        Set<MqttHandler> handlers = mqttSubscribe.getHandler(topicName);
        if (handlers != null && handlers.size() > 0) {
            handlers.forEach(handler -> handler.handler(topicName, message.payload()));
        }
    }
}
