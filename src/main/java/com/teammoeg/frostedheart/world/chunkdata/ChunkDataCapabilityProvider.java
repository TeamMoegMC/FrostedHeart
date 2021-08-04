/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.chunkdata;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class ChunkDataCapabilityProvider {
    @CapabilityInject(ChunkData.class)
    public static Capability<ChunkData> CAPABILITY;
    public static final ResourceLocation KEY = new ResourceLocation(FHMain.MODID, "chunk_data");

    public static void setup() {
        CapabilityManager.INSTANCE.register(ChunkData.class, new Capability.IStorage<ChunkData>() {
            public INBT writeNBT(Capability<ChunkData> capability, ChunkData instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<ChunkData> capability, ChunkData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, () -> {
            return null;
        });
    }


}