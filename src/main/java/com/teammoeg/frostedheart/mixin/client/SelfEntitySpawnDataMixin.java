package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.content.utility.seld.IExtraClientSpawnData;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IExtraClientSpawnData.class)
public interface SelfEntitySpawnDataMixin extends IEntityAdditionalSpawnData {

}
