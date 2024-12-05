package com.teammoeg.frostedheart.content.tips.client;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.TipLockManager;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TipElement implements Cloneable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public List<Component> contents = new ArrayList<>();
    public String ID = "";
    public String next = "";
    public boolean alwaysVisible = false;
    public boolean onceOnly = false;
    public boolean hide = false;
    public boolean history = false;
    public int visibleTime = 30000;
    public int fontColor = FHColorHelper.CYAN;
    public int BGColor = 0xFF000000;

    public TipElement() {
    }

    public TipElement(List<Component> contents) {
        this.contents = contents;
    }

    public TipElement(String ID) {
        LOGGER.debug("Loading tip '{}'", ID);
        this.ID = ID;

        File filePath = new File(TipLockManager.TIPS, ID + ".json");
        if (!filePath.exists()) {
            LOGGER.error("File does not exists '{}'", filePath);
            replaceToError(filePath, "not_exists");
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
                    String s = JContents.get(i).toString();
                    if (I18n.exists(s)) {
                        contents.add(Component.translatable(s.substring(1, s.length() - 1)));
                    } else {
                        contents.add(Component.literal(s));
                    }
                }
            }
            if (contents.isEmpty()) {
                LOGGER.error("No contents to display '{}'", filePath);
                replaceToError(filePath, "empty");
                return;
            }

            if (jsonObject.has("fontColor"      )) {fontColor     = Integer.parseUnsignedInt(jsonObject.get("fontColor").getAsString(), 16);}
            if (jsonObject.has("backgroundColor")) {BGColor       = Integer.parseUnsignedInt(jsonObject.get("backgroundColor").getAsString(), 16);}
            if (jsonObject.has("alwaysVisible"  )) {alwaysVisible = jsonObject.get("alwaysVisible").getAsBoolean();}
            if (jsonObject.has("onceOnly"       )) {onceOnly      = jsonObject.get("onceOnly").getAsBoolean();}
            if (jsonObject.has("hide"           )) {hide          = jsonObject.get("hide").getAsBoolean();}
            if (jsonObject.has("visibleTime"    )) {visibleTime   = Math.max(jsonObject.get("visibleTime").getAsInt(), 0);}
            if (jsonObject.has("next"           )) {next          = jsonObject.get("next").getAsString();}
            history = true;

        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid JSON file format '{}'", filePath, e);
            replaceToError(filePath, "invalid");
        } catch (Exception e) {
            LOGGER.error("Unable to load file '{}'", filePath, e);
            replaceToError(filePath, "load");
        }
    }

    public void replaceToError(File filePath, String type) {
        contents.clear();
        contents.add(Lang.translate("tips." + FHMain.MODID + ".error." + type));
        contents.add(Lang.str(filePath.getPath()));
        contents.add(Lang.translate("tips." + FHMain.MODID + ".error.desc"));
        fontColor = FHColorHelper.RED;
        BGColor = FHColorHelper.BLACK;
        alwaysVisible = true;
        onceOnly = false;
        hide = true;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
