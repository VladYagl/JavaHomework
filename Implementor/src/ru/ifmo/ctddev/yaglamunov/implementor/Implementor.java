package ru.ifmo.ctddev.yaglamunov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * Provides implementation for interfaces {@link Impler} and {@link JarImpler}.
 */
public class Implementor implements Impler, JarImpler {

    /**
     * Simple name of class to implement.
     */
    private String className = "";

    /**
     * Default indent in code.
     */
    private static final String INDENT = "    ";

    /**
     * Default constructor
     * <p>
     * Does nothing
     * </p>
     */
    Implementor() {}
    /**
     * Type token to implement.
     */
    private Class<?> cls;

    /**
     * Internal string buffer, used to write constructors or methods by appending each elements during the generation.
     * <p>
     * Resets after writing each method to file.
     * </p>
     */
    private StringBuilder sb = new StringBuilder();

    /**
     * Entry point of the program for command line arguments.
     * <p>
     * Usage:
     * <ul>
     * <li>{@code java -jar Implementor.jar -jar class-to-implement path-to-jar}</li>
     * <li>{@code java -jar Implementor.jar class-to-implement path-to-class}</li>
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
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("not enought amount of parametrs");
        } catch (ClassNotFoundException e) {
            System.err.println("no such class: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("error while implementing class: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("invalid path " + e.getMessage());
        }
    }

    /**
     * Produces code implementing for java class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name have to contains of type token full name and suffix <tt>Impl</tt>
     * Generated code should be placed in the correct subdirectory of the specified <tt>root</tt>
     * directory and have correct filename i.e. the implementation of the
     * interface {@link java.util.List} should be placed into <tt>$root/java/util/ListImpl.java</tt>
     *
     * @param token type token to create implementation for
     * @param root  root directory
     * @throws ImplerException {@link info.kgeorgiy.java.advanced.implementor.ImplerException}
     *                         when implementation can't be generated
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        if (root == null || token == null) {
            throw new ImplerException("Wrong arguments, here is null in arguments");
        }

        if (token.isPrimitive() || token.isArray() || token == Enum.class) {
            throw new ImplerException("token should be a class or an interface");
        }
        cls = token;
        className = cls.getSimpleName() + "Impl";
        if (Modifier.isFinal(cls.getModifiers())) {
            throw new ImplerException("here was in attempt to impl final class");
        }

        try (Writer printWriter = new UnicodeFilter(Files.newBufferedWriter(getFileName(this.cls, root)))) {
            printPackage(printWriter);
            printClassHeader(printWriter);
            printConstructors(printWriter);
            printMethods(printWriter);
            printWriter.write("\n");
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Creates <tt>.jar</tt> file of implemented class or interface <code>aClass</code>
     * by using {@link Implementor#implement(Class, Path)} new file have to be printed into the correct
     * subdirectory according to <code>path</code>
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt>
     * suffix added.
     * </p>
     *
     * @param aClass type token to create implementation for.
     * @param path   target <tt>.jar</tt> file.
     * @throws ImplerException {@link ImplerException} when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        //Path tmpFilesDir = Paths.get("javax");
        try {
            Path tmp = Paths.get(".");
            Implementor implementor = new Implementor();
            implementor.implement(aClass, tmp);
            Path fileToCompile = implementor.getFileName(aClass, tmp).normalize();
            compileClass(tmp, fileToCompile);
            Path classfile = getClassPathJar(fileToCompile);
            printJar(path, classfile);
            classfile.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new ImplerException("I/O error: " + e.getMessage());
        }
    }

    /**
     * Receives and prints package of implementing class by using {@link Writer} into the required file.
     *
     * @param printWriter output destination.
     * @throws IOException error during writing into required file.
     */
    private void printPackage(Writer printWriter) throws IOException {
        if (cls.getPackage() != null) {
            printWriter.write("package " + cls.getPackage().getName() + ";\n");
        }
    }

