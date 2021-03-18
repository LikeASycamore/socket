package com.example.socket.bio.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @description: 聊天服务器
 * @author: chenzhuo
 * @create: 2021-03-11 21:55
 */
public class ChatServer {

    private final int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectedClients;
    private ExecutorService executorService;

    public ChatServer() {
        executorService = Executors.newFixedThreadPool(10);
        connectedClients = new HashMap<>();
    }

    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            System.out.println("客户端[" + port + "]连接到服务器");
        }
    }

    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                // 关闭消息流
                connectedClients.get(port).close();
                connectedClients.remove(port);
            }
            System.out.println("客户端[" + port + "]已断开连接");
        }
    }

    // 将消息转发到其它在线客户
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Integer id : connectedClients.keySet()) {
            if (!id.equals(socket.getPort())) {
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    // 运行服务器
    public void start() {
        // 绑定监听端口
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口:" + DEFAULT_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                // 为每一个用户添加一个线程，并启动  优化方案：添加到线程池中
                executorService.execute(new ChatHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }

    }

    // 检查用户是否准备退出
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }


    // 关闭服务器socket
    public synchronized void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("关闭ServerSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }

}
