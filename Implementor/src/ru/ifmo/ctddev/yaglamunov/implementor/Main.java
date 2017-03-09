package ru.ifmo.ctddev.yaglamunov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Paths;

/**
 * Main class for {@code Implementor}
 *
 * @see Implementor
 */
public class Main {
    /**
     * Main function.
     * <p>
     * Usage:
     * <ul>
     * <li>{@code -jar class-to-implement path-to-jar} - generates a .jar file with implementation </li>
     * <li>{@code class-to-implement} - generates a .java file with implementation </li>
     * </ul>
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        try {
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get("."));
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Not enough arguments");
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Implementor error: ");
            e.printStackTrace();
        }
    }
}
