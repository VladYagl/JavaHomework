package ru.ifmo.ctddev.yaglamunov.implementor;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Class to wrap {@code Writer} to print java code.
 *
 * @see Writer
 */
class Printer {
    /**
     * Writer to write with.
     */
    private Writer writer;

    /**
     * Constructs {@code Printer} with provided writer.
     *
     * @param writer writer to write with.
     */
    Printer(Writer writer) {
        this.writer = writer;
    }

    /**
     * Prints {@code String a} in a new line.
     *
     * @param a string to print.
     * @throws IOException if {@code writer} throws.
     */
    void println(String a) throws IOException {
        print(a + "\n");
    }

    /**
     * Prints new empty line.
     *
     * @throws IOException if {@code writer} throws.
     */
    void println() throws IOException {
        println("");
    }

    /**
     * Prints {@code String a} formatted string using the specified format string and
     * arguments in a new line.
     *
     * @param format A format string
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.
     * @throws IOException                      if {@code writer} throws.
     * @throws java.util.IllegalFormatException if {@code String.format} throws.
     * @see String#format(String, Object...)
     */
    void println(String format, Object... args) throws IOException {
        println(String.format(format, args));
    }

    /**
     * Prints {@code String a}.
     *
     * @param a string to print.
     * @throws IOException if {@code writer} throws.
     */
    void print(String a) throws IOException {
        writer.write(a);
    }

    /**
     * Prints {@code String a} formatted string using the specified format string and
     * arguments.
     *
     * @param format A format string
     * @param args   Arguments referenced by the format specifiers in the format
     *               string.
     * @throws IOException                      if {@code writer} throws.
     * @throws java.util.IllegalFormatException if {@code String.format} throws.
     * @see String#format(String, Object...)
     */
    void print(String format, Object... args) throws IOException {
        print(String.format(format, args));
    }

    /**
     * Prints {@code parameters} separated with ",".
     *
     * @param parameters parameters to print.
     * @throws IOException if {@code writer} throws.
     */
    void printParameters(Parameter[] parameters) throws IOException {
        int position = 0;
        for (Parameter parameter : parameters) {
            if (position++ != 0) {
                print(", ");
            }
            print("%s %s", parameter.getType().getCanonicalName(), parameter.getName());
        }
    }

    private static String getDefaultValue(Class clazz) {
        if (clazz == char.class) {
            return "\'\u0000\'";
        }
        return Objects.toString(Array.get(Array.newInstance(clazz, 1), 0)) + (clazz == float.class ? "F" : "");
    }

    /**
     * Prints abstract {@code method} in Java style.
     *
     * @param method methods to print.
     * @throws IOException if {@code writer} throws.
     */
    void printMethod(Method method) throws IOException {
        print("\t%s ", Modifier.toString(~Modifier.ABSTRACT & method.getModifiers() & Modifier.methodModifiers()));
        print("%s %s(", method.getReturnType().getCanonicalName(), method.getName());
        printParameters(method.getParameters());
        print(") {");
        if (method.getReturnType() != void.class) {
            print("\n\t\treturn %s;\n\t", getDefaultValue(method.getReturnType()));
        }
        println("}\n");
    }
}
