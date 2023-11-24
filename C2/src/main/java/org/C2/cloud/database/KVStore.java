package org.C2.cloud.database;

import org.json.JSONException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            this.setuplog();
        }
    }

    private void setuplog() {
        try {
            FileHandler filehandler = new FileHandler("kvstore.log", true);
            filehandler.setFormatter(new SimpleFormatter());

            this.logger.addHandler(filehandler);
        } catch (IOException ignored) {}
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
            if (this.logging) {
                this.logger.log(Level.SEVERE, "Error while reading file: " + filepath, e);
            }

            return Optional.empty();
        }
    }

    public void put(String server, String key, String contents) {
        String svdir = this.directory + File.separator + server;
        String filepath = this.directory + File.separator + key + ".json";

        try {
            Path serverPath = Paths.get(svdir);
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
