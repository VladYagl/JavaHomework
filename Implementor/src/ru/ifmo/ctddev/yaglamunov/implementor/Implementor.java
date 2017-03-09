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
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation of class {@code JarImpler}
 */
public class Implementor implements JarImpler {

    /**
     * Charset for writing <tt>.java</tt> file.
     */
    private static final Charset charset = Charset.forName("UTF-8");

    /**
     * Prints {@code aClass}'s constructors using {@code printer}.
     * <p>
     * All constructors simply call super class constructor.
     *
     * @param aClass  class to print constructors for.
     * @param printer print for writing constructors.
     * @throws IOException     if <tt>printer</tt> throws.
     * @throws ImplerException if <tt>aClass</tt> don't have available default constructor.
     */
    private void printConstructors(Class aClass, Printer printer) throws IOException, ImplerException {
        boolean nonPrivate = false;
        for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            nonPrivate = true;
            printer.println();
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

        if (!aClass.isInterface() && !nonPrivate) {
            throw new ImplerException("There is no default constructor available in " + aClass.getSimpleName());
        }
    }

    /**
     * Prints {@code aClass}'s abstract methods using {@code printer}.
     * <p>
     * All methods return default value for return type.
     *
     * @param aClass  class to print constructors for.
     * @param printer print for writing constructors.
     * @throws IOException if <tt>printer</tt> throws.
     */
    private void printMethods(Class aClass, Printer printer) throws IOException {
        List<Method> methods = new ArrayList<>();
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
                    printer.print("\n" + fullMethod);
                }
            }
        }
    }

    /**
     * Returns a directory for <tt>.java</tt> file depending on {@code aClass} package.
     *
     * @param aClass class to return directory for.
     * @param path   root directory.
     * @return a directory for {@code aClass} package.
     */
    private Path getSourceDir(Class aClass, Path path) {
        return path.resolve(aClass.getPackage().getName().replace(".", File.separator));
    }

    /**
     * Returns a directory for <tt>.java</tt> file depending on <tt>aClass</tt> package
     * from the current working directory.
     *
     * @param aClass class to return directory for.
     * @return a directory for {@code aClass} package.
     */
    private Path getSourceDir(Class aClass) {
        return Paths.get(aClass.getPackage().getName().replace(".", File.separator));
    }

    /**
     * Produces class implementing {@code aClass} with same full name but <tt>Impl</tt> suffix added.
     * File placed in correct sub directory of the {@code path}
     *
     * @param aClass class or interface to create implementation for.
     * @param path   root directory.
     * @throws ImplerException when when implementation cannot be
     *                         generated.
     */
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
            Printer printer = new Printer(new UnicodeWriter(writer));
            printer.println("package " + aClass.getPackage().getName() + ";\n");
            printer.print("public class %sImpl %s %s {\n", aClass.getSimpleName(), aClass.isInterface() ? "implements" : "extends", aClass.getName());

            printConstructors(aClass, printer);
            printMethods(aClass, printer);

            printer.println("}\n");
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Produces <tt>.jar</tt> file implementing {@code aClass} with same full name but <tt>Impl</tt> suffix added.
     *
     * @param aClass  class or interface to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException when when implementation cannot be
     *                         generated.
     */
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

    /**
     * Wraps other writer to replace Unicode characters with \{@code uXXXX}.
     */
    private class UnicodeWriter extends Writer {
        /**
         * Writer which this wraps.
         */
        Writer writer;

        /**
         * Construct {@code UnicodeWriter} to wrap provided writer.
         *
         * @param other writer to be wrapped.
         */
        UnicodeWriter(Writer other) {
            writer = other;
        }

        /**
         * {@inheritDoc}
         * <p>
         * replaces Unicode characters with \{@code uXXXX}.
         *
         * @param cbuf {@inheritDoc}
         * @param off  {@inheritDoc}
         * @param len  {@inheritDoc}
         * @throws IOException {@inheritDoc}
         */
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                if (cbuf[i] < 128) {
                    writer.write(cbuf[i]);
                } else {
                    writer.write(String.format("\\u%04X", (int) cbuf[i]));
                }
            }
        }

        /**
         * {@inheritDoc}
         *
         * @throws IOException {@inheritDoc}
         */
        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        /**
         * {@inheritDoc}
         *
         * @throws IOException {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}
