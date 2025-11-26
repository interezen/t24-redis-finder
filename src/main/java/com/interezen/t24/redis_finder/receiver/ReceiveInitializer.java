package com.interezen.t24.redis_finder.receiver;

import com.interezen.t24.redis_finder.cfg.StaticProperties;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Created by root on 2015-08-05.
 */
public class ReceiveInitializer extends ChannelInitializer<SocketChannel> {

    private int packetMaxByte = StaticProperties.getInstance().getInt("receiver.max.packet.byte", 20480);
    private int socketReadIdleTimeoutMs = StaticProperties.getInstance().getInt("receiver.read.idle.timeout", 30000);
    private String packetCharset = StaticProperties.getInstance().getString("receiver.packet.charset", "");

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        // 테스트용. 실제 API는 \n을 통해 socket close를 하지 않기 때문에 테스트 확인 후 아래 라인을 주석처리한다.
//        ch.pipeline().addLast("framer", new DelimiterBasedFrameDecoder(packetMaxByte, Delimiters.lineDelimiter()));

        if(StringUtils.isNotEmpty(packetCharset)) {
            ch.pipeline().addLast("decoder", new StringDecoder(Charset.forName(packetCharset)));
        } else {
            ch.pipeline().addLast("decoder", new StringDecoder());
        }
        ch.pipeline().addLast("encoder", new StringEncoder());
        ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(socketReadIdleTimeoutMs, 0, 0, TimeUnit.MILLISECONDS));
        ch.pipeline().addLast("handler",          new ReceiveServerHandler());
    }
}
