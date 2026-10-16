package com.elyrecode.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ElyrecodeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(), "elyrecode_config.json");

    public enum NameMode {
        ABOVE_ARMOR("Above Armor"),
        FREE("Free"),
        CROSSHAIR("Crosshair");

        public final String label;
        NameMode(String label) {
            this.label = label;
        }
    }

    public boolean showPercentage = false;
    public boolean showOpponentName = true;
    public boolean showDurabilityBar = true;
    public boolean showBarWhenFull = false;
    public boolean smartSnapping = true;
    public NameMode nameMode = NameMode.CROSSHAIR;
    public int gridSize = 0;

    public int playerX = 10;
    public int playerY = 30;

    public int opponentX = 80;
    public int opponentY = 30;

    public int opponentNameX = 80;
    public int opponentNameY = 16;

    public int crosshairYOffset = 18;

    public static ElyrecodeConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader r = new FileReader(CONFIG_FILE)) {
                ElyrecodeConfig cfg = GSON.fromJson(r, ElyrecodeConfig.class);
                if (cfg != null) {
                    if (cfg.nameMode == null) {
                        cfg.nameMode = NameMode.CROSSHAIR;
                    }
                    return cfg;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ElyrecodeConfig cfg = new ElyrecodeConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try (FileWriter w = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
