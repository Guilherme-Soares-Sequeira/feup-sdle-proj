package org.C2.cloud.database;

import org.json.JSONException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class KVStore {
    private final String directory;
    private final boolean logging;
    private Logger logger;

    public KVStore(String directory, boolean logging) {
        this.directory = directory;
        this.logging = logging;

        if (this.logging) {
            this.logger = Logger.getLogger(KVStore.class.getName());
            this.setupLog();
        }

        Path path = Paths.get(directory);

        if (!Files.exists(path)) {
            try {
                // Create the directory
                Files.createDirectories(path);
                System.out.println("Directory created: " + path);
            } catch (Exception e) {
                this.logger.log(Level.SEVERE, "Couldn't create directory: " + e);
            }
        }
    }

    public List<String> getLists() {

        List<String> lists = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(directory), EnumSet.noneOf(FileVisitOption.class), 1,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (Files.isRegularFile(file)) {
                                String fileName = file.getFileName().toString();
                                int lastDotIndex = fileName.lastIndexOf('.');
                                if (lastDotIndex > 0) {
                                    lists.add(fileName.substring(0, lastDotIndex));
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });

            return lists;
        } catch (Exception e) {
            System.out.println("GOt an exception");

            throw new RuntimeException("Couldn't read local lists: " + e);
        }
    }

    private void setupLog() {
        try {
            FileHandler filehandler = new FileHandler("kvstore.log", true);
            filehandler.setFormatter(new SimpleFormatter());

            this.logger.addHandler(filehandler);
        } catch (IOException ignored) {
        }
    }

    public Optional<String> get(String key) {
        String filepath = this.directory + File.separator + key + ".json";

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            return Optional.of(content.toString());
        } catch (IOException | JSONException e) {
            //if (this.logging) {this.logger.log(Level.SEVERE, "Error while reading file: " + filepath, e);}

            return Optional.empty();
        }
    }

    public void put(String key, String contents) {
        String filepath = this.directory + File.separator + key + ".json";

        System.out.println("[KVSTORE LOG] Inserting a json = " + contents + " and directory = " + directory);

        try {
            Path serverPath = Paths.get(this.directory);
            if (!Files.exists(serverPath)) {
                Files.createDirectories(serverPath);
            }

            try (FileWriter writer = new FileWriter(filepath, StandardCharsets.UTF_8)) {
                writer.write(contents);
            }
        } catch (IOException e) {
            if (this.logging) {
                this.logger.log(Level.SEVERE, "Error while writing to file: " + filepath, e);
            }
        }
    }
}
