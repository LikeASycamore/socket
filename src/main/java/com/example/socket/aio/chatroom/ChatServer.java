package com.example.socket.aio.chatroom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @description:
 * @author: chenzhuo
 * @create: 2021-03-17 21:01
 */
public class ChatServer {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    private List<ClientHandler> connectedClients;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = inetSocketAddress.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端[" + clientPort + "]:";
    }

    private void start() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
        try {
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 将channel绑定到group中
            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverChannel.bind(new InetSocketAddress(LOCALHOST, port));
            System.out.println("启动服务器，监听端口[" + port + "]");

            while (true) {
                serverChannel.accept(null, new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }


    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }
            if (clientChannel != null && clientChannel.isOpen()) {
                // 通过构造方法，将channel与handler进行绑定
                ClientHandler handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
                // 第二个参数是attachment，可以传入到Hanler的回调函数
                clientChannel.read(buffer, buffer, handler);
                addClient(handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败" + exc);
        }
    }

    private class ClientHandler implements CompletionHandler<Integer, Object> {
        private AsynchronousSocketChannel clientChannel;

        public AsynchronousSocketChannel getClientChannel() {
            return clientChannel;
        }

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer != null) {
                if (result <= 0) {
                    // 客户端异常，移除客户端
                    removeClient(this);
                } else {
                    buffer.flip();
                    String fwdMsg = receive(buffer);
                    System.out.println(getClientName(clientChannel) + ":" + fwdMsg);
                    // 转发消息
                    forwardMsg(clientChannel, fwdMsg);
                    buffer.clear();
                    if (readyToQuit(fwdMsg)) {
                        removeClient(this);
                    } else {
                        // 继续接收消息并转发
                        clientChannel.read(buffer, buffer, this);
                    }
                }
            }
        }


        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读写操作失败：" + exc.getMessage());
        }
    }

    // 是否准备退出
    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }


    // 接收消息
    private String receive(ByteBuffer buffer) {
        return String.valueOf(charset.decode(buffer));
    }

    // 添加客户端
    private synchronized void addClient(ClientHandler clientHandler) {
        connectedClients.add(clientHandler);
        System.out.println(getClientName(clientHandler.getClientChannel()) + "已经连接");
    }

    // 移除客户端
    private synchronized void removeClient(ClientHandler clientHandler) {
        AsynchronousSocketChannel clientChannel = clientHandler.getClientChannel();
        connectedClients.remove(clientHandler);
        System.out.println(getClientName(clientChannel) + "已经断开连接");
        close(clientChannel);
    }

    // 转发消息
    private synchronized void forwardMsg(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        for (ClientHandler connectedHandler : connectedClients) {
            AsynchronousSocketChannel client = connectedHandler.getClientChannel();
            if (!client.equals(clientChannel)) {
                try {
                    // 将消息存入缓存区中
                    ByteBuffer buffer = charset.encode(getClientName(client) + fwdMsg);
                    // 写给每个客户端
                    client.write(buffer, null, connectedHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        ChatServer charServer = new ChatServer(8888);
        charServer.start();
    }

}
