package ru.ifmo.ctddev.yaglamunov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;

/**
 * Sends request to the server, accepts the results and prints them.
 */
public class HelloUDPClient implements HelloClient {
    private final Charset charset = Charset.forName("UTF8");

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
                    String requestStr = prefix + threadNumber + "_" + requestNumber;
                    byte message[] = requestStr.getBytes(charset);
                    DatagramPacket request = new DatagramPacket(message, message.length, serverAddress);

                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    boolean success = false;
                    while (!Thread.interrupted() && !success) {
                        try {
                            if (Thread.interrupted()) {
                                return;
                            }
                            socket.send(request);
                            socket.receive(response);

                            String responseMessage = new String(response.getData(), response.getOffset(), response.getLength());
                            if (responseMessage.substring(7).equals(requestStr)) {
                                System.out.println(responseMessage);
                                success = true;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("SocketException");
            }
        }
    }

    /**
     * Starts client.
     * @param host name or ip-address computer where server is run.
     * @param port port to send requests to.
     * @param prefix prefix for the requests.
     * @param requests number of requests in each thread.
     * @param threads number of threads.
     */
    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        InetSocketAddress serverAddress = new InetSocketAddress(host, port);

        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(new Sender(requests, prefix, i, serverAddress));
            thread.start();
        }
    }

    /**
     * Entry point for {@code HelloUDPClient}.
     */
    public static void main(String[] args) {
        final int port;
        final int threads;
        final int requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("Wrong input");
            return;
        }

        new HelloUDPClient().start(args[0], port, args[2], requests, threads);
    }
}
