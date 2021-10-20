package cn.zxsmart;

/**
 * @author zhengkun
 * @date 2021/7/21
 */
public interface MqttMessageAdapter<T, E> {

    /**
     * 消息适配器
     * @param t 消息
     * @return 转换后的消息
     */
    E covert(T t);

}
