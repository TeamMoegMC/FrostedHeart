package com.teammoeg.frostedheart.content.tips;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.category.Category;
import com.teammoeg.chorda.client.cui.contentpanel.Alignment;
import com.teammoeg.chorda.client.cui.contentpanel.Line;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.chorda.client.cui.editor.EditListDialog;
import com.teammoeg.chorda.client.cui.editor.EditUtils;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder;
import com.teammoeg.chorda.client.cui.editor.Editors;
import com.teammoeg.chorda.client.cui.theme.Coloring;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.theme.UIColors;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.content.archive.ArchiveCategory;
import com.teammoeg.frostedheart.infrastructure.command.TipClientCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.teammoeg.frostedheart.content.tips.Tip.GSON;
import static com.teammoeg.frostedheart.content.tips.Tip.LOGGER;

public class TipHelper {
    static final Editor<Tip.Display> DISPLAY_EDITOR = EditorDialogBuilder.create(b -> b
            .add(Editors.openDialog(Editors.STACK_LIST).withName("Display Items").forGetter(Tip.Display::displayItems))
            .add(Editors.INT.withName(Component.literal("Display Time (ms)")).forGetter(Tip.Display::displayTime))
            .add(Editors.INT.withName(Component.literal("Text Color")).forGetter(Tip.Display::fontColor))
            .add(Editors.INT.withName(Component.literal("Background Color")).forGetter(Tip.Display::backgroundColor))
            .add(Editors.BOOLEAN.withName(Component.literal("Always Visible")).forGetter(Tip.Display::alwaysVisible))
            .add(Editors.BOOLEAN.withName(Component.literal("Once Only")).forGetter(Tip.Display::onceOnly))
            .add(Editors.BOOLEAN.withName(Component.literal("Hide in Archive")).forGetter(Tip.Display::hide))
            .add(Editors.BOOLEAN.withName(Component.literal("Top")).forGetter(Tip.Display::pin))
            .apply(Tip.Display::new)
    );

    public static final Editor<Tip> EDITOR = EditorDialogBuilder.create(b -> b
                    .add(Editors.STRING_ID.withName(Component.literal("ID")).forGetter(Tip::id))
                    .add(Editors.openDialog(EditListDialog.STRING_LIST).withName(Component.literal("Contents")).forGetter(Tip::contents))
                    .add(Editors.STRING.withName(Component.literal("Category")).forGetter(Tip::category))
                    .add(Editors.RESOURCELOCATION.withName(Component.literal("Image Location")).forGetter(Tip::image))
                    .add(Editors.STRING.withName(Component.literal("Next Tip")).forGetter(Tip::nextTip))
                    .add(Editors.openDialog(EditListDialog.STRING_LIST).withName(Component.literal("Unlocks")).forGetter(Tip::unlocks))
                    .add(Editors.openDialog(EditListDialog.STRING_LIST).withName(Component.literal("Children")).forGetter(Tip::children))
                    .add(Editors.openDialog(ClickActions.EDITOR).withName("Jump Button Click Action").forGetter(Tip::clickAction))
                    .add(Editors.openDialog(DISPLAY_EDITOR).withDefault(() -> Tip.Display.DEFAULT).withName(Component.literal("Edit Display")).forGetter(Tip::display))
                    .apply(Tip::new)
    );

    public static void edit(String id, @Nullable Theme theme) {
        if (TipManager.INSTANCE.hasTip(id)) {
            edit(TipManager.INSTANCE.getTip(id), theme);
        } else {
            edit(Tip.builder(id==null?randomString():id).build(), theme);
        }
    }

    /**
     * 编辑已存在的 Tip 优先使用 ID
     * @see #edit(String, Theme)
     */
    public static void edit(@Nullable Tip edit, @Nullable Theme theme) {
        Tip e = edit == null ? Tip.builder(randomString()).build() : edit;
        EDITOR.open(EditUtils.openEditorScreen(theme), Component.literal("Edit Tip"), e, t -> {
            if (!e.equals(t)) {
                save(t);
            }
            TipManager.display().removeCurrent();
        });
    }

