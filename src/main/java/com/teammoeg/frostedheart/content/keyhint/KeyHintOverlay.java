package com.teammoeg.frostedheart.content.keyhint;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.screenadapter.OverlayPositioner;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class KeyHintOverlay extends PrimaryLayer {
    public static final KeyHintOverlay INSTANCE = new KeyHintOverlay();
    private KeyHintOverlay() {}

    private static final List<KeyHint> active = new ArrayList<>();
    private static final List<KeyHint> collected = new ArrayList<>();

    @Override
    public void addChildUIElements() {
        clearElement();
        for (KeyHint key : active) {
            add(new KeyHintEntry(this, key));
        }
    }

    @Override
    public void refresh() {
        addChildUIElements();
        if (elements.isEmpty()) return;
        alignWidgets();
        setSizeToContentSize();
        var pos = OverlayPositioner.position(this, FHConfig.CLIENT.hintPosition.get().startPos(this)); // TODO config
        setOffsetX(pos.getX());
        setOffsetY(pos.getY());
    }

    @Override
    public void alignWidgets() {
        align(6, false);
    }

    @Override
    public void onBeforeRender() {
        super.onBeforeRender();
        if (ClientUtils.getWorld() != null && FHConfig.CLIENT.enableKeyHints.get()) {
            collectKeys(ClientUtils.getLocalPlayer());
            active.clear();
            active.addAll(collected);
            refresh();
        } else if (!active.isEmpty()) {
            active.clear();
            refresh();
        }
    }

    @Override
    public boolean shouldRenderGradient() {
        return false;
    }

    public static CustomHint customHint(Component key, Component hint) {
        return new CustomHint(key, hint);
    }
    public static KeyMappingHint keyMappingHint(KeyMapping key) {
        return new KeyMappingHint(key);
    }
    public static class CustomHint implements KeyHint {
        final Component key;
        final Component Hint;

        public CustomHint(Component key, Component hint) {
            this.key = key;
            Hint = hint;
        }

        @Override
        public Component getKey() {
            return key;
        }

        @Override
        public Component getHint() {
            return Hint;
        }
    }
    public static class KeyMappingHint implements KeyHint {
        final KeyMapping key;

        public KeyMappingHint(KeyMapping key) {
            this.key = key;
        }

        @Override
        public Component getKey() {
            return key.getTranslatedKeyMessage();
        }

        @Override
        public Component getHint() {
            return Component.translatable(key.getName());
        }
    }
    public interface KeyHint {
        Component getKey();
        Component getHint();
    }

    public static void collectKeys(LocalPlayer player) {
        collected.clear();

        // 遍历所有类型
        for (TriggerType<?> type : TriggerType.getAllTypes()) {
            Object data = type.getDataExtractor().apply(player);
            if (data == null) continue;

            // 遍历该类型的所有项目
            for (var entry : type.getRegistered().entrySet()) {
                boolean skip = false;
                // 检查是否被禁用
                for (String s : FHConfig.CLIENT.disabledHints.get()) {
                    if (type.getRegistered().containsKey(new ResourceLocation(s))) {
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;
                // 收集提示
                @SuppressWarnings("unchecked")
                Collection<KeyHint> hints = ((Function<Object, Collection<KeyHint>>) entry.getValue()).apply(data);
                if (hints != null) {
                    collected.addAll(hints);
                }
            }
        }

        MinecraftForge.EVENT_BUS.post(new KeyHintCollectEvent(player, collected));
    }

    public static <T> void registerTrigger(ResourceLocation rl, TriggerType<T> type, Function<T, Collection<KeyHint>> provider) {
        type.registered.put(rl, provider);
    }

    @SuppressWarnings("unused")
    public static class TriggerType<T> {
        private static final Map<ResourceLocation, TriggerType<?>> ALL_TYPES = new LinkedHashMap<>();

        /** 主手物品 */
        public static final TriggerType<ItemStack> HOLDING_ITEM_MAINHAND = registerType(
                FHMain.rl("holding_item_mainhand"),
                player -> {
                    var item = player.getMainHandItem();
                    return item.isEmpty() ? null : item;
                }
        );
        /** 副手物品 */
        public static final TriggerType<ItemStack> HOLDING_ITEM_OFFHAND = registerType(
                FHMain.rl("holding_item_offhand"),
                player -> {
                    var item = player.getOffhandItem();
                    return item.isEmpty() ? null : item;
                }
        );
        /** 注视方块 */
        public static final TriggerType<BlockHitResult> LOOKING_AT_BLOCK = registerType(
                FHMain.rl("looking_at_block"),
                player -> {
                    var hit = Minecraft.getInstance().hitResult;
                    return hit instanceof BlockHitResult bhr ? bhr : null;
                }
        );
        /** 注视实体 */
        public static final TriggerType<EntityHitResult> LOOKING_AT_ENTITY = registerType(
                FHMain.rl("looking_at_entity"),
                player -> {
                    var hit = Minecraft.getInstance().hitResult;
                    return hit instanceof EntityHitResult ehr ? ehr : null;
                }
        );
        /** 骑乘实体 */
        public static final TriggerType<Entity> RIDING_ENTITY = registerType(
                FHMain.rl("riding_entity"),
                LocalPlayer::getVehicle
        );
        /** 每时每刻 */
        public static final TriggerType<Minecraft> EVERY_FRAME = registerType(
                FHMain.rl("every_frame"),
                player -> Minecraft.getInstance()
        );

        @Getter
        private final Map<ResourceLocation, Function<T, Collection<KeyHint>>> registered = new LinkedHashMap<>();
        @Getter
        private final ResourceLocation location;
        @Getter
        private final Function<LocalPlayer, T> dataExtractor;

        private TriggerType(ResourceLocation location, Function<LocalPlayer, T> dataExtractor) {
            this.location = location;
            this.dataExtractor = dataExtractor;
        }

        public static <T> TriggerType<T> registerType(ResourceLocation location, Function<LocalPlayer, T> dataExtractor) {
            var newType = new TriggerType<>(location, dataExtractor);
            ALL_TYPES.put(location, newType);
            return newType;
        }

        public static Collection<TriggerType<?>> getAllTypes() {
            return ALL_TYPES.values();
        }
    }

    static class KeyHintEntry extends UIElement {
        final KeyHint key;
        final Component message;

        public KeyHintEntry(UIElement panel, KeyHint key) {
            super(panel);
            this.key = key;
            var m = Component.empty();
            if (!key.getKey().getString().isEmpty())
                m.append(Component.literal("[" + key.getKey().getString() + "]  ").withStyle(ChatFormatting.GOLD));
            if (!key.getHint().getString().isEmpty())
                m.append(key.getHint());
            this.message = m;
            setHeight(8);
            setWidth(getFont().width(message));
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 800);
            graphics.fill(x-2, y-2, x+w+2, y+h+2, Colors.setAlpha(Colors.BLACK, 0.5F));
            graphics.drawString(getFont(), message, x, y, Colors.themeColor());
            graphics.pose().popPose();
        }
    }
}
