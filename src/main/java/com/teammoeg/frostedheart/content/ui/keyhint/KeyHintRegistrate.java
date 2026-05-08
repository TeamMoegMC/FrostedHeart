package com.teammoeg.frostedheart.content.ui.keyhint;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.content.utility.seld.ContainerHolderEntity;
import com.teammoeg.frostedheart.content.utility.seld.SledEntity;
import com.teammoeg.frostedheart.content.water.event.WaterClientEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.teammoeg.frostedheart.content.ui.keyhint.KeyHintOverlay.*;

public class KeyHintRegistrate {

    public static void init() {
        registerTrigger(FHMain.rl("drink_world_water"), TriggerType.LOOKING_AT_BLOCK, r -> {
            var pos = r.getBlockPos();
            if (!WaterClientEvents.containsWater((pos))) {
                if (!WaterClientEvents.containsWater(pos.relative(r.getDirection()))) {
                    return List.of();
                }
            }
            return List.of(keyMappingHint(FHKeyMappings.key_drink.get()));
        });
        registerTrigger(FHMain.rl("sled_interact"), TriggerType.LOOKING_AT_ENTITY, e -> {
            if (e.getEntity() instanceof SledEntity sled) {
                var player = ClientUtils.getLocalPlayer();
                var item = player.getMainHandItem();
                if (!sled.hasChest() && ContainerHolderEntity.isValidContainer(item)) {
                    return List.of(customHint(Component.translatable("key.mouse.right"), Component.translatable("hint.frostedheart.sled.put")));
                }
                if (item.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CarpetBlock && sled.getSeatType() == null) {
                    return List.of(customHint(Component.translatable("key.mouse.right"), Component.translatable("hint.frostedheart.sled.put")));
                }
                if (!sled.hasPuller()) {
                    for (Mob mob : ClientUtils.getWorld().getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(5))) {
                        if (mob instanceof Wolf wolf && wolf.isOwnedBy(player) && wolf.getLeashHolder() == player) {
                            return List.of(customHint(Component.translatable("key.mouse.right"), Component.translatable("hint.frostedheart.sled.leash")));
                        }
                    }
                }
            }
            return List.of();
        });
    }
}
