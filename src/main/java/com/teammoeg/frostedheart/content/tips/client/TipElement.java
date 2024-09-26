package com.teammoeg.frostedheart.content.tips.client;

import com.google.gson.*;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.network.chat.Component;
import com.teammoeg.frostedheart.util.TranslateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TipElement implements Cloneable {
    private static final Logger LOGGER = LogManager.getLogger();
    public List<Component> contents = new ArrayList<>();
    public String ID = "";
    public boolean alwaysVisible = false;
    public boolean onceOnly = false;
    public boolean hide = false;
    public boolean history = false;
    public int visibleTime = 30000;
    public int fontColor = 0xFFC6FCFF;
    public int BGColor = 0xFF000000;

    public TipElement() {
    }

    public TipElement(List<Component> contents) {
        this.contents = contents;
    }

    public TipElement(String ID) {
        LOGGER.debug("Loading tip '{}'", ID);
        this.ID = ID;

        File filePath = new File(UnlockedTipManager.TIPS, ID + ".json");
        if (!filePath.exists()) {
            LOGGER.error("File does not exists '{}'", filePath);
            replaceToError(filePath, "not_exists");
            contents.add(TranslateUtils.translate("tip." + FHMain.MODID + ".error.desc"));
            return;
        }

        readFromJsonFile(filePath);
    }

    public void readFromJsonFile(File filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(String.valueOf(filePath))));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonElement.class).getAsJsonObject();

            if (jsonObject.has("contents")) {
                JsonArray JContents = jsonObject.getAsJsonArray("contents");
                for (int i = 0; i < JContents.size(); i++) {
                    String raw = JContents.get(i).toString();
                    contents.add(TranslateUtils.translate(raw.substring(1, raw.length() - 1)));
                }
            }
            if (contents.isEmpty()) {
                LOGGER.error("No contents to display '{}'", filePath);
                replaceToError(filePath, "empty");
                return;
            }

            if (jsonObject.has("fontColor"      )) {fontColor = Integer.parseUnsignedInt(jsonObject.get("fontColor").getAsString(), 16);}
            if (jsonObject.has("backgroundColor")) {BGColor = Integer.parseUnsignedInt(jsonObject.get("backgroundColor").getAsString(), 16);}
            if (jsonObject.has("alwaysVisible"  )) {alwaysVisible = jsonObject.get("alwaysVisible").getAsBoolean();}
            if (jsonObject.has("onceOnly"       )) {onceOnly = jsonObject.get("onceOnly").getAsBoolean();}
            if (jsonObject.has("hide"           )) {hide = jsonObject.get("hide").getAsBoolean();}
            if (jsonObject.has("visibleTime"    )) {visibleTime = Math.max(jsonObject.get("visibleTime").getAsInt(), 0);}
            history = true;

        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid JSON file format '{}'", filePath);
            replaceToError(filePath, "invalid");
            contents.add(TranslateUtils.translate("tips." + FHMain.MODID + ".error.desc"));
        } catch (Exception e) {
            LOGGER.error("Unable to load file '{}'", filePath);
            replaceToError(filePath, "load");
        }
    }

    public void replaceToError(File filePath, String type) {
        contents = new ArrayList<>();
        contents.add(TranslateUtils.translate("tips." + FHMain.MODID + ".error." + type));
        contents.add(TranslateUtils.str(filePath.getPath()));
        fontColor = 0xFFFF5340;
        BGColor = 0xFF000000;
        alwaysVisible = true;
        onceOnly = false;
        hide = true;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
