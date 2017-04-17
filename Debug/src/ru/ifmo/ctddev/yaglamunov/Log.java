package ru.ifmo.ctddev.yaglamunov;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Log {
    private static BufferedWriter writer;
    private static volatile boolean initialized = false;

    private synchronized static void init() {
        try {
            writer = new BufferedWriter(new FileWriter("log.txt"));
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void save() {
        if (!initialized) init();
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized static void print(String string) {
        if (!initialized) init();
        try {
            writer.append(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void println(String string) {
        print(string + "\n");
    }

    public synchronized static void println() {
        println("");
    }

    private static final Map<String, Integer> values = new HashMap<>();

    public synchronized static void putInt(String name, int value) {
        values.put(name, value);
    }

    public synchronized static Integer getInt(String name) {
        return values.get(name);
    }
}
