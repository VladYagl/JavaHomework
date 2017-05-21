package ru.ifmo.ctddev.yaglamunov.bank;

import java.rmi.Naming;

public class Server {
    private final static int PORT = 8888;
    public static void main(String[] args) {
        try {
            Bank bank = new BankImpl(PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (Exception e) {
            System.out.println("Can't start server");
            e.printStackTrace();
            return;
        }
        System.out.println("Server started");
    }
}
