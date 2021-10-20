package cn.zxsmart;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public interface MqttHandler<T> {

    /**
     * 下游处理
     * @param topic topic
     * @param payload payload
     */
    void handler(String topic, T payload);

    /**
     * 获取处理器name
     * @return name
     */
    String getName();
}
