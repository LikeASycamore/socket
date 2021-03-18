package com.example.socket.quickstart;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @description: 服务端
 * @author: chenzhuo
 * @create: 2021-03-11 20:11
 */
public class Server {
    public static void main(String[] args) {
        final int DEFAULT_PORT = 8888;
        final String QUIT = "quit";

        try (
                ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
                Socket socket = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        ) {
            System.out.println("启动服务器，监听端口:" + DEFAULT_PORT);
            String msg;
            while ((msg = bufferedReader.readLine()) != null) {
                // 读取客户端发出的消息
                System.out.println("客户端[" + socket.getPort() + "]:" + msg);
                // 回复客户端的消息
                bufferedWriter.write("服务器收到：" + msg + "\n");
                bufferedWriter.flush();
                if (QUIT.equals(msg)) {
                    System.out.println("客户端[" + socket.getPort() + "]已断开连接！");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
