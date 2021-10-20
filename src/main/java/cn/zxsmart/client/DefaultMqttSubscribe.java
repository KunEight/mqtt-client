package cn.zxsmart.client;

import cn.zxsmart.MessageIdUtill;
import cn.zxsmart.MqttHandler;
import cn.zxsmart.MqttSubscribe;
import cn.zxsmart.config.MqttConfig;
import cn.zxsmart.exception.MqttChannelCloseException;
import cn.zxsmart.exception.MqttException;
import cn.zxsmart.qos.*;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * @author zhengkun
 * @date 2021/7/8
 */
public class DefaultMqttSubscribe implements MqttSubscribe {

    /**已经订阅完成的topic*/
    private Map<String, Set<MqttHandler>> topics = new ConcurrentHashMap<>();

    private Map<String, Set<MqttHandler>> realTopic = new ConcurrentHashMap<>();

    /**等待中的topic*/
    private Map<String, Set<MqttHandler>> pendingTopic = new ConcurrentHashMap<>();

    private Map<String, Promise<Void>> subscribeFuture = new ConcurrentHashMap<>();

    private Map<String, AbstractMqttMessage> pendingMap = new ConcurrentHashMap();

    private Map<Integer, AbstractMqttMessage> pendingUnSubscribe = new ConcurrentHashMap<>();

    private Channel channel;

    private EventLoopGroup eventLoopGroup;

    private MqttConfig mqttConfig;

    public DefaultMqttSubscribe(Channel channel, EventLoopGroup eventLoopGroup, MqttConfig mqttConfig) {
        this.channel = channel;
        this.eventLoopGroup = eventLoopGroup;
        this.mqttConfig = mqttConfig;
    }

    @Override
    public Future subscribe(String topic, Set<MqttHandler> handler, MqttQoS qoS, boolean once) {
        if (topic == null || "".equals(topic)) {
            throw new MqttException("subcribe topic is null");
        }

        if(handler == null || handler.size() == 0) {
            throw new MqttException("handler is null");
        }

        //如果已经订阅了，就将handler加入
        if (topics.keySet().contains(topic)) {
            Set<MqttHandler> mqttHandlers = topics.get(topic);
            mqttHandlers.addAll(handler);
            return channel.newSucceededFuture();
        }

        //如果在等待队列中，也直接将handler加入
        if (pendingTopic.keySet().contains(topic)) {
            Set<MqttHandler> mqttHandlers = topics.get(topic);
            mqttHandlers.addAll(handler);
            return pendingMap.get(topic).getFuture();
        }
        //没有就构建消息，然后发送
        int messageId = MessageIdUtill.getNewMessageId();
        MqttSubscribeMessage message = MqttMessageBuilders.subscribe().addSubscription(qoS, topic).messageId(messageId).build();

        AbstractMqttMessage abstractMqttMessage = MessageFactory.create(channel, message, messageId,
                eventLoopGroup.next(), mqttConfig, qoS, topic);
        Set<MqttHandler> handlers = new CopyOnWriteArraySet<>();
        handlers.addAll(handler);
        pendingTopic.put(topic, handlers);
        pendingMap.put(topic, abstractMqttMessage);

        DefaultPromise<Void> promise = new DefaultPromise<>(eventLoopGroup.next());
        subscribeFuture.put(topic, promise);
        abstractMqttMessage.publish();
        abstractMqttMessage.getFuture().addListener(f -> {
            //发送失败后要设置订阅失败
            if (!f.isSuccess() && !promise.isDone()) {
                promise.setFailure(new TimeoutException("subscribe timeout,topic: " + topic));
            }
        });
        //长时间没有收到订阅成功的消息也失败;
        Future future = scheduleFaild(eventLoopGroup.next(), promise, topic);
        handlerResult(promise, topic, future, abstractMqttMessage);

        return promise;
    }

    @Override
    public Future unSubscribe(String topic) {
        if (pendingUnSubscribe.get(topic) != null) {
            return pendingUnSubscribe.get(topic).getFuture();
        }
        Future future = sendUnSubscribe(topic);
        return future;
    }

