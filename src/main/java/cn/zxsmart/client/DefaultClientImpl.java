package cn.zxsmart.client;

import cn.zxsmart.*;
import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.exception.MqttTimeoutException;
import cn.zxsmart.handler.MqttChannelHandler;
import cn.zxsmart.handler.MqttPingHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public class DefaultClientImpl implements MqttClient {
    private static final Logger log = LoggerFactory.getLogger(MqttPingHandler.class);

    private final MqttConfig mqttConfig;

    private volatile Channel channel;

    private boolean conncet = true;

    private NioEventLoopGroup work;

    private MqttPublish mqttPublish;

    private MqttSubscribe mqttSubscribe;

    private ScheduledFuture scheduleReconnect;

    private DefaultThreadFactory defaultThreadFactory = new DefaultThreadFactory("mqtt-thread", true);

    private NioEventLoopGroup boss = new NioEventLoopGroup(1, defaultThreadFactory);

    public DefaultClientImpl(MqttConfig mqttConfig) {
        this.mqttConfig = mqttConfig;
        mqttPublish = new DefaultMqttPublish(channel, work, mqttConfig);
        mqttSubscribe = new DefaultMqttSubscribe(channel, work, mqttConfig);
//        Future<MqttConnectResult> connect = connect();
//        try {
//            MqttConnectResult mqttConnectResult = connect.get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public Future<MqttConnectResult> connect() {
        InetSocketAddress address = new InetSocketAddress(mqttConfig.getHost(), mqttConfig.getPort());
        Bootstrap bootstrap = new Bootstrap();
        if (work == null) {
            work = new NioEventLoopGroup(
                    mqttConfig.getHandlerThreads(),
                    defaultThreadFactory);
            mqttPublish.setEventLoopGroup(work);
            mqttSubscribe.setEventLoopGroup(work);
        }
        Promise<MqttConnectResult> connectResult = new DefaultPromise<>(work.next());
        bootstrap.group(boss)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MqttDecoder());
                        pipeline.addLast(MqttEncoder.INSTANCE);
                        pipeline.addLast(new IdleStateHandler(mqttConfig.getKeepalive() - 1, mqttConfig.getKeepalive(), 0));
                        pipeline.addLast(new MqttPingHandler(mqttConfig.getKeepalive()));
                        pipeline.addLast(new MqttChannelHandler(connectResult, channel, work, mqttConfig, mqttPublish, mqttSubscribe));
                    }
                });
        ChannelFuture connect = bootstrap.connect(address);
        connect.addListener((ChannelFutureListener)f -> {
            if (f.isSuccess()) {
                log.info("MQTT: connect success, " + address.getHostName() + ":" + address.getPort());
                if (scheduleReconnect != null) {
                    scheduleReconnect.cancel(true);
                    scheduleReconnect = null;
                }
                this.channel = f.channel();
                mqttPublish.setChannel(channel);
                mqttSubscribe.setChannel(channel);
                channel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                   log.info("MQTT: disconnect, " + address.getHostName() + ":" + address.getPort());
                   mqttSubscribe.clear();
                   if (conncet && mqttConfig.isReConnect()) {
                        scheduleConnect();
                   }
                });
            } else {
                connectResult.setFailure(new MqttTimeoutException("connect timeout", 1));
                log.info("MQTT: connect falid, " + address.getHostName() + ":" + address.getPort());
            }
        });
        return connectResult;
    }

    private void scheduleConnect() {
        if (this.conncet && mqttConfig.isReConnect()) {
            scheduleReconnect = work.next().schedule(() -> connect(), mqttConfig.getConnecttimeout(), TimeUnit.SECONDS);
        }
    }

    @Override
    public Future<Void> publish(String topic, ByteBuf message) {

        return publish(topic, message, MqttQoS.AT_LEAST_ONCE);
    }

    @Override
    public Future<Void> publish(String topic, ByteBuf message, MqttQoS qos) {
        return publish(topic, message, qos, false);
    }

    @Override
    public Future<Void> publish(String topic, ByteBuf message, MqttQoS qoS, boolean retain) {
        Future<Void> publish = mqttPublish.publish(topic, message, qoS, retain);
        return publish;
    }

    @Override
    public Future<Void> subscribe(String topic, MqttHandler handler) {
        return subscribe(topic, handler, MqttQoS.AT_LEAST_ONCE);
    }

    @Override
    public Future<Void> subscribe(String topic, MqttHandler handler, MqttQoS qoS) {

        return  subscribe(topic, handler, MqttQoS.AT_LEAST_ONCE, false);
    }

    @Override
    public Future<Void> subscribe(String topic, MqttHandler handler, MqttQoS qoS, boolean once) {
        HashSet<MqttHandler> mqttHandlers = new HashSet<>();
        mqttHandlers.add(handler);
        return mqttSubscribe.subscribe(topic, mqttHandlers, qoS, once);
    }

    @Override
    public Future<Void> offSubscribe(String topic) {
        return mqttSubscribe.unSubscribe(topic);
    }

    @Override
    public boolean isConnect() {
        return conncet;
    }

    @Override
    public void disConnect() {

    }

    @Override
    public MqttSubscribe getMqttSubscribe() {
        return mqttSubscribe;
    }

    @Override
    public Channel getChannel() {
        return this.channel;
    }
}
