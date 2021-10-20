package cn.zxsmart;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public class MqttConnectResult {

    private boolean status;

    private MqttConnectReturnCode code;

    private ChannelFuture closeFuture;

    public MqttConnectResult(boolean status, MqttConnectReturnCode code, ChannelFuture closeFuture) {
        this.status = status;
        this.code = code;
        this.closeFuture = closeFuture;
    }

    public boolean isStatus() {
        return status;
    }

    public MqttConnectReturnCode getCode() {
        return code;
    }

    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }
}
