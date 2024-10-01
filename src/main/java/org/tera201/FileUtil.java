package org.tera201;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public final class FileUtil {

    private FileUtil() {
        throw new UnsupportedOperationException("FileUtil class");
    }

    public static boolean lineInFileStartsWith(File file, String trackInfo) {
        try {
            if (!file.exists()) return false;
            log.info( "{} already exists in file {}", trackInfo, file.getPath());

            return Files.lines(file.toPath())
                    .anyMatch(line -> line.startsWith(trackInfo));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public static boolean isLineInFile(File file, String trackInfo) throws IOException {
        if (!file.exists()) return false;

        return Files.lines(file.toPath())
                .anyMatch(line -> line.equals(trackInfo));
    }

    public static void appendLineToFile(File file, String trackInfo) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(trackInfo);
            writer.newLine();
        }
    }


}
