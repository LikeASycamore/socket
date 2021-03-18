package com.example.socket.bio.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @description:
 * @author: chenzhuo
 * @create: 2021-03-11 21:56
 */
public class UserInputHandler implements Runnable{
    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        try {
            // 等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();
                // 向服务器发送消息
                chatClient.send(input);

                // 检查用户是否准备退出
                if (chatClient.isReadyToQuit(input)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            chatClient.close();
        }
    }

}
