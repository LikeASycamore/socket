package com.example.socket.aio.chatroom;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @description:
 * @author: chenzhuo
 * @create: 2021-03-17 23:10
 */
public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        // 等待用户的输入
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                // 阻塞式去读控制台信息
                String message = consoleReader.readLine();
                chatClient.send(message);
                if (chatClient.readyToQuit(message)) {
                    System.out.println("已退出聊天室");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