    public static boolean save(Tip tip) {
        var errors = new ArrayList<String>();
        if (isTipIdInvalid(tip.id()))
            errors.add("tips.frostedheart.error.invalid_id");
        if (tip.id().isBlank())
            errors.add("tips.frostedheart.error.load.no_id");
        if (tip.contents().isEmpty())
            errors.add("tips.frostedheart.error.load.empty");
        if (tip.nextTip().equals(tip.id()))
            errors.add("Next tip cannot be self");
        if (tip.children().stream().anyMatch(c -> c.equals(tip.id())))
            errors.add("Children list contains self");
        if (tip.display().displayTime() < 0)
            errors.add("Display time cannot less than 0. If you want to make it always visible, enable alwaysVisible instead");
        if (tip.display().displayItems().stream().anyMatch(ItemStack::isEmpty))
            errors.add("Illegal item(s)");
        if (!errors.isEmpty()) {
            display(Tip.builder(randomString()).contents(Error.SAVE.desc()).contents(errors).fontColor(Colors.RED).pin().alwaysVisible().temporary().clickAction("edit_tip", toString(tip)).build());
            return false;
        }

        var warns = new ArrayList<String>();
        if (tip.category().isBlank())
            warns.add("No category");
        if (!tip.nextTip().isBlank() && !TipManager.INSTANCE.hasTip(tip.nextTip()))
            warns.add("The given next tip not exists");
        if (hasClickAction(tip) && !ClickActions.hasAction(tip.clickAction().action()))
            warns.add("Action '" + tip.clickAction().action() + "' not exists");
        if (!warns.isEmpty())
            display(Tip.builder(randomString()).contents("Warning").contents(warns).fontColor(Colors.ChatColors.GOLD).pin().alwaysVisible().temporary().clickAction("edit_tip", toString(tip)).build());

        File file = getTipPath(tip);
        try {
            FileUtil.transfer(toString(tip), file);
            String message = "Saved tip '%s' to %s".formatted(tip.id(), file);
            LOGGER.info(message);
            Popup.put(message);
        } catch (Exception e) {
            LOGGER.error("Unable to save tip: '{}'", file, e);
            Popup.put(Component.translatable("tips.frostedheart.error.save").withStyle(ChatFormatting.RED));
            return false;
        }
        TipManager.INSTANCE.loadFromFile();
        return true;
    }

    public static boolean delete(Tip tip) {
        var path = getTipPath(tip);
        if (!path.exists()) {
            TipHelper.Error.OTHER.createAndDisplay("tips.frostedheart.error.load.file_not_exists", path.getPath().replace("\\", "\\\\"));
            return false;
        }
        if (path.delete()) {
            Popup.put("Deleted tip '" + tip.id() + "'");
            return true;
        }
        TipHelper.Error.OTHER.createAndDisplay("Unable to delete file: " + path);
        return false;
    }

    public static File getTipPath(Tip tip) {
        return new File(TipManager.TIP_PATH, tip.id() + ".json");
    }

    public static String toString(Tip tip) {
        return GSON.toJson(Tip.CODEC.encodeStart(JsonOps.INSTANCE, tip).getOrThrow(true, LOGGER::error));
    }

    public static Tip load(File filePath) {
        LOGGER.debug("Loading tip '{}'", filePath.getName());
        if (filePath.exists()) {
            try {
                return parse(FileUtil.readString(filePath));
            } catch (Exception e) {
                LOGGER.error("Unable to load file '{}'", filePath, e);
                return Error.LOAD.create(filePath.toString().replace("\\", "\\\\"), e.getMessage()).build();
            }
        } else {
            LOGGER.error("File does not exist '{}'", filePath);
            return Error.LOAD.create("tips.frostedheart.error.load.file_not_exists", filePath.toString().replace("\\", "\\\\")).build();
        }
    }

    public static Tip parse(String jsonString) {
        try {
            return parse(GSON.fromJson(jsonString, JsonElement.class).getAsJsonObject());
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.warn("Unable to parse json: {}", jsonString, e);
            return Error.LOAD.create("tips.frostedheart.error.load.invalid_json", jsonString, e.getMessage()).build();
        }
    }

    public static Tip parse(JsonObject json) {
        return Tip.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(LOGGER::error).orElse(Error.LOAD.create("tips.frostedheart.error.load.invalid_json", json.toString()).build());
    }

    public static Tip parse(CompoundTag nbt) {
        return Tip.CODEC.parse(NbtOps.INSTANCE, nbt).resultOrPartial(LOGGER::error).orElse(Error.LOAD.create("tips.frostedheart.error.load.invalid_nbt", nbt.toString()).build());
    }

    public enum Error {
        LOAD,
        SAVE,
        DISPLAY,
        OTHER;

        public String desc() {
            return "tips.frostedheart.error." + this.name().toLowerCase(Locale.ROOT);
        }

        public Tip.Builder create() {
            return Tip.builder("/error/" + randomString())
                    .fontColor(Colors.RED)
                    .alwaysVisible()
                    .hide()
                    .pin()
                    .temporary();
        }

        public Tip.Builder create(String... messages) {
            return create(List.of(messages));
        }

        public Tip.Builder create(Collection<String> messages) {
            return Tip.builder("/error/" + randomString())
                    .contents(desc())
                    .contents(messages)
                    .contents("tips.frostedheart.error.desc")
                    .fontColor(Colors.RED)
                    .alwaysVisible()
                    .hide()
                    .pin()
                    .temporary();
        }

        public Tip createAndDisplay(String... messages) {
            Tip tip = create(messages).build();
            display(tip);
            return tip;
        }
    }

    public static void display(Tip tip) {
        TipManager.display().general(tip);
    }

    public static void display(Tip tip, ServerPlayer player) {
        ServerTipHelper.sendCustom(tip, player);
    }

    public static boolean hasNext(Tip tip) {
        return tip != null && tip.nextTip() != null && !tip.nextTip().isBlank();
    }

