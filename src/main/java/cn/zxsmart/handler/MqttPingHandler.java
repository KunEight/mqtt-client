package cn.zxsmart.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public class MqttPingHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private static final Logger log = LoggerFactory.getLogger(MqttPingHandler.class);

    private volatile AtomicReference<ScheduledFuture<?>> timeoutFuture = new AtomicReference<ScheduledFuture<?>>();

    private int keepAliveTime;

    public MqttPingHandler(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        if (msg.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            log.debug("Mqtt: receive idle message");
            if (timeoutFuture.get() != null) {
                synchronized (this) {
                    ScheduledFuture<?> scheduledFuture = timeoutFuture.get();
                    scheduledFuture.cancel(true);
                    timeoutFuture.set(null);
                }
            }
        }
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idl = (IdleStateEvent) evt;
            if (idl.state() == IdleState.WRITER_IDLE) {
                MqttFixedHeader fixedHeader = new MqttFixedHeader(
                        MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
                ctx.writeAndFlush(new MqttMessage(fixedHeader));
                log.debug("Mqtt: send idle message");
                if (timeoutFuture.get() == null) {
                    synchronized (this) {
                        ScheduledFuture<?> schedule = ctx.channel().eventLoop().schedule(() -> {
                            MqttFixedHeader fixedHeader2 = new MqttFixedHeader(MqttMessageType.DISCONNECT, false,
                                    MqttQoS.AT_MOST_ONCE, false, 0);
                            ctx.writeAndFlush(new MqttMessage(fixedHeader2)).addListener(ChannelFutureListener.CLOSE);
                            log.info("MQTT: disconnect for connect timeout");
                        }, 2 * keepAliveTime, TimeUnit.SECONDS);
                        if (!timeoutFuture.compareAndSet(null, schedule)) {
                            schedule.cancel(true);
                        }
                    }
                }
            }
        }
    }

}
