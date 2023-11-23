package org.C2.cloud.database;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
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
    private final Logger logger;

    public KVStore(String directory) {
        this.directory = directory;
        this.logger = Logger.getLogger(KVStore.class.getName());
        this.logsetup();
    }

    private void logsetup() {
        try {
            FileHandler filehandler = new FileHandler("kvstore.log", true);
            filehandler.setFormatter(new SimpleFormatter());

            this.logger.addHandler(filehandler);
        } catch (IOException ignored) {}
    }

    public void mksv(String server) {
        String svdir = this.directory + File.separator + server;

        try {
            Path path = Paths.get(svdir);
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException e) {
            this.logger.log(Level.INFO, "Server already exists", e);
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "Error creating server: " + svdir, e);
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
            this.logger.log(Level.SEVERE, "Error while reading file: " + filepath, e);

            return Optional.empty();
        }
    }

    public void put(String key, String contents) {
        String filepath = this.directory + File.separator + key + ".json";

        try (FileWriter writer = new FileWriter(filepath, StandardCharsets.UTF_8)) {
            writer.write(contents);
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "Error while writing to file: " + filepath, e);
        }
    }
}