    /**
     * Receives and prints header of implementing class by using {@link Writer} into the required file.
     *
     * @param printWriter output destination.
     * @throws IOException error during writing into required file.
     */
    private void printClassHeader(Writer printWriter) throws IOException {
        printWriter.write(Modifier.toString(cls.getModifiers() & ~(Modifier.ABSTRACT | Modifier.INTERFACE)) +
                " class " +
                className +
                (cls.isInterface() ? " implements " : " extends ") +
                cls.getSimpleName() +
                " {\n");
    }

    /**
     * Receives and prints constructors of implementing class by using {@link Writer} into the required file.
     *
     * @param printWriter output destination.
     * @throws IOException     error during writing into required file.
     * @throws ImplerException {@link ImplerException} when
     *                         implementing class have no public or protected constructors.
     */
    private void printConstructors(Writer printWriter) throws IOException, ImplerException {
        boolean NPrivateConstructor = false;
        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            NPrivateConstructor = true;
            printHeader(printWriter, className, constructor);
            sb.setLength(0);
            sb.append(" {\n")
                    .append(INDENT)
                    .append(INDENT)
                    .append("super(");

            for (int i = 0; i < constructor.getParameterCount(); ++i) {
                sb.append(i == 0 ? "arg" + i : ", arg" + i);
            }

            printWriter.write(sb.append(");\n")
                    .append(INDENT)
                    .append("}")
                    .toString());
        }