    @Override
    public void unSubSuccess(Integer messageId) {
        AbstractMqttMessage abstractMqttMessage = pendingUnSubscribe.get(messageId);
        if (abstractMqttMessage != null) {
            abstractMqttMessage.getFuture().setSuccess(null);
        }
        MqttUnsubscribeMessage payload = (MqttUnsubscribeMessage) abstractMqttMessage.getOriMessage().payload();
        List<String> unTopics = payload.payload().topics();
        unTopics.forEach(topic -> {
            if (topics.get(topic) != null) {
                Set<MqttHandler> mqttHandler = topics.get(topic);
                topics.remove(topic);
                mqttHandler.forEach(handler -> {
                    realTopic.entrySet().forEach(entry -> {
                        Set<MqttHandler> mqttHandlers = entry.getValue();
                        if (mqttHandlers.contains(handler)) {
                            mqttHandlers.remove(handler);
                        }
                        if (mqttHandlers.size() == 0) {
                            realTopic.remove(entry.getKey());
                        }
                    });
                });

            }
        });

    }

    private Future sendUnSubscribe(String topic) {
        int messageId = MessageIdUtill.getNewMessageId();
        MqttUnsubscribeMessage unsubscribeMessage = MqttMessageBuilders.unsubscribe().addTopicFilter(topic).messageId(messageId).build();
        AbstractMqttMessage abstractMqttMessage = MessageFactory.create(channel, unsubscribeMessage, messageId,
                eventLoopGroup.next(), mqttConfig, MqttQoS.AT_LEAST_ONCE, topic);
        pendingUnSubscribe.put(messageId, abstractMqttMessage);
        Future publish = abstractMqttMessage.publish();
        publish.addListener(f -> {
            if (f.isDone()) {
                pendingUnSubscribe.remove(messageId, abstractMqttMessage);
            }
        });
        return publish;
    }

    @Override
    public Map<String, Set<MqttHandler>> getSubscribedTopic() {
        return topics;
    }

    @Override
    public Map<String, AbstractMqttMessage> getSubscribeingTopic() {
        return pendingMap;
    }

    @Override
    public boolean successe(String topic) {
        subscribeFuture.get(topic).setSuccess(null);
        return true;
    }

    @Override
    public Set<MqttHandler> getHandler(String topic) {
        Set<MqttHandler> mqttHandlers = realTopic.get(topic);
        if (mqttHandlers == null || mqttHandlers.size() == 0) {
            CopyOnWriteArraySet<MqttHandler> realHandlers = new CopyOnWriteArraySet<>();
            topics.keySet().forEach(key -> {
                Pattern pattern = Pattern.compile(key.replace("+", "[^/]+")
                        .replace("#", ".+") + "$");
                if (pattern.matcher(topic).matches()) {
                    realHandlers.addAll(topics.get(key));
                }
            });
            realTopic.put(topic, realHandlers);
        }
        return realTopic.get(topic);
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    private Future scheduleFaild(EventLoop eventLoop, Promise future, String topic) {
        return eventLoop.schedule(() -> {
            future.setFailure(new TimeoutException("subscribe timeout,topic: " + topic));
        }, mqttConfig.getSubscribeTimeout(), TimeUnit.SECONDS);
    }

    private Future handlerResult(Future future, String topic, Future scheduleFuture, AbstractMqttMessage message) {
        future.addListener(f -> {
            if (f.isSuccess()) {
                Pattern pattern = Pattern.compile(topic.replace("+", "[^/]+")
                        .replace("#", ".+") + "$");
                topics.put(topic, pendingTopic.get(topic));
                realTopic.entrySet().forEach(entry -> {
                    if (pattern.matcher(entry.getKey()).matches()) {
                        entry.getValue().addAll(pendingTopic.get(topic));
                    }
                });
//                message.success();
            }
            if (f.isDone()) {
                pendingMap.remove(topic);
                pendingTopic.remove(topic);
                subscribeFuture.remove(topic);
                scheduleFuture.cancel(true);
                if (!message.getFuture().isDone()) {
                    message.cancle();;
                }
            }
        });
        return future;
    }

    @Override
    public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public void clear() {
//        topics.clear();
        realTopic.clear();
        subscribeFuture.values().forEach(promise -> {
            promise.setFailure(new MqttChannelCloseException());
        });
    }
}
