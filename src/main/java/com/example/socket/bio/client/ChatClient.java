package com.example.socket.bio.client;

import java.io.*;
import java.net.Socket;

/**
 * @description:
 * @author: chenzhuo
 * @create: 2021-03-11 21:55
 */
public class ChatClient {
    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final String QUIT = "quit";
    private final int DEFAULT_SERVER_PORT = 8888;

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    // 发送消息给服务器
    public void send(String message) throws IOException {
        if (!socket.isOutputShutdown()) {
            bufferedWriter.write(message + "\n");
            bufferedWriter.flush();
        }
    }

    // 接收服务器消息
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = bufferedReader.readLine();
        }
        return msg;
    }

    // 检查用户是否准备退出
    public boolean isReadyToQuit(String msg){
        return QUIT.equals(msg);
    }

    public void start() {
        try {
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            // 创建IO流
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // 处理用户输入
            new Thread(new UserInputHandler(this)).start();
            // 读取服务器转发的消息
            String msg = null;
            while ((msg = receive())!= null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭客户端
            close();
        }

    }

    // 关闭客户端socket
    public synchronized void close() {
        if (socket != null) {
            try {
                socket.close();
                System.out.println("关闭Socket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }

}
