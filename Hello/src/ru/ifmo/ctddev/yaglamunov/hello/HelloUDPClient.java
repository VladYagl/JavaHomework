package ru.ifmo.ctddev.yaglamunov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import ru.ifmo.ctddev.yaglamunov.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    private CountDownLatch countDown;

    private class Sender implements Runnable {
        private final int requests;
        private final String prefix;
        private final int threadNumber;
        private final InetSocketAddress serverAddress;
        private final byte[] buffer = new byte[1024];

        private Sender(int requests, String prefix, int threadNumber, InetSocketAddress serverAddress) {
            this.requests = requests;
            this.prefix = prefix;
            this.threadNumber = threadNumber;
            this.serverAddress = serverAddress;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                socket.setSoTimeout(50);

                for (int requestNumber = 0; requestNumber < requests && !Thread.interrupted(); ++requestNumber) {
                    String requestStr = prefix + threadNumber + requestNumber;
                    byte message[] = requestStr.getBytes();
                    DatagramPacket request = new DatagramPacket(message, message.length, serverAddress);

                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    boolean success = false;
                    while (!Thread.interrupted() && !success) {
                        try {
                            socket.send(request);
                            socket.receive(response);

                            Log.println("Client received = [" + new String(response.getData(),
                                    response.getOffset(),
                                    response.getLength()) + "]");
                            Log.save();

                            String responseMessage = new String(response.getData(), response.getOffset(), response.getLength());
//                            if (responseMessage.substring(7).equals(requestStr)) {
                                System.out.println(responseMessage);
                                success = true;
//                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("SocketException");
            }
            countDown.countDown();
        }
    }

    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        InetSocketAddress serverAddress = new InetSocketAddress(host, port);
        countDown = new CountDownLatch(threads);

        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            threadPool.execute(new Sender(requests, prefix, i, serverAddress));
        }

        try {
            countDown.await();
        } catch (InterruptedException e) {
            System.err.println("CountDown waiting interrupted");
        }

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        final int port = Integer.parseInt(args[1]);
        final int threads = Integer.parseInt(args[3]);
        final int requests = Integer.parseInt(args[4]);

        new HelloUDPClient().start(args[0], port, args[2], requests, threads);
    }
}
