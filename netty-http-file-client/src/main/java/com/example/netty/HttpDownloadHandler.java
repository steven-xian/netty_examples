package com.example.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.io.FileOutputStream;

public class HttpDownloadHandler extends ChannelInboundHandlerAdapter {
    private boolean readingChunks = false;
    private FileOutputStream fileOutputStream = null;
    private File file = null;
    private String local = null;
    private int succCode;

    public HttpDownloadHandler(String local) {
        this.local = local;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            succCode = response.getStatus().code();
            if (succCode == 200) {
                setDownloadFile();
                readingChunks = true;
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            if (chunk instanceof LastHttpContent) {
                readingChunks = false;
            }

            ByteBuf buffer = chunk.content();
            byte[] dst = new byte[buffer.readableBytes()];
            if (succCode == 200) {
                while( buffer.isReadable()) {
                    buffer.readBytes(dst);
                    fileOutputStream.write(dst);
                    buffer.release();
                }
                if (null != fileOutputStream) {
                    fileOutputStream.flush();
                }
            }
        }
        if (!readingChunks) {
            if (null != fileOutputStream) {
                System.out.println("Download done -> " + file.getAbsolutePath());
                fileOutputStream.flush();
                fileOutputStream.close();
                file = null;
                fileOutputStream = null;
            }
            ctx.channel().close();
        }
    }

    private void setDownloadFile() throws Exception {
        if (null == fileOutputStream) {
            local = SystemPropertyUtil.get("user.dir") + File.separator + local;
            file = new File(local);
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Channel exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.channel().close();
    }
}
