package cn.zxsmart.qos;

import cn.zxsmart.config.MqttConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;

/**
 * @author zhengkun
 * @date 2021/6/15
 */
public class Qos0Message extends AbstractMqttMessage{


    public Qos0Message(MqttMessage oriMessage,
                       Integer messageId,
                       Channel channel,
                       EventLoop eventLoop,
                       MqttConfig config,
                       String topic) {
        super(oriMessage, messageId, channel, eventLoop, config, topic);
    }

    @Override
    public Future publish() {
        Future publish = super.publish();
        if (oriMessage.payload() instanceof ByteBuf) {
            ((ByteBuf)oriMessage.payload()).release();
        }
        result = new DefaultPromise<>(eventLoop);
        success();
        return result;
    }

    @Override
    public void cancle() {
    }

    @Override
    public Future scheduleCancle(int publishTimeout) {
        return null;
    }
}
