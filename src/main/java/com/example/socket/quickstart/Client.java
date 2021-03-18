package com.example.socket.quickstart;

import java.io.*;
import java.net.Socket;

/**
 * @description: 客户端
 * @author: chenzhuo
 * @create: 2021-03-11 20:14
 */
public class Client {

    public static void main(String[] args) {
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final String QUIT = "quit";
        final int DEFAULT_SERVER_PORT = 8888;
        try (Socket socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
             // 创建IO流
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        ) {
            while (true) {
                // 等待用户输入消息
                String input = reader.readLine();
                // 发送消息给服务器
                bufferedWriter.write(input + "\n");
                bufferedWriter.flush();
                // 读取服务器返回的消息
                System.out.println(bufferedReader.readLine());
                if (QUIT.equals(input)) {
                    System.out.println("客户端[" + socket.getPort() + "]退出！");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
