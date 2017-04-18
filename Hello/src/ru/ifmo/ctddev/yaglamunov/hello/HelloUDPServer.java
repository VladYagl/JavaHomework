package ru.ifmo.ctddev.yaglamunov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listen to packets sent by {@link HelloUDPClient} and answers them.
 */
public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService threadPool;

    private final Charset charset = Charset.forName("UTF8");

    private class Listener implements Runnable {
        final byte[] buffer = new byte[1024];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    socket.receive(packet);

                    byte[] message = ("Hello, " + new String(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength(),
                            charset)).getBytes(charset);

                    DatagramPacket response = new DatagramPacket(message, message.length, packet.getSocketAddress());
                    socket.send(response);
                } catch (IOException e) {
                    System.err.println("socket error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Starts server.
     *
     * @param port port to send requests to.
     * @param threads number of threads.
     */
    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Unable to create socket");
            return;
        }

        threadPool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            threadPool.execute(new Listener());
        }
    }

    /**
     * Closes server.
     */
    @Override
    public void close() {
        socket.close();
        threadPool.shutdownNow();
    }

    /**
     * Entry point for {@code HelloUDPServer}.
     */
    public static void main(String[] args) {
        final int port = Integer.parseInt(args[0]);
        final int threads = Integer.parseInt(args[1]);

        new HelloUDPServer().start(port, threads);
    }
}
