package cn.zxsmart.exception;

/**
 * @author zhengkun
 * @date 2021/6/16
 */
public class MqttTimeoutException extends RuntimeException {

    private int packetId;

    public MqttTimeoutException(String message, int packetId) {
        super(message);
        this.packetId = packetId;
    }

    public MqttTimeoutException(String message, Throwable cause, int packetId) {
        super(message, cause);
        this.packetId = packetId;
    }

    public MqttTimeoutException(Throwable cause, int packetId) {
        super(cause);
        this.packetId = packetId;
    }

    public MqttTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int packetId) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.packetId = packetId;
    }
}
