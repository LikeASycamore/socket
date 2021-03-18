package com.example.socket.bio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @description: 读取用户输入并转发的线程
 * @author: chenzhuo
 * @create: 2021-03-11 21:56
 */
public class ChatHandler implements Runnable{
    private ChatServer chatServer;
    private Socket socket;

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 存储新上线用户
            chatServer.addClient(socket);

            // 读取用户发送的消息
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = null;
            while ((msg = bufferedReader.readLine())!= null) {
                String fwdMsg = "客户端[" + socket.getPort() + "]:" + msg + "\n";
                System.out.println(fwdMsg);
                // 将消息转发给其它在线用户
                chatServer.forwardMessage(socket, fwdMsg);
                // 检查用户是否准备退出
                if (chatServer.readyToQuit(msg)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 最后需要移除离线用户
                chatServer.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
