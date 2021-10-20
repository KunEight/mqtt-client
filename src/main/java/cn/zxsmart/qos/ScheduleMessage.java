package cn.zxsmart.qos;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.util.concurrent.ScheduledFuture;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author zhengkun
 * @date 2021/6/16
 */
public interface ScheduleMessage {

    Logger logger = Logger.getLogger(ScheduleMessage.class);

    /**
     * 定时发送消息
     * @param eventLoop eventLoop
     * @param channel channel
     * @param frequence frequence
     * @param message message
     * @return ScheduledFuture
     */
    default ScheduledFuture schedule(EventLoop eventLoop,
                                    Channel channel,
                                    int frequence,
                                    MqttMessage message,
                                     Integer messageId) {
        return eventLoop.scheduleAtFixedRate(() -> {
            if (channel.isActive()) {
                logger.debug("MQTT: schedule resend type = " + message.fixedHeader().messageType().name() + ",packedId:" + messageId);
                if (channel != null && channel.isActive()) {
                    if (message.payload() instanceof ByteBuf) {
                        ((ByteBuf) message.payload()).retain();
                    }
                    ChannelFuture future = channel.writeAndFlush(message);
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, frequence, frequence, TimeUnit.SECONDS);
    }
}