        if (!cls.isInterface() && !NPrivateConstructor) {
            throw new ImplerException("no public or protected constructor");
        }
    }

    /**
     * Receives and prints methods of implementing class by using {@link Writer} into the required file.
     *
     * @param printWriter output destination.
     * @throws IOException error during writing into required file.
     */
    private void printMethods(Writer printWriter) throws IOException {
        Set<MethodWrapper> methods = new HashSet<>();
        for (Method method : cls.getMethods()) {
            methods.add(new MethodWrapper(method));
        }
        Class<?> clsTmp = cls;
        while (clsTmp != null && !clsTmp.equals(Object.class)) {
            for (Method method : clsTmp.getDeclaredMethods()) {
                methods.add(new MethodWrapper(method));
            }
            clsTmp = clsTmp.getSuperclass();
        }

        for (MethodWrapper method : methods) {
            if (!Modifier.isAbstract(method.method.getModifiers())) {
                continue;
            }
            printMethod(method, printWriter);
        }
        printWriter.write("}\n");
    }

    /**
     * Receives and prints current method of implementing class by using {@link Writer} into the required file.
     *
     * @param method      current method of implementing file
     *                    which was received by using {@link Implementor#printMethods(Writer)}.
     * @param printWriter output destination.
     * @throws IOException error during printing method.
     */
    private void printMethod(MethodWrapper method, Writer printWriter) throws IOException {
        printHeader(printWriter, method.method.getName(), method.method);
        StringBuilder sb = new StringBuilder(" {\n");

        if (method.method.getReturnType() != void.class) {
            sb.append(INDENT)
                    .append(INDENT)
                    .append("return ")
                    .append(getReturnedValueByDefaultMethod(method.method.getReturnType()))
                    .append(";\n");
        } else {
            sb.append(INDENT)
                    .append(INDENT)
                    .append("return; \n");
        }
        printWriter.write(sb.append(INDENT)
                .append("}\n")
                .toString());
    }

    /**
     * Prints header of {@link Executable} by using {@link Writer}.
     * <p>
     * Prints return type of executable if it is a method, name of executable,
     * parameters it takes (with fully qualified type names) and exceptions it can throw.
     * </p>
     *
     * @param printWriter    output destination.
     * @param executableName name of executable to be printed.
     * @param executable     executable whose header is printed.
     * @throws IOException error during printing header.
     */
    private void printHeader(Writer printWriter, String executableName, Executable executable)
            throws IOException {
        sb.setLength(0);
        sb.append("\n")
                .append(INDENT)
                .append(Modifier.toString(executable.getModifiers() &
                        ~(Modifier.INTERFACE | Modifier.ABSTRACT | Modifier.TRANSIENT)))
                .append(" ")
                .append((executable instanceof Method) ?
                        ((Method) executable).getReturnType().getCanonicalName() + " " : "")
                .append(executableName)
                .append("(");

        printWriter.write(sb.toString());
        printArguments(printWriter, executable);
        printExceptions(printWriter, executable);
    }

    /**
     * Prints arguments of {@link Executable} by using {@link Writer}.
     *
     * @param printWriter output destination.
     * @param executable  executable whose header is printed.
     * @throws IOException error during printing arguments.
     */
    private void printArguments(Writer printWriter, Executable executable) throws IOException {
        Parameter[] parameters = executable.getParameters();
        printWriter.write(Arrays.stream(parameters)
                .map((param) ->
                        param.getType().getCanonicalName()
                                + " "
                                + param.getName())
                .collect(Collectors.joining(", ")));
        printWriter.write(")");
    }

    /**
     * Prints exceptions of {@link Executable} by using {@link Writer}.
     *
     * @param printWriter output destination.
     * @param executable  executable whose header is printed.
     * @throws IOException error during printing arguments.
     */
    private void printExceptions(Writer printWriter, Executable executable) throws IOException {
        Class<?> exceptions[] = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            printWriter.write(" throws");
            printWriter.write(Arrays.stream(exceptions)
                    .map((exep) ->
                            " " + exep.getCanonicalName())
                    .collect(Collectors.joining(", ")));
        }
    }

    /**
     * Generates the string representing the default value of class with given <code>token</code>.
     * <p>
     * It is <tt>null</tt> for non-primitive types,
     * <tt>false</tt> for {@link Boolean boolean},
     * empty string for {@link Void void}
     * and <tt>0</tt> for other.
     *
     * @param type type token to get default value for.
     * @return {@link java.lang.String} with default value.
     */
    private String getReturnedValueByDefaultMethod(Class<?> type) {
        if (type.equals(boolean.class)) {
            return "true";
        } else if (type.equals(void.class)) {
            return "";
        } else {
            return type.isPrimitive() ? "0" : "null";
        }
    }

    /**
     * Builds a path, where implementation of <code>clazz</code> should be placed.
     * Implementation file has suffix <tt>Impl.java</tt> and
     * is placed in <tt>$root/&lt;package-dirs&gt;/</tt> folder.
     * <p>
     * All directories are creating automatically.
     *
     * @param cls  Token to resolve implementation path for.
     * @param path Directory to be treated as packages root.
     * @return Path to file with implementation.
     * @throws IOException If creation of directories has failed.
     */
    private Path getFileName(Class<?> cls, Path path) throws IOException {

        if (cls.getPackage() != null) {
            path = path.resolve(cls.getPackage()
                    .getName()
                    .replace('.', '/')
                    + "/");
            Files.createDirectories(path);
        }
        return path.resolve(cls.getSimpleName() + "Impl.java");
    }

    /**
     * Wrapper for Method class with custom {@link MethodWrapper#hashCode()} implementation.
     * <p>
     * {@link MethodWrapper#equals(Object)} and {@link MethodWrapper#hashCode()} implemented here
     * are used for comparing methods on their signature.
     * </p>
     */
    private class MethodWrapper {
        /**
         * current method.
         */
        Method method;

        /**
         * Constructor which takes method we want to create MethodWrapper for.
         *
         * @param method we want to make MethodWrapper for.
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Test of equality of signatures.
         *
         * @param obj {@code MethodWrapper} to be compared to.
         * @return {@code true} if signatures match, {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodWrapper)) {
                return false;
            }
            MethodWrapper that = (MethodWrapper) obj;
            return this.method.getName().equals(that.method.getName()) &&
                    Arrays.equals(method.getParameters(), method.getParameters());
        }

        /**
         * Computes hashcode based on method name and parameters' types only.
         *
         * @return hashcode.
         */
        @Override
        public int hashCode() {
            Parameter[] parameters = method.getParameters();
            int hashCode = ((Integer.hashCode(parameters.length) + 1) * 17 + method.getName().hashCode()) % 1023;
            for (Parameter parameter : parameters) {
                hashCode = (hashCode * 17 + parameter.getType().getCanonicalName().hashCode()) % 1023;
            }
            return hashCode;
        }
    }

    /**
     * Generates a path to compiled .class file by path to .java file by replacing
     * <code>execFileName</code> .java extension to .class extension.
     *
     * @param execFileName A path to .java file.
     * @return A path to corresponding .class file in same folder.
     * @throws IllegalArgumentException If <code>execFileName</code> doesn't have .java extension.
     */
    private Path getClassPathJar(Path execFileName) {
        String pathStr = execFileName.toString();
        if (pathStr.endsWith(".java")) {
            return Paths.get(pathStr.substring(0, pathStr.length() - 5) + ".class");
        } else {
            throw new IllegalArgumentException("Is not a java file");
        }
    }

    /**
     * Compiles given file.
     * <p>
     * Using default java compiler, provided by {@link ToolProvider#getSystemJavaCompiler()}, compiles
     * the file located at <code>codeFileName</code>, using <code>packageRoot</code> as a classpath.
     *
     * @param packageRoot  Path to a package root, will be used as a classpath.
     * @param codeFileName Path to a file. Should already contain path to the package.
     * @throws ImplerException {@link ImplerException} when
     *                         default compiler is unavailable or compilation error has occurred
     *                         (compiler returned non-zero exit code).
     */
    private void compileClass(Path packageRoot, Path codeFileName) throws ImplerException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new ImplerException("Compilation exception: compiler wasn't found");
        }
        int returnCode = javaCompiler.run(null, null, null, codeFileName.toString(), "-cp",
                packageRoot
                        + File.pathSeparator
                        + System.getProperty("java.class.path"),
                "-encoding", "Cp866"
        );
        if (returnCode != 0) {
            throw new ImplerException("Compilation exception: Compiler returned non-zero exit code");
        }
    }

    /**
     * Creates jar archive at <code>jarPath</code> with given <code>jarPath</code>
     * and copies existing <code>fileToPrint</code> to it.
     * <p>
     * If archive already exists, overwrites it.
     * <p>
     * Creates <tt>MANIFEST.MF</tt> file too with the <tt>MAIN_CLASS</tt> named <code>fileToPrint</code>
     * and  <tt>MANIFEST_VERSION</tt>: <tt>1.0</tt>
     * </p>
     *
     * @param jarPath     The path for jar archive. Directories on path should exist.
     * @param fileToPrint The name of file to be copied into archive. Should exist.
     * @throws IOException If I/O error occurred, e.g. if <code>fileName</code> of <code>jarDirectory</code>
     *                     don't exist, or if unexpected error happened during creating or writing to archive.
     */
    private void printJar(Path jarPath, Path fileToPrint) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            jarOutputStream.putNextEntry(new ZipEntry(fileToPrint.toString()));
            Files.copy(fileToPrint, jarOutputStream);
        }
    }

    /**
     * Filters non-ASCII characters in output stream and converts it to "\\uXXXX" sequences.
     * <p>
     * Use {@link #write(String, int, int)} to filter.
     */
    private class UnicodeFilter extends FilterWriter {

        /**
         * Construct the Filter on provided Writer.
         *
         * @param out writer to filter.
         */
        protected UnicodeFilter(Writer out) {
            super(out);
        }

        /**
         * Writes symbol in requires format.
         *
         * @param c symbol to write.
         * @throws IOException error during writing symbol.
         */
        @Override
        public void write(int c) throws IOException {
            if (c >= 128) {
                super.write(String.format("\\u%04X", (int) c));
            } else {
                super.write(c);
            }
        }

        /**
         * Replaces unicode characters in <code>string</code> to <code>"\\uXXXX"</code>" sequences.
         */
        @Override
        public void write(String string, int off, int len) throws IOException {
            for (char c : string.substring(off, off + len).toCharArray()) {
                write(c);
            }
        }
    }

}


