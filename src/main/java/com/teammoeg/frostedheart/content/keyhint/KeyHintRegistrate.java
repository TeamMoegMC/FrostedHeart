package com.teammoeg.frostedheart.content.keyhint;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import static com.teammoeg.frostedheart.content.keyhint.KeyHintOverlay.keyMappingHint;
import static com.teammoeg.frostedheart.content.keyhint.KeyHintOverlay.registerTrigger;

public class KeyHintRegistrate {

    public static void init() {
        registerTrigger(FHMain.rl("drink_world_water"), KeyHintOverlay.TriggerType.LOOKING_AT_BLOCK, r -> {
            if (ClientUtils.getMc().hitResult instanceof BlockHitResult bhr) {
                var pos = bhr.getBlockPos().relative(bhr.getDirection());
                if (ClientUtils.getWorld().getFluidState(pos).is(Fluids.WATER)) {
                    return List.of(keyMappingHint(FHKeyMappings.key_drink.get()));
                }
            }
            return List.of();
        });
    }
}
