package com.github.mozvip.romm.filetypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatFactory {

    public static final Logger LOGGER = LoggerFactory.getLogger(DatFactory.class);

    public static Dat readDat(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            LOGGER.error("{} is not a regular file", file.toString());
            return null;
        }

        long start = System.currentTimeMillis();

        LOGGER.info("Loading dat file {}", file.toAbsolutePath().toString());

        String fileName = file.getFileName().toString();

        boolean xml = false;
        if (fileName.endsWith(".xml")) {
            xml = true;
        } else {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                try {
                    String firstLine = reader.readLine();
                    if (firstLine.contains("<?xml")) {
                        xml = true;
                    }
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        Dat result = xml ? XmlDat.read(file):Dat.read(file);
        LOGGER.info("{} loaded in {} ms, {} games found", file.getFileName().toString(), System.currentTimeMillis() - start, result.getGames().size());
        if (result.getName() == null) {
            String datName = fileName.substring(0, fileName.lastIndexOf('.'));
            result.setName(datName);
        }
        return result;
    }
}
