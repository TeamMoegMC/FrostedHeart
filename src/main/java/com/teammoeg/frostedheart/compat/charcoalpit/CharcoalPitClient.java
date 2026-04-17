package com.teammoeg.frostedheart.compat.charcoalpit;

import charcoalPit.block.BlockBloomery;
import charcoalPit.block.BlockLogPile;
import charcoalPit.block.BlockMainBloomery;
import charcoalPit.block.BlockPotteryKiln;
import charcoalPit.core.KeyBindings;
import charcoalPit.recipe.BloomeryRecipe;
import charcoalPit.recipe.PotteryKilnRecipe;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;

import java.util.List;

import static com.teammoeg.frostedheart.content.keyhint.KeyHintOverlay.*;

public class CharcoalPitClient {

    public static void init() {
        registerTrigger(FHMain.rl("charcoal_pit_kilnable"), TriggerType.HOLDING_ITEM_MAINHAND, s -> {
            var manager = ClientUtils.getWorld().getRecipeManager();
            var container = new SimpleContainer(s);
            if (manager.getRecipeFor(PotteryKilnRecipe.POTTERY_RECIPE, container, ClientUtils.getWorld()).isPresent()) {
                return List.of(keyMappingHint(KeyBindings.PLACE_KILN));
            }
            return List.of();
        });

        registerTrigger(FHMain.rl("charcoal_pit_kiln"), TriggerType.LOOKING_AT_BLOCK, b -> {
            var state = ClientUtils.getWorld().getBlockState(b.getBlockPos());
            if (state.getBlock() instanceof BlockPotteryKiln) {
                CustomHint hint = null;
                var type = state.getValue(BlockPotteryKiln.TYPE);
                hint = switch (type) {
                    case EMPTY -> customHint(Component.translatable("hint.frostedheart.charcoal_pit.rclick_with_straws"), Component.translatable("hint.frostedheart.charcoal_pit.put_straws"));
                    case THATCH -> customHint(Component.translatable("hint.frostedheart.charcoal_pit.rclick_with_logs"), Component.translatable("hint.frostedheart.charcoal_pit.put_logs"));
                    case WOOD -> customHint(Component.translatable("hint.frostedheart.charcoal_pit.rclick_with_igniter"), Component.translatable("hint.frostedheart.charcoal_pit.ignite"));
                    default -> hint;
                };
                if (hint != null)
                    return List.of(hint);
            } else if (state.getBlock() instanceof BlockLogPile) {
                return List.of(customHint(Component.translatable("hint.frostedheart.charcoal_pit.rclick_with_igniter"), Component.translatable("hint.frostedheart.charcoal_pit.ignite")));
            }
            return List.of();
        });

        registerTrigger(FHMain.rl("charcoal_pit_bloomery"), TriggerType.LOOKING_AT_BLOCK, b -> {
            if (b.getDirection() == Direction.UP) {
                var state = ClientUtils.getWorld().getBlockState(b.getBlockPos());
                var item = ClientUtils.getLocalPlayer().getMainHandItem();
                if (!(state.getBlock() instanceof BlockBloomery && item.is(TagKey.create(Registries.ITEM, new ResourceLocation("charcoal_pit:orekiln_fuels"))))) {
                    if (!((state.getBlock() instanceof BlockMainBloomery || state.getBlock() instanceof BlockBloomery) && BloomeryRecipe.getRecipe(item, ClientUtils.getWorld()) != null)) {
                        if (state.getBlock() instanceof BlockBloomery && state.getValue(BlockBloomery.STAGE) == 8) {
                            return List.of(customHint(Component.translatable("hint.frostedheart.charcoal_pit.rclick_with_igniter"), Component.translatable("hint.frostedheart.charcoal_pit.ignite")));
                        } else {
                            return List.of();
                        }
                    }
                }
                var a = customHint(Component.translatable("hint.frostedheart.crouch_and_rclick"), Component.translatable("hint.frostedheart.charcoal_pit.put_ingredient"));
                if (state.getBlock() instanceof BlockBloomery && state.getValue(BlockBloomery.STAGE) == 8) {
                    return List.of(a, customHint(Component.translatable("hint.frostedheart.charcoal_pit.rclick_with_igniter"), Component.translatable("hint.frostedheart.charcoal_pit.ignite")));
                }
                return List.of(a);
            }
            return List.of();
        });
    }
}
