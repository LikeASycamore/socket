package com.example.socket.nio;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @description:
 * @author: chenzhuo
 * @create: 2021-03-14 22:50
 */
public class FileCopyDemo {

    private static final int ROUNDS = 5;

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void benchMark(FileCopyRunner fileCopyRunner, File source, File target) {
        long elapsed = 0L;
        for (int i = 0; i < ROUNDS; i++) {
            long startTime = System.currentTimeMillis();
            fileCopyRunner.fileCopy(source, target);
            elapsed += System.currentTimeMillis() - startTime;
            target.delete();
        }
        System.out.println(fileCopyRunner + ":" + elapsed / ROUNDS);
    }

    public static void main(String[] args) {
        // 不使用buffer，最简单的文件复制
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void fileCopy(File source, File target) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new FileInputStream(source);
                    out = new FileOutputStream(target);
                    int result;
                    while ((result = in.read()) != -1) {
                        out.write(result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(in);
                    close(out);
                }
            }

            @Override
            public String toString() {
                return "noBufferStreamCopy";
            }
        };

        // 使用bufferedxx
        FileCopyRunner bufferedStreamCopy = new FileCopyRunner() {
            @Override
            public void fileCopy(File source, File target) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(source));
                    out = new BufferedOutputStream(new FileOutputStream(target));

                    byte[] buffer = new byte[1024];
                    while (in.read(buffer) != -1) {
                        out.write(buffer);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(in);
                }
            }
            @Override
            public String toString() {
                return "bufferedStreamCopy";
            }
        };

        // 使用Channel与Buffer实现文件复制
        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void fileCopy(File source, File target) {
                FileChannel in = null;
                FileChannel out = null;

                try {
                    in = new FileInputStream(source).getChannel();
                    out = new FileOutputStream(target).getChannel();

                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    while (in.read(byteBuffer) != -1) {
                        // 将buffer由读模式转换为写模式
                        byteBuffer.flip();
                        // 将buffer所有数据全部读取
                        while (byteBuffer.hasRemaining()) {
                            out.write(byteBuffer);
                        }
                        // 又将buffer从读模式转换为写模式
                        byteBuffer.clear();

                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(in);
                    close(out);
                }
            }
            @Override
            public String toString() {
                return "nioBufferCopy";
            }
        };

        // 使用两个channel实现文件复制
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void fileCopy(File source, File target) {
                FileChannel in = null;
                FileChannel out = null;
                try {
                    in = new FileInputStream(source).getChannel();
                    out = new FileOutputStream(target).getChannel();
                    long transferrd = 0L;
                    long size = in.size();
                    while (transferrd != size) {
                        transferrd += in.transferTo(0, size, out);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public String toString() {
                return "nioTransferCopy";
            }
        };

        // 小文件，不到1M
        File source = new File("G:\\source\\small.txt");
        File target = new File("G:\\target\\small.txt");

        benchMark(bufferedStreamCopy, source, target);
        benchMark(nioBufferCopy, source, target);
        benchMark(nioTransferCopy, source, target);
        System.out.println("-------------------");

        // 中等大小文件 100M
        source = new File("G:\\source\\medium.pdf");
        target = new File("G:\\target\\medium.pdf");

        benchMark(bufferedStreamCopy, source, target);
        benchMark(nioBufferCopy, source, target);
        benchMark(nioTransferCopy, source, target);
        System.out.println("-------------------");


        // 大文件 500M
        source = new File("G:\\source\\big.wav");
        target = new File("G:\\target\\big.wav");
        benchMark(bufferedStreamCopy, source, target);
        benchMark(nioBufferCopy, source, target);
        benchMark(nioTransferCopy, source, target);
        System.out.println("-------------------");

    }
}
