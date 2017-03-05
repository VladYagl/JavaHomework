package ru.ifmo.ctddev.yaglamunov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

public class Implementor implements Impler {

    private static final Charset charset = Charset.forName("UTF-8");

    private BufferedWriter localWriter;

    private void println(String a) throws IOException {
        localWriter.write(a);
        localWriter.newLine();
    }

    private void println() throws IOException {
        println("");
    }

    private void println(String format, Object... args) throws IOException {
        println(String.format(format, args));
    }

    private void print(String a) throws IOException {
        localWriter.write(a);
    }

    private void print(String format, Object... args) throws IOException {
        print(String.format(format, args));
    }

    private void printParameters(Parameter[] parameters) throws IOException {
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

    private void printConstructors(Class aClass) throws IOException, ImplerException {
        boolean nonPrivate = false;
        for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            nonPrivate = true;
            print("\tpublic %sImpl(", aClass.getSimpleName());
            printParameters(constructor.getParameters());
            print(")");
            if (constructor.getExceptionTypes().length > 0) {
                print(" throws ");
                boolean first = true;
                for (Class exception : constructor.getExceptionTypes()) {
                    if (!first) {
                        print(", ");
                    } else {
                        first = false;
                    }
                    print(exception.getCanonicalName());
                }
            }
            print(" {");
            if (constructor.getParameters().length > 0) {
                print("super(");
                for (int i = 0; i < constructor.getParameters().length; i++) {
                    if (i != 0) {
                        print(", ");
                    }
                    print("arg" + i);
                }
                print(");");
            }
            println("}");
        }
        println();

        if (!aClass.isInterface() && !nonPrivate) {
            throw new ImplerException("There is no default constructor available in " + aClass.getSimpleName());
        }
    }

    private void printMethod(Method method, String modif) throws IOException {
        print("\t%s %s %s(", modif, method.getReturnType().getCanonicalName(), method.getName());
        printParameters(method.getParameters());
        print(") {");
        if (method.getReturnType() != void.class) {
            print("return %s;", getDefaultValue(method.getReturnType()));
        }
        println("}");
    }

    private void printMethods(Class aClass) throws IOException {
        HashSet<Method> methods = new HashSet<>();
        Class a = aClass;
        while (a != null) {
            methods.addAll(Arrays.asList(a.getDeclaredMethods()));
            a = a.getSuperclass();
        }

        for (Method method : aClass.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                printMethod(method, "public");
            }
        }

        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers()) && Modifier.isProtected(method.getModifiers())) {
                printMethod(method, "protected");
            }
        }
    }

    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        if (aClass == null || path == null) {
            throw new ImplerException("Null arguments");
        }
        if (aClass.isPrimitive()) {
            throw new ImplerException("Primitive class");
        }
        if (aClass.isArray()) {
            throw new ImplerException("Array class");
        }
        if (Modifier.isFinal(aClass.getModifiers())) {
            throw new ImplerException("Final class");
        }

        try {
            path = Paths.get(path.toString() + File.separator + aClass.getPackage().getName().replace(".", File.separator));
            Files.createDirectories(path);
        } catch (IOException | InvalidPathException e) {
            throw new ImplerException(e);
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(Paths.get(path.toString() + File.separator + aClass.getSimpleName() + "Impl.java"), charset)) {
            localWriter = writer;
            println("package " + aClass.getPackage().getName() + ";");
            print("public class %sImpl %s %s", aClass.getSimpleName(), aClass.isInterface() ? "implements" : "extends", aClass.getName());

            if (aClass.getInterfaces().length > 0) {
                int count = 1;
                if (!aClass.isInterface()) {
                    print(" implements ");
                    count = 0;
                }
                for (Class inter : aClass.getInterfaces()) {
                    if (count++ != 0) {
                        print(", ");
                    }
                    print(inter.getName());
                }
            }
            println("{\n");

            printConstructors(aClass);
            printMethods(aClass);

            println("}");
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }
}
