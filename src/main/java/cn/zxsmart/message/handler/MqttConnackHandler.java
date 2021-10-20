package cn.zxsmart.message.handler;

import cn.zxsmart.MqttConnectResult;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.util.concurrent.Promise;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public class MqttConnackHandler implements MessageHandler<MqttConnAckMessage>{

    private Promise<MqttConnectResult> resultPromise;

    public MqttConnackHandler(Promise<MqttConnectResult> resultPromise) {
        this.resultPromise = resultPromise;
    }

    @Override
    public void handler(MqttConnAckMessage message, Channel channel) {
        MqttConnectReturnCode connectReturnCode = message.variableHeader().connectReturnCode();
        switch (connectReturnCode) {
            case CONNECTION_ACCEPTED:
                this.resultPromise.setSuccess(new MqttConnectResult(true, connectReturnCode, channel.closeFuture()));
                // TODO: 2021/7/2 根据状态重发未发送完成的消息
                break;
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                this.resultPromise.setSuccess(new MqttConnectResult(false, connectReturnCode, channel.closeFuture()));
                channel.close();
                break;
            default:
                //who is care
                break;
        }
    }
}
