package com.example.socket.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @description: 聊天服务器
 * @author: chenzhuo
 * @create: 2021-03-11 21:55
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");
    private int port;


    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            server = ServerSocketChannel.open();
            // 设置通道为非阻塞模式
            server.configureBlocking(false);
            // 绑定端口
            server.bind(new InetSocketAddress(port));
            // 创建selector对象
            selector = Selector.open();
            // 将server注册到selector上，并且关联accept操作
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，端口[" + port + "]");

            while (true) {
                // 此方法会阻塞，直到有事件触发
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    handles(selectionKey);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);
        }
    }

    // 检查用户是否准备退出
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handles(SelectionKey selectionKey) throws IOException {
        // accept事件 - 和客户端建立了连接
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            // 将Client注册到selector上，并且关联read操作
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) + "已连接");
        }
        // read事件 - 客户端发送了消息
        else if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                // 客户端异常，selectionKey对应的通道和channel之间的注册关系被取消
                selectionKey.cancel();
                // 唤醒selector,selector重新获取所有的SelectionKey
                selector.wakeup();
            } else {
                // 转发消息
                forWardMessage(client, fwdMsg);

                // 检查用户是否退出
                if (readyToQuit(fwdMsg)) {
                    selectionKey.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client) + "已断开");
                }
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        // 清空消息
        rBuffer.clear();
        while (client.read(rBuffer) > 0);
        // 写模式转换为读模式
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private void forWardMessage(SocketChannel client, String fwdMsg) throws IOException {
        for (SelectionKey key : selector.keys()) {
            Channel connectedClient = key.channel();
            // 不需要将消息发送给服务端
            if (connectedClient instanceof ServerSocketChannel) {
                continue;
            }
            // 也不需要转发给自己
            if (key.isValid() && !client.equals(connectedClient)) {
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(client) + ":" + fwdMsg));
                // 写模式转换为读模式
                wBuffer.flip();
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel)connectedClient).write(wBuffer);
                }
            }
        }
    }

    private String getClientName(SocketChannel channel) {
        return "客户端[" + channel.socket().getPort() + "]";
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }

}
