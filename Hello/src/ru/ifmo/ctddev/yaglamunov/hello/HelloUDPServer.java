package ru.ifmo.ctddev.yaglamunov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import ru.ifmo.ctddev.yaglamunov.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService threadPool;

    private class Listener implements Runnable {
        final byte[] buffer = new byte[1024];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    socket.receive(packet);

                    Log.println("Server received = [" + new String(packet.getData(),
                            packet.getOffset(),
                            packet.getLength()) + "]");
                    Log.save();

                    byte[] message = ("Hello, " + new String(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength())).getBytes();

                    Log.println("Server send = [" + new String(message,
                            packet.getOffset(),
                            message.length) + "]");
                    Log.save();

                    DatagramPacket response = new DatagramPacket(message, message.length, packet.getSocketAddress());
                    socket.send(response);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("SocketException: ");
            return;
        }

        threadPool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            threadPool.execute(new Listener());
        }
    }

    @Override
    public void close() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        final int port = Integer.parseInt(args[0]);
        final int threads = Integer.parseInt(args[1]);

        new HelloUDPServer().start(port, threads);
    }
}
