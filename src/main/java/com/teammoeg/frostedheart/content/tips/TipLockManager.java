package com.teammoeg.frostedheart.content.tips;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.frostedheart.content.tips.client.TipElement;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class TipLockManager {
    public static final File CONFIG_PATH = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhtips");
    public static final File TIPS = new File(CONFIG_PATH, "tips");
    public static final File UNLOCKED_FILE = new File(CONFIG_PATH, "unlocked_tips.json");
    
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private List<String> visible = new ArrayList<>();
    private List<String> hide = new ArrayList<>();
    private List<List<String>> custom = new ArrayList<>();

    public static final TipLockManager manager = new TipLockManager();
    public static String errorType = "";

    static {
        if (TIPS.mkdir()) {
            LOGGER.info("Config path created");
        }
        manager.loadFromFile();
    }

    private TipLockManager() {
    }

    public void loadFromFile() {
        if (!UNLOCKED_FILE.exists()) {
            createFile();
            return;
        }

        LOGGER.debug("Loading unlocked tips");
        try (FileReader reader = new FileReader(UNLOCKED_FILE)) {
            TipLockManager fileManager = GSON.fromJson(reader, TipLockManager.class);
            this.visible = fileManager.visible;
            this.hide = fileManager.hide;
            this.custom = fileManager.custom;

        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            errorType = "load";
            LOGGER.error("Unable to load file: '{}'", UNLOCKED_FILE);
            createFile();
        }
    }

    public void saveToFile() {
        try (FileWriter writer = new FileWriter(UNLOCKED_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
            errorType = "save";
            LOGGER.error("Unable to save file: '{}'", UNLOCKED_FILE);
        }
    }

    public void createFile() {
        if (UNLOCKED_FILE.exists()) {
            File backupFile = new File(UNLOCKED_FILE + ".bak");
            try {
                Files.copy(UNLOCKED_FILE.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.warn("Old file has been saved as '{}'", backupFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LOGGER.debug("Creating file: '{}'", UNLOCKED_FILE);
        try (FileWriter writer = new FileWriter(UNLOCKED_FILE)) {
            reset();
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getVisible() {
        return visible;
    }

    public List<String> getHide() {
        return hide;
    }

    public List<List<String>> getCustom() {
        return custom;
    }

    public void unlock(String ID, boolean hide) {
        if (isUnlocked(ID)) return;
        if (hide) {
            this.hide.add(ID);
        } else {
            this.visible.add(ID);
        }
        saveToFile();
    }

    public void unlockCustom(TipElement ele) {
        if (isUnlocked(ele.ID)) return;
        List<String> custom = new ArrayList<>();

        custom.add(ele.ID);
        custom.add(Integer.toString(ele.visibleTime));
        custom.add(ele.contents.get(0).getString());
        for (int i = 1; i < ele.contents.size(); i++) {
            custom.add(ele.contents.get(i).getString());
        }

        this.custom.add(custom);
        saveToFile();
    }

    public void removeUnlocked(String ID) {
        this.visible.remove(ID);
        this.hide.remove(ID);
        this.custom.removeIf((l) -> l.get(0).equals(ID));
        saveToFile();
    }

    public boolean isUnlocked(String ID) {
        return visible.contains(ID) || hide.contains(ID) || custom.stream().anyMatch(l -> l.get(0).equals(ID));
    }

    public void reset() {
        this.visible = new ArrayList<>();
        this.hide = new ArrayList<>();
        this.custom = new ArrayList<>();
    }
}
