package cn.zxsmart.client;

import cn.zxsmart.MqttHandler;
import cn.zxsmart.MqttMessageAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author zhengkun
 * @date 2021/7/21
 */
public class ListennerMqttHandler<T> implements MqttHandler<T> {

    private String name;

    private Object bean;

    private Method method;

    private MqttMessageAdapter mqttMessageAdapter;

    public ListennerMqttHandler(String name, Object bean, Method method, MqttMessageAdapter mqttMessageAdapter) {
        this.name = name;
        this.bean = bean;
        this.method = method;
        this.mqttMessageAdapter = mqttMessageAdapter;
    }

    @Override
    public void handler(String topic, T payload) {
        // 暂时不支持参数自动化适配，参数必须是（String topic, ByteBuf message）
        try {
            method.invoke(bean, topic, payload);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getName() {
        return name;
    }
}
