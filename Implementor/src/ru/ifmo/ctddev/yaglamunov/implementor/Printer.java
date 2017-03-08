package ru.ifmo.ctddev.yaglamunov.implementor;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Objects;

class Printer {
    Writer writer;

    Printer(Writer writer) {
        this.writer = writer;
    }

    void println(String a) throws IOException {
        writer.write(a);
        writer.write("\n");
    }

    void println() throws IOException {
        println("");
    }

    void println(String format, Object... args) throws IOException {
        println(String.format(format, args));
    }

    void print(String a) throws IOException {
        writer.write(a);
    }

    void print(String format, Object... args) throws IOException {
        print(String.format(format, args));
    }

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
