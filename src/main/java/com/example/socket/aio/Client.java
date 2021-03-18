package com.example.socket.aio;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @description:
 * @author: chenzhuo
 * @create: 2021-03-17 16:11
 */
public class Client {
    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    AsynchronousSocketChannel clientChannel;

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            clientChannel = AsynchronousSocketChannel.open();
            Future<Void> future = clientChannel.connect(new InetSocketAddress(LOCALHOST,DEFAULT_PORT));
            future.get();

            // 等待用户的输入
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = bReader.readLine();
                byte[] inputBytes = input.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(inputBytes);

                // 返回写入的字节数
                Future<Integer> writeResult = clientChannel.write(buffer);
                writeResult.get();
                // todo 此处为什么要flip呢
                buffer.flip();
                // 读取服务器的数据
                Future<Integer> readResult = clientChannel.read(buffer);
                readResult.get();
                String echo = new String(buffer.array());
                System.out.println(echo);
                buffer.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            close(clientChannel);
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