    public static boolean hasClickAction(Tip tip) {
        return tip != null && tip.clickAction() != ClickActions.NO_ACTION;
    }

    public static boolean hasImage(Tip tip) {
        return tip.image() != null && !tip.image().getPath().isBlank();
    }

    public static void runClickAction(Tip tip) {
        if (hasClickAction(tip)) {
            tip.clickAction().run();
        }
    }

    public static boolean isTipIdInvalid(String id) {
        return id.matches(FileUtil.ILLEGAL_CHARS_REGEX);
    }

    public static Category getCategory(UILayer parent) {
        Category category = new Category(parent, Component.translatable("gui.frostedheart.archive.category.tips"));
        List<Tip> unlockedTips = TipManager.INSTANCE.getUnlockedTips();

        // 1. 收集所有子提示的 ID
        Set<String> childTipIds = unlockedTips.stream()
                .flatMap(tip -> tip.children().stream())
                .collect(Collectors.toSet());

        // 2. 准备子分类映射和主分类条目列表
        Map<String, Category> subTipCategory = new HashMap<>();
        List<ArchiveCategory.TipEntry> mainCategoryEntries = new ArrayList<>();

        // 3. 再次遍历，填充分类和条目
        for (Tip tip : unlockedTips) {
            // 跳过隐藏的提示
            if (tip.display().hide()) continue;
            // 跳过子提示和空提示
            if (tip.contents().isEmpty() || childTipIds.contains(tip.id())) continue;

            String categoryName = tip.category();
            if (!categoryName.isBlank()) {
                // 获取或创建子分类
                Category subCategory = subTipCategory.computeIfAbsent(categoryName,
                        name -> new Category(category, Component.translatable(name)));
                subCategory.add(new ArchiveCategory.TipEntry(subCategory, tip.id()));
            } else {
                // 无分类的提示放入主分类
                mainCategoryEntries.add(new ArchiveCategory.TipEntry(category, tip.id()));
            }
        }

        // 4. 将所有主分类条目添加到主分类
        category.addAll(mainCategoryEntries);
        return category;
    }

    public static String getTitle(Tip tip) {
        return tip.contents().isEmpty() ? tip.id() : tip.contents().get(0);
    }

    public static List<Line<?>> contentLines(Tip tipInput,UIElement parent) {
        List<Line<?>> lines = new ArrayList<>();
        List<Tip> tips = new ArrayList<>();
        tips.add(tipInput);
        tips.addAll(TipManager.state().getChildren(tipInput));

        for (int j = 0; j < tips.size(); j++) {
            final Tip tip = tips.get(j);
            if (tip.display().hide()) continue;
            var tipContents = tip.contents();
            int ocolor = Colors.cyanToTheme(tip.display().fontColor());
            Coloring color=Coloring.argb(ocolor);
            Coloring readable=Coloring.argb(Colors.readableColor(ocolor));
            // title
            if (j == 0) {
                lines.add(LineHelper.text(parent, Component.translatable(tipContents.get(0))).color(UIColors.UI_ALT_TEXT).scale(2).button(tip.clickAction()));
                // new tip notification
            } else if (!TipManager.state().isViewed(tip)) {
                lines.add(LineHelper.br(parent));
                lines.add(LineHelper.text(parent, Component.translatable("gui.frostedheart.archive.new_tip")).title(color, 1).color(readable));
            }
            // if child tip has different title or has click action
            if (j != 0 && (TipHelper.hasClickAction(tip) || !tipContents.get(0).equals(tipInput.contents().get(0)))) {
                lines.add(LineHelper.space(parent));
                lines.add(LineHelper.text(parent, Component.translatable(tipContents.get(0))).quote(color).button(tip.clickAction()));
            }
            // lines
            for (int i = 1; i < tipContents.size(); i++) {
                String text = tipContents.get(i);
                if (!text.isBlank()) {
                    lines.add(LineHelper.text(parent, Component.translatable(text)));
                }
            }
            // items
            if (!tip.display().displayItems().isEmpty()) {
                lines.add(LineHelper.items(parent, tip.display().displayItems()));
            }
            // image
            if (TipHelper.hasImage(tip)) {
                var img = LineHelper.img(parent, tip.image());
                lines.add(img);
            }
            // debug
            if (TipClientCommand.editMode) {
                lines.add(LineHelper.text(parent, "ID: " + tip.id())
                        .color(UIColors.UI_BORDER)
                        .alignment(Alignment.RIGHT)
                        .button(Component.translatable("controls.reset"), FlatIcon.HISTORY, b -> TipManager.state().reset(tip))
                        .button(Component.translatable("selectServer.edit"), FlatIcon.WRENCH, b -> TipHelper.edit(tip.id(), parent.getLayerHolder().theme()))
                );
                lines.add(LineHelper.space(parent));
            }
        }
        return lines;
    }

    public static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        TipManager.INSTANCE.getAllTips().forEach(tip ->
                builder.suggest(tip.id(), Component.translatable(getTitle(tip)))
        );
        return builder.buildFuture();
    }

    public static String randomString() {
        return Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }
}
