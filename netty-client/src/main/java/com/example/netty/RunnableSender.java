package com.example.netty;

public class RunnableSender implements Runnable {
    private String host;
    private String message;
    private int port;

    RunnableSender(String host, int port, String message) {
        this.host = host;
        this.port = port;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            new EchoClient(host, port).start(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
