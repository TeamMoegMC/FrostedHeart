package com.teammoeg.frostedheart.content.tips.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import com.teammoeg.frostedheart.content.tips.client.util.AnimationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
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
    private static List<String> unlockedTipsHidden = new ArrayList<>();
    public static boolean readError = false;

    public static void displayTip(String ID, boolean first) {
        displayTip(getTipEle(ID), first);
    }

    public static void displayTip(TipElement element, boolean first) {
        if (element.ID.isEmpty()) return;
        if (element.onceOnly && TipHandler.isUnlocked(element.ID)) return;

        for (TipElement ele : RenderHUD.renderQueue) {
            if (ele.ID.equals(element.ID)) {
                return;
            }
        }

        if (element.fromFile) {
            TipHandler.unlock(element.ID, element.hide);
        }

        if (first) {
            RenderHUD.renderQueue.add(0, element);
        } else {
            RenderHUD.renderQueue.add(element);
        }
    }

    public static void forceAdd(String ID, boolean first) {
        for (TipElement ele : RenderHUD.renderQueue) {
            if (ele.ID.equals(ID)) {
                return;
            }
        }

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
            if (!loadUnlocked()) {
                return false;
            }
        }
        return unlockedTips.contains(ID) || unlockedTipsHidden.contains(ID);
    }

    public static TipElement getTipEle(String ID) {
        if (CACHE.containsKey(ID)) {
            return CACHE.get(ID);
        } else {
            TipElement newElement = new TipElement(ID);
            if (newElement.fromFile) {
                CACHE.put(ID, newElement);
            }
            return newElement;
        }
    }

    public static List<String> getVisibleUnlocked() {
        checkUnlocked();
        return new ArrayList<>(unlockedTips);
    }

    public static List<String> getHiddenUnlocked() {
        checkUnlocked();
        return new ArrayList<>(unlockedTipsHidden);
    }

    public static boolean loadUnlocked() {
        if (!UNLOCKED_FILEPATH.exists()) {
            if (CONFIG_PATH.mkdirs()) {
                LOGGER.info("Path: '{}' created", CONFIG_PATH);
            }
            return resetUnlocked();
        }

        LOGGER.debug("Loading unlocked tips");
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            String content = new String(Files.readAllBytes(Paths.get(String.valueOf(UNLOCKED_FILEPATH))));
            unlockedTipFile = GSON.fromJson(content, JsonElement.class).getAsJsonObject();
            unlockedTips = GSON.fromJson(unlockedTipFile.getAsJsonArray("visible"), type);
            unlockedTipsHidden = GSON.fromJson(unlockedTipFile.getAsJsonArray("hide"), type);

            if (unlockedTips == null || unlockedTipsHidden == null) {
                resetUnlocked();
                return loadUnlocked();
            }
            return true;

        } catch (IllegalStateException | JsonSyntaxException | NullPointerException e) {
            e.printStackTrace();
            return resetUnlocked();

        } catch (IOException e) {
            LOGGER.error("Unable to load file: '{}'", UNLOCKED_FILEPATH);
            readError = true;
            return false;
        }
    }

    public static void saveUnlocked() {
        if (unlockedTipFile.size() == 0 || !checkUnlocked()) {
            return;
        }

        LOGGER.debug("Saving unlocked tips");
        try {
            String content = GSON.toJson(unlockedTipFile);
            FileUtils.writeStringToFile(UNLOCKED_FILEPATH, content, "UTF-8");

        } catch (IOException e) {
            LOGGER.error("Unable to save file: '{}'", UNLOCKED_FILEPATH);

            TipElement ele = new TipElement();
            ele.replaceToError(UNLOCKED_FILEPATH, "save");
            ele.contents.add(new TranslationTextComponent("tips." + FHMain.MODID + ".error.save_desc"));

            displayTip(ele, true);
        }
    }

    public static boolean checkUnlocked() {
        if (unlockedTipFile == null) {
            return loadUnlocked();
        } else {
            return true;
        }
    }

    public static boolean resetUnlocked() {
        try {
            if (UNLOCKED_FILEPATH.exists()) {
                int bak = 1;
                File newPath = new File(CONFIG_PATH, "unlocked_tips.backup" + bak + ".json");
                while (newPath.exists()) {
                    bak++;
                    newPath = new File(CONFIG_PATH, "unlocked_tips.backup" + bak + ".json");
                }

                LOGGER.warn("File corrupted, trying to recreate file: '{}'", UNLOCKED_FILEPATH);
                FileUtils.moveFile(UNLOCKED_FILEPATH, newPath);
                LOGGER.warn("Old file has been renamed to '{}'", newPath.getName());
            }
            FileUtils.writeStringToFile(UNLOCKED_FILEPATH, "{\"visible\":[],\"hide\": []}", "UTF-8");
            return true;

        } catch (IOException e) {
            LOGGER.error("Unable to create file: '{}'", UNLOCKED_FILEPATH);
            readError = true;
            return false;
        }
    }

    public static void unlock(String ID, boolean hide) {
        if (ID.isEmpty() || TipHandler.isUnlocked(ID) || !checkUnlocked()) {
            return;
        }

        JsonArray visible = unlockedTipFile.getAsJsonArray("visible");
        JsonArray hidden = unlockedTipFile.getAsJsonArray("hide");
        if (hide) {
            unlockedTipsHidden.add(ID);
            hidden.add(ID);
        } else {
            unlockedTips.add(ID);
            visible.add(ID);
        }

        JsonObject newObj = new JsonObject();
        newObj.add("visible", visible);
        newObj.add("hide", hidden);
        unlockedTipFile = newObj;
        saveUnlocked();
    }

    public static void removeUnlocked(String ID) {
        if (ID.isEmpty() || TipHandler.isUnlocked(ID) || !checkUnlocked()) {
            return;
        }

        JsonArray visible = unlockedTipFile.getAsJsonArray("visible");
        JsonArray hidden = unlockedTipFile.getAsJsonArray("hide");

        boolean removed = false;
        for (int i = 0; i < visible.size(); i++) {
            String s = visible.get(i).toString();
            s = s.substring(1, s.length()-1);
            if (s.equals(ID)) {
                unlockedTips.remove(ID);
                visible.remove(i);
                removed = true;
                break;
            }
        }

        for (int i = 0; !removed && i < hidden.size(); i++) {
            String s = hidden.get(i).toString();
            s = s.substring(1, s.length()-1);
            if (s.equals(ID)) {
                unlockedTipsHidden.remove(ID);
                hidden.remove(i);
                break;
            }
        }

        JsonObject newObj = new JsonObject();
        newObj.add("visible", visible);
        newObj.add("hide", hidden);
        unlockedTipFile = newObj;
        saveUnlocked();
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

    public static void openDebugScreen() {
        Minecraft.getInstance().displayGuiScreen(new DebugScreen());
    }
}
