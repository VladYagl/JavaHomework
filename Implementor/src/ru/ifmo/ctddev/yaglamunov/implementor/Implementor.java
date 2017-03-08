package ru.ifmo.ctddev.yaglamunov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {

    private static final Charset charset = Charset.forName("UTF-8");

    private Printer printer;

    private void printConstructors(Class aClass) throws IOException, ImplerException {
        boolean nonPrivate = false;
        for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            nonPrivate = true;
            printer.print("\tpublic %sImpl(", aClass.getSimpleName());
            printer.printParameters(constructor.getParameters());
            printer.print(")");
            if (constructor.getExceptionTypes().length > 0) {
                printer.print(" throws ");
                boolean first = true;
                for (Class exception : constructor.getExceptionTypes()) {
                    if (!first) {
                        printer.print(", ");
                    } else {
                        first = false;
                    }
                    printer.print(exception.getCanonicalName());
                }
            }
            printer.print(" {");
            if (constructor.getParameters().length > 0) {
                printer.print("\n\t\tsuper(");
                for (int i = 0; i < constructor.getParameters().length; i++) {
                    if (i != 0) {
                        printer.print(", ");
                    }
                    printer.print("arg" + i);
                }
                printer.print(");\n\t");
            }
            printer.println("}\n");
        }
        printer.println();

        if (!aClass.isInterface() && !nonPrivate) {
            throw new ImplerException("There is no default constructor available in " + aClass.getSimpleName());
        }
    }

    private void printMethods(Class aClass) throws IOException {
        Set<Method> methods = new HashSet<>();
        Set<String> fullMethods = new HashSet<>();
        Class a = aClass;
        while (a != null) {
            methods.addAll(Arrays.asList(a.getMethods()));
            methods.addAll(Arrays.asList(a.getDeclaredMethods()));
            a = a.getSuperclass();
        }

        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                StringWriter stringWriter = new StringWriter();
                Printer stringPrinter = new Printer(stringWriter);
                stringPrinter.printMethod(method);
                String fullMethod = stringWriter.getBuffer().toString();
                if (!fullMethods.contains(fullMethod)) {
                    fullMethods.add(fullMethod);
                    printer.println(fullMethod);
                }
            }
        }
    }

    private Path getSourceDir(Class aClass, Path path) {
        return path.resolve(aClass.getPackage().getName().replace(".", File.separator));
    }

    private Path getSourceDir(Class aClass) {
        return Paths.get(aClass.getPackage().getName().replace(".", File.separator));
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
        if (aClass.equals(Enum.class)) {
            throw new ImplerException("Enum");
        }

        try {
            path = getSourceDir(aClass, path);
            Files.createDirectories(path);
        } catch (IOException | InvalidPathException e) {
            throw new ImplerException(e);
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(path.resolve(aClass.getSimpleName() + "Impl.java"), charset)) {
            printer = new Printer(new UnicodeWriter(writer));
            printer.println("package " + aClass.getPackage().getName() + ";\n");
            printer.print("public class %sImpl %s %s {\n", aClass.getSimpleName(), aClass.isInterface() ? "implements" : "extends", aClass.getName());

            printConstructors(aClass);
            printMethods(aClass);

            printer.println("}\n");
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    @Override
    public void implementJar(Class<?> aClass, Path jarFile) throws ImplerException {
        implement(aClass, Paths.get("./"));
        Path dir = getSourceDir(aClass);
        Path sourceFile = dir.resolve(aClass.getSimpleName() + "Impl.java");
        Path classFile = dir.resolve(aClass.getSimpleName() + "Impl.class");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not get java compiler");
        }
        if (compiler.run(null, null, null, sourceFile.toString(), "-encoding", "cp866") != 0) {
            throw new ImplerException("Compilation error");
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            jar.putNextEntry(new ZipEntry(classFile.toString()));
            Files.copy(classFile, jar);
            jar.close();
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(classFile);
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    private class UnicodeWriter extends FilterWriter {

        UnicodeWriter(Writer out) {
            super(out);
        }

        @Override
        public void write(int c) throws IOException {
            if (c < 128) {
                super.write(c);
            } else {
                super.write(String.format("\\u%04X", (int) c));
            }
        }
    }
}
