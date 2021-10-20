package cn.zxsmart.handler;

import cn.zxsmart.MqttConnectResult;
import cn.zxsmart.MqttHandler;
import cn.zxsmart.MqttPublish;
import cn.zxsmart.MqttSubscribe;
import cn.zxsmart.client.DefaultMqttPublish;
import cn.zxsmart.client.DefaultMqttSubscribe;
import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.exception.MqttException;
import cn.zxsmart.exception.MqttTimeoutException;
import cn.zxsmart.message.handler.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
@ChannelHandler.Sharable
public class MqttChannelHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private static final Logger log = LoggerFactory.getLogger(MqttChannelHandler.class);

    private Map<MqttMessageType, MessageHandler> handlerMap = new HashMap();

    private Promise<MqttConnectResult> connectResult;

    private MqttConfig mqttConfig;

    private MqttPublish mqttPublish;

    private MqttSubscribe mqttSubscribe;

    private EventLoopGroup eventLoopGroup;

    public MqttChannelHandler(Promise<MqttConnectResult> connectResult,
                              Channel channel,
                              EventLoopGroup eventLoopGroup,
                              MqttConfig config,
                              MqttPublish mqttPublish,
                              MqttSubscribe mqttSubscribe) {
        super();
        this.connectResult = connectResult;
        this.mqttConfig = config;
        this.mqttPublish = mqttPublish;
        this.mqttSubscribe = mqttSubscribe;
        this.eventLoopGroup = eventLoopGroup;
        handlerMap.put(MqttMessageType.CONNACK, new MqttConnackHandler(connectResult));
        handlerMap.put(MqttMessageType.SUBACK, new MqttSubAckHandler(mqttSubscribe));
        handlerMap.put(MqttMessageType.PUBLISH, new MqttPublishHandler(mqttSubscribe, mqttPublish, config, eventLoopGroup));
        handlerMap.put(MqttMessageType.UNSUBSCRIBE, new MqttUnSubAckHandler(mqttSubscribe));
        handlerMap.put(MqttMessageType.PUBACK, new MqttPublishAckHandler(mqttPublish));
        handlerMap.put(MqttMessageType.PUBREC, new MqttPubRecHandler(mqttPublish));
        handlerMap.put(MqttMessageType.PUBREL, new MqttPubRelHandler(mqttPublish));
        handlerMap.put(MqttMessageType.PUBCOMP, new MqttPubCompHandler(mqttPublish));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
       //由于异步处理，导致出这个方法就会被释放，所以在此引用一次
        if (msg.payload() instanceof ByteBuf) {
            ((ByteBuf) msg.payload()).retain();
        }
        eventLoopGroup.execute(() -> {
            //根据不同类型的消息处理
            try {
                MqttMessageType messageType = msg.fixedHeader().messageType();
                if (handlerMap.get(messageType) != null) {
                    handlerMap.get(messageType).handler(msg, ctx.channel());
                }
            }finally {
                //上面引用了一次，这里要释放
                if (msg.payload() instanceof ByteBuf) {
                    ((ByteBuf) msg.payload()).release();
                }
            }

        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelFuture future = sendConnectionMessage(ctx);
        future.get(mqttConfig.getPublishTimeout(), TimeUnit.SECONDS);
        if (!future.isSuccess()) {
            connectResult.setFailure(new MqttTimeoutException("Mqtt connect timeout", 0));
            ctx.channel().close();
        } else {
            //重新订阅
            log.info("Mqtt: send connect success");
            synchronized (mqttSubscribe) {
                Map<String, Set<MqttHandler>> subscribedTopic = mqttSubscribe.getSubscribedTopic();
                HashMap<String, Set<MqttHandler>> cloneTopic = new HashMap<>();
                cloneTopic.putAll(subscribedTopic);
                subscribedTopic.clear();
                cloneTopic.keySet().forEach(topic -> {
                    mqttSubscribe.subscribe(topic, cloneTopic.get(topic), MqttQoS.AT_LEAST_ONCE, false);
                });
            }

        }
        super.channelActive(ctx);
    }

    private ChannelFuture sendConnectionMessage(ChannelHandlerContext ctx) {
        MqttMessageBuilders.ConnectBuilder connect = MqttMessageBuilders.connect();
        connect.protocolVersion(MqttVersion.valueOf(mqttConfig.getMqttVersion())).clientId(mqttConfig.getClientId());
        if (mqttConfig.getClientId() == null) {
            Random random = new Random();
            connect.clientId("netty-mqtt" + random.nextInt(10));
        }
        connect.cleanSession(mqttConfig.getClearSession()).keepAlive(mqttConfig.getKeepalive());
        connect.willFlag(mqttConfig.isWillFlag());
        if (mqttConfig.isWillFlag()) {
            connect.willQoS(MqttQoS.valueOf(mqttConfig.getWillQos()));
            if (mqttConfig.getWillTopic() == null) {
                throw new MqttException("connect is will, but willtopic is null");
            }
            connect.willTopic(mqttConfig.getWillTopic());
            connect.willMessage(mqttConfig.getWillMessage().getBytes());
            connect.willRetain(mqttConfig.isWillRetain());
        }
        if (mqttConfig.getUsername() == null) {
            connect.hasUser(false);
        } else {
            connect.username(mqttConfig.getUsername());
            connect.password(mqttConfig.getPassword().getBytes());
        }

        MqttConnectMessage connectMessage = connect.build();
        return ctx.writeAndFlush(connectMessage);
    }
}
