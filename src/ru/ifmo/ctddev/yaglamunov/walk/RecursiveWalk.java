package ru.ifmo.ctddev.yaglamunov.walk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    private static final Charset charset = Charset.forName("UTF-8");

    private static int FNVHash(final byte[] bytes, int size, int lastHash) {
        for (int i = 0; i < size; i++) {
            lastHash *= 0x01000193;
            lastHash ^= (bytes[i] & 0xff);
        }
        return lastHash;
    }

    private static int FNVHash(InputStream stream) throws IOException {
        byte[] b = new byte[1024];
        int hash = 0x811c9dc5;
        int size;
        while ((size = stream.read(b)) >= 0) {
            hash = FNVHash(b, size, hash);
        }
        return hash;
    }

    public static void main(String[] args) {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Error in input parameters");
            return;
        }
        Path inputFile = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);
        try (final BufferedWriter writer = Files.newBufferedWriter(outputFile, charset)) {
            Files.lines(inputFile, charset).forEach(line -> {
                Path start = Paths.get(line);
                try {
                    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            writer.write(String.format("%08x" + " " + file.toString() + "\n", FNVHash(Files.newInputStream(file))));
                            return super.visitFile(file, attrs);
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            System.err.println("Visit file failed: " + file.toString());
                            writer.write(String.format("%08x" + " " + file.toString() + "\n", 0));
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    System.err.println("Error while walking");
                }
            });
        } catch (IOException e) {
            System.err.println("Error in input/output files");
        }
    }
}
