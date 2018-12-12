package com.example.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.net.URI;

/**
 * Hello world!
 *
 */
public class NettyHttpFileClient
{
    public void connect(String host, int port, String url, final String local) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChildChannelHandler(local));

            ChannelFuture f = b.connect(host, port).sync();

            URI uri = new URI(url);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
            request.headers().set(HttpHeaders.Names.HOST, host);
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());
            f.channel().write(request);
            f.channel().flush();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        String local;
        public ChildChannelHandler(String local) {
            this.local = local;
        }

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new HttpResponseDecoder());
            socketChannel.pipeline().addLast(new HttpRequestEncoder());
            socketChannel.pipeline().addLast(new ChunkedWriteHandler());
            socketChannel.pipeline().addLast(new HttpDownloadHandler(local));
        }
    }

    public static void main( String[] args ) throws Exception
    {
        NettyHttpFileClient client = new NettyHttpFileClient();
        client.connect("localhost", 9003, "/1.pdf", "fp1.pdf");
//        client.connect("www.ghost64.com", 80, "http://www.ghost64.com/qqtupian/zixunImg/local/2017/05/27/1495855297602.jpg", "1495855297602.jpg");
    }
}
