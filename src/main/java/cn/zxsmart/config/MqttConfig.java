package cn.zxsmart.config;

/**
 * @author zhengkun
 * @date 2021/6/10
 */
public class MqttConfig {

    private String mqttVersion = "MQTT_3_1_1";

    /**客户端id*/
    private String clientId;

    /**清除会话，在断开连接后是否存储订阅状态:
     * 存储订阅的消息Qos1和Qos2消息，当客户端重新订阅时发送
     * 服务端正在发送消息给客户端期间连接丢失导致发送失败的消息*/
    private boolean clearSession = false;

    private int keepalive = 60;

    /**是否开启遗嘱消息*/
    private boolean willFlag = false;

    /**遗嘱消息的qos*/
    private int willQos = 1;

    /**遗嘱消息推送的队列*/
    private String willTopic;

    /**遗嘱消息*/
    private String willMessage;

    /**持久化遗嘱消息*/
    private boolean willRetain = false;

    /**mqtt服务器ip*/
    private String host;

    /**mqtt服务器端口*/
    private Integer port;

    /**mqtt用户名*/
    private String username;

    /**mqtt密码*/
    private String password;

    /**是否重连*/
    private boolean reConnect = true;

    /**连接超时时间*/
    private int connecttimeout = 5;

    /**推送超时时间*/
    private int publishTimeout = 120;

    /**订阅超时时间*/
    private int subscribeTimeout = 120;

    /**推送后没有收到响应，重新发送消息的时间*/
    private int rePublishFrequecy = 30;

    /**处理线程数，默认是cpu内核数*/
    private int handlerThreads = Runtime.getRuntime().availableProcessors();


    public String getMqttVersion() {
        return mqttVersion;
    }

    public void setMqttVersion(String mqttVersion) {
        this.mqttVersion = mqttVersion;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Boolean getClearSession() {
        return clearSession;
    }

    public void setClearSession(Boolean clearSession) {
        this.clearSession = clearSession;
    }

    public int getKeepalive() {
        return keepalive;
    }

    public void setKeepalive(int keepalive) {
        this.keepalive = keepalive;
    }

    public boolean isWillFlag() {
        return willFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.willFlag = willFlag;
    }

    public int getWillQos() {
        return willQos;
    }

    public void setWillQos(int willQos) {
        this.willQos = willQos;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    public boolean isWillRetain() {
        return willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isReConnect() {
        return reConnect;
    }

    public void setReConnect(boolean reConnect) {
        this.reConnect = reConnect;
    }

    public int getConnecttimeout() {
        return connecttimeout;
    }

    public void setConnecttimeout(int connecttimeout) {
        this.connecttimeout = connecttimeout;
    }

    public int getPublishTimeout() {
        return publishTimeout;
    }

    public void setPublishTimeout(int publishTimeout) {
        this.publishTimeout = publishTimeout;
    }

    public int getSubscribeTimeout() {
        return subscribeTimeout;
    }

    public void setSubscribeTimeout(int subscribeTimeout) {
        this.subscribeTimeout = subscribeTimeout;
    }

    public int getRePublishFrequecy() {
        return rePublishFrequecy;
    }

    public void setRePublishFrequecy(int rePublishFrequecy) {
        this.rePublishFrequecy = rePublishFrequecy;
    }

    public int getHandlerThreads() {
        return handlerThreads;
    }

    public void setHandlerThreads(int handlerThreads) {
        this.handlerThreads = handlerThreads;
    }
}
