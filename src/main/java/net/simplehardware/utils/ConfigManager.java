package net.simplehardware.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final String CONFIG_PATH = System.getProperty("user.home")
            + File.separator + ".mazeeditor_config.json";


    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigData config = new ConfigData();

    static {
        loadConfig();
    }

    public static class ConfigData {
        public boolean confirmationsEnabled = true;
    }

    public static boolean isConfirmationsEnabled() {
        return config.confirmationsEnabled;
    }

    public static void setConfirmationsEnabled(boolean enabled) {
        config.confirmationsEnabled = enabled;
        saveConfig();
    }

    public static void loadConfig() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            saveConfig(); // create default file
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            config = gson.fromJson(reader, ConfigData.class);
            if (config == null) config = new ConfigData();
        } catch (IOException e) {
            e.printStackTrace();
            config = new ConfigData();
        }
    }

    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
