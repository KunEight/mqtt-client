package cn.zxsmart;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.DefaultPromise;

import java.util.concurrent.Future;

/**
 * @author zhengkun
 * @date 2021/6/15
 */
public interface SendMessage {

    /**
     * 发送消息
     * @param channel channel
     * @param message message
     * @return ChannelFuture
     */
    default ChannelFuture sendMessage(Channel channel, Object message) {
        if (channel != null && channel.isActive()) {
            return channel.writeAndFlush(message);
        } else {
            DefaultChannelPromise future = new DefaultChannelPromise(channel);
            future.setFailure(new ChannelException("channle closed"));
            return future;
        }
    }
}
