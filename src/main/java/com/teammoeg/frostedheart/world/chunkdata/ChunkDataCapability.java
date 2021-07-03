/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.chunkdata;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

import com.teammoeg.frostedheart.FHUtil;

public final class ChunkDataCapability
{
    @CapabilityInject(ChunkData.class)
    public static final Capability<ChunkData> CAPABILITY = FHUtil.notNull();
    public static final ResourceLocation KEY = new ResourceLocation(FHMain.MODID, "chunk_data");

    public static void setup()
    {
        FHUtil.registerSimpleCapability(ChunkData.class);
    }


}