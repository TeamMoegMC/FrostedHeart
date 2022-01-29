package com.teammoeg.frostedheart.world.feature;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpacecraftFeature extends Feature<NoFeatureConfig> {
    public SpacecraftFeature(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, NoFeatureConfig config) {
        ChunkPos chunkpos = new ChunkPos(pos.north(16));
        List<Integer> listX = IntStream.rangeClosed(chunkpos.getXStart(), chunkpos.getXEnd()).boxed().collect(Collectors.toList());
        Collections.shuffle(listX, rand);
        List<Integer> listZ = IntStream.rangeClosed(chunkpos.getZStart(), chunkpos.getZEnd()).boxed().collect(Collectors.toList());
        Collections.shuffle(listZ, rand);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(Integer integerX : listX) {
            for(Integer integerZ : listZ) {
                blockpos$mutable.setPos(integerX, generator.getNoiseHeightMinusOne(integerX, integerZ, Heightmap.Type.WORLD_SURFACE_WG), integerZ);

                if (reader.isAirBlock(blockpos$mutable) || reader.getBlockState(blockpos$mutable).getCollisionShapeUncached(reader, blockpos$mutable).isEmpty()) {

                    PlacementSettings settings = (new PlacementSettings()).setRotation(Rotation.randomRotation(rand)).setMirror(Mirror.NONE);
                    Template template = reader.getWorld().getStructureTemplateManager().getTemplate(new ResourceLocation(FHMain.MODID,"relic/spacecraft"));
                    MutableBoundingBox boundingBox = template.getMutableBoundingBox(settings, blockpos$mutable);
                    Vector3i vector3i = boundingBox.func_215126_f();

                    if (template.func_237146_a_(reader, blockpos$mutable, new BlockPos(vector3i.getX(), boundingBox.minY, vector3i.getZ()), settings, reader.getRandom(), 2)) {
//                      FHMain.LOGGER.log(Level.DEBUG, "spacecraft at " + (centre.getX()) + " " + centre.getY() + " " + (centre.getZ()));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
