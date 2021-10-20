package cn.zxsmart;

import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhengkun
 * @date 2021/7/8
 */
public class MessageIdUtill {
    private static final AtomicInteger NEXT_MESSAGE_ID = new AtomicInteger(1);

    public static int getNewMessageId() {
        NEXT_MESSAGE_ID.compareAndSet(0xffff, 1);
        return NEXT_MESSAGE_ID.getAndIncrement();
//        return MqttMessageIdVariableHeader.from(nextMessageId.getAndIncrement());
    }
}
