package org.C2.cloud.database;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        this.setupLogger();
    }

    private void setupLogger() {
        try {
            FileHandler filehandler = new FileHandler("kvstore.log", true);
            filehandler.setFormatter(new SimpleFormatter());

            this.logger.addHandler(filehandler);
        } catch (IOException ignored) {}
    }

    public Optional<JSONObject> get(String key) {
        String filepath = this.directory + File.separator + key;

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            return Optional.of(new JSONObject(new JSONTokener(content.toString())));
        } catch (IOException | JSONException e) {
            this.logger.log(Level.SEVERE, "Error while reading file: " + filepath, e);

            return Optional.empty();
        }

    }

    public void put(String key, String contents) {
        String filepath = this.directory + File.separator + key;

        try (FileWriter writer = new FileWriter(filepath, StandardCharsets.UTF_8)) {
            writer.write(contents);
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "Error while writing to file: " + filepath, e);
        }
    }
}
