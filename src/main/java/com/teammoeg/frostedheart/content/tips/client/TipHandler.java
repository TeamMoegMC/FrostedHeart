package com.teammoeg.frostedheart.content.tips.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.util.AnimationUtil;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TipHandler {
    public static final File CONFIG_PATH = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhtips");
    public static final File UNLOCKED_FILEPATH = new File(CONFIG_PATH, "unlocked_tips.json");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, TipElement> CACHE = new HashMap<>();

    private static JsonObject unlockedTipFile;
    private static List<String> unlockedTips = new ArrayList<>();
    public static boolean readError = false;

    public static void addToRenderQueue(String ID, boolean first) {
        if (ID.isEmpty()) return;

        for (TipElement ele : RenderHUD.renderQueue) {
            if (ele.ID.equals(ID)) {
                return;
            }
        }

        if (ID.charAt(0) == '_') {
            if (TipHandler.isUnlocked(ID)) {
                return;
            }
        }

        addToRenderQueue(getTipEle(ID), first);
    }

    public static void addToRenderQueue(TipElement element, boolean first) {
        if (!element.hide) {
            TipHandler.unlockOrRemove(element.ID, false);
        }

        if (first) {
            RenderHUD.renderQueue.add(0, element);
        } else {
            RenderHUD.renderQueue.add(element);
        }
    }

    public static void forceAdd(String ID, boolean first) {
        if (first) {
            RenderHUD.renderQueue.add(0, getTipEle(ID));
        } else {
            RenderHUD.renderQueue.add(getTipEle(ID));
        }
    }

    public static void removeCurrent() {
        RenderHUD.renderQueue.remove(0);
        resetTipAnimation();
        RenderHUD.currentTip = null;
    }

    public static void pinTip(String ID) {
        for (int i = 0; i < RenderHUD.renderQueue.size(); i++) {
            TipElement ele = RenderHUD.renderQueue.get(i);
            if (ele.ID.equals(ID)) {
                try {
                    TipElement clone = (TipElement)ele.clone();
                    clone.alwaysVisible = true;
                    RenderHUD.renderQueue.set(i, clone);
                    break;
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    public static void moveToFirst(String ID) {
        if (RenderHUD.renderQueue.size() <= 1 || RenderHUD.renderQueue.get(0).ID.equals(ID)) {
            return;
        }
        for (int i = 0; i < RenderHUD.renderQueue.size(); i++) {
            TipElement ele = RenderHUD.renderQueue.get(i);
            if (ele.ID.equals(ID)) {
                RenderHUD.renderQueue.remove(i);
                RenderHUD.renderQueue.add(0, ele);
                RenderHUD.currentTip = null;
                resetTipAnimation();
                return;
            }
        }
    }

    public static boolean isUnlocked(String ID) {
        if (unlockedTipFile == null) {
            if (!loadUnlockedFromFile()) {
                return false;
            }
        }
        return unlockedTips.contains(ID);
    }

    public static TipElement getTipEle(String ID) {
        if (CACHE.containsKey(ID)) {
            return CACHE.get(ID);
        } else {
            TipElement newElement = new TipElement(ID);
            CACHE.put(ID, newElement);
            return newElement;
        }
    }

    public static List<String> getUnlockedTips() {
        return new ArrayList<>(unlockedTips);
    }

    public static boolean loadUnlockedFromFile() {
        if (!UNLOCKED_FILEPATH.exists()) {
            CONFIG_PATH.mkdirs();
            try(FileWriter writer = new FileWriter(UNLOCKED_FILEPATH)) {
                writer.write("{\"unlocked\":[]}");

            } catch (IOException e) {
                LOGGER.error("Unable to create file: '" + UNLOCKED_FILEPATH + "'");
                readError = true;
                return false;
            }
        }

        LOGGER.debug("Loading unlocked tips");
        try {
            String content = new String(Files.readAllBytes(Paths.get(String.valueOf(UNLOCKED_FILEPATH))));
            unlockedTipFile = GSON.fromJson(content, JsonElement.class).getAsJsonObject();
            unlockedTips = GSON.fromJson(unlockedTipFile.getAsJsonArray("unlocked"), new TypeToken<List<String>>() {}.getType());
            return true;

        } catch (IOException e) {
            LOGGER.error("Unable to load file: '" + UNLOCKED_FILEPATH + "'");
            readError = true;
            return false;

        } catch (IllegalStateException | JsonSyntaxException e) {
            int bak = 1;
            File newName = new File(CONFIG_PATH, "unlocked_tips.backup" + bak + ".json");
            while (newName.exists() && bak++ <= 4) {
                newName = new File(CONFIG_PATH, "unlocked_tips.backup" + bak + ".json");
            }
            UNLOCKED_FILEPATH.renameTo(newName);
            LOGGER.warn("File corrupted, trying to recreate file: '" + UNLOCKED_FILEPATH);
            LOGGER.warn("Old file has been renamed to '" + newName.getName() + "'");
            return false;
        }
    }

    public static void saveUnlockedToFile() {
        if (unlockedTipFile == null || unlockedTipFile.size() == 0) {
            return;
        }

        LOGGER.debug("Saving unlocked tips");
        try(FileWriter writer = new FileWriter(UNLOCKED_FILEPATH)) {
            String content = GSON.toJson(unlockedTipFile);
            writer.write(content);
            LOGGER.debug("Unlocked tips saved");

        } catch (IOException e) {
            LOGGER.error("Unable to save file: '" + UNLOCKED_FILEPATH + "'");

            TipElement ele = new TipElement();
            ele.replaceToError(UNLOCKED_FILEPATH, "save");
            ele.contents.add(new TranslationTextComponent("tips." + FHMain.MODID + ".error.save_desc"));

            addToRenderQueue(ele, true);
        }
    }

    public static void unlockOrRemove(String ID, boolean remove) {
        if (unlockedTipFile == null) {
            if (!loadUnlockedFromFile()) {
                return;
            }
        }

        JsonArray array = unlockedTipFile.getAsJsonArray("unlocked");
        if (!unlockedTips.contains(ID)) {
            unlockedTips.add(ID);
            array.add(ID);

        } else if (remove) {
            unlockedTips.remove(ID);
            for (int i = 0; i < array.size(); i++) {
                String s = array.get(i).toString();
                if (s.length() > 2) {
                    s = s.substring(1, s.length()-1);
                }
                if (s.equals(ID)) {
                    array.remove(i);
                    break;
                }
            }
        } else {
            return;
        }
        JsonObject newObj = new JsonObject();
        newObj.add("unlocked", array);
        unlockedTipFile = newObj;
        saveUnlockedToFile();
    }

    public static void resetTipAnimation() {
        AnimationUtil.removeAnimation("TipFadeIn");
        AnimationUtil.removeAnimation("TipFadeOut");
        AnimationUtil.removeAnimation("TipVisibleTime");
    }

    public static void clearRenderQueue() {
        RenderHUD.renderQueue.clear();
        resetTipAnimation();
        RenderHUD.currentTip = null;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
