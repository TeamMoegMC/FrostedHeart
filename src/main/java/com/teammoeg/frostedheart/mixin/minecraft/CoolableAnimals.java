package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SheepEntity.class, BeeEntity.class, MooshroomEntity.class, PigEntity.class, RabbitEntity.class})
public class CoolableAnimals extends MobEntity {
    short hxteTimer;

    protected CoolableAnimals(EntityType<? extends MobEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$writeAdditional(CompoundNBT compound, CallbackInfo cbi) {
        compound.putShort("hxthermia", hxteTimer);

    }

    @Inject(at = @At("HEAD"), method = "writeAdditional")
    public void fh$readAdditional(CompoundNBT compound, CallbackInfo cbi) {
        hxteTimer = compound.getShort("hxthermia");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isRemote) {
            float temp = ChunkData.getTemperature(this.getEntityWorld(), this.getPosition());
            if (temp < WorldClimate.HEMP_GROW_TEMPERATURE || temp > WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (hxteTimer < 100) {
                    hxteTimer++;
                } else {
                    hxteTimer = 0;
                    this.attackEntityFrom(temp > 0 ? FHDamageSources.HYPERTHERMIA : FHDamageSources.HYPOTHERMIA, 2);
                }
            } else if (hxteTimer > 0)
                hxteTimer--;
        }
    }
}
