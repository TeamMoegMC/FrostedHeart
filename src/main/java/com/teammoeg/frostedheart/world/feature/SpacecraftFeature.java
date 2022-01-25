package com.teammoeg.frostedheart.world.feature;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.state.properties.StructureMode;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorldWriter;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import org.apache.logging.log4j.Level;

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

        PlacementSettings settings = (new PlacementSettings()).setRotation(Rotation.randomRotation(rand)).setMirror(Mirror.NONE);
        Template template = reader.getWorld().getStructureTemplateManager().getTemplate(new ResourceLocation(FHMain.MODID,"relic/spacecraft"));

        List<Integer> list = IntStream.rangeClosed(chunkpos.getXStart(), chunkpos.getXEnd()).boxed().collect(Collectors.toList());
        Collections.shuffle(list, rand);
        List<Integer> list1 = IntStream.rangeClosed(chunkpos.getZStart(), chunkpos.getZEnd()).boxed().collect(Collectors.toList());
        Collections.shuffle(list1, rand);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(Integer integer : list) {
            for(Integer integer1 : list1) {
                blockpos$mutable.setPos(integer, 0, integer1);
//                BlockPos centre = reader.getHeight(Heightmap.Type.WORLD_SURFACE_WG, blockpos$mutable);
                BlockPos centre = new BlockPos(blockpos$mutable.getX(),generator.getHeight(blockpos$mutable.getX(), blockpos$mutable.getZ(), Heightmap.Type.WORLD_SURFACE_WG),blockpos$mutable.getZ());

                if (reader.isAirBlock(centre) || reader.getBlockState(centre).getCollisionShapeUncached(reader, centre).isEmpty()) {

//                    this.fillWithBlocks(reader,point,1,0,1,5,5,5,Blocks.AIR.getDefaultState());
//                    this.fillWithBlocks(reader,point,1, 0, 1, 5, 0, 5, RankineBlocks.DIORITE_BRICKS.get().getDefaultState());

                    if (template.func_237146_a_(reader, centre, centre, settings, reader.getRandom(), 2)) {

//                        FHMain.LOGGER.log(Level.DEBUG, "spacecraft at " + (centre.getX()) + " " + centre.getY() + " " + (centre.getZ()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

//    @Override
//    protected void setBlockState(IWorldWriter world, BlockPos pos, BlockState state) {
//        world.setBlockState(pos, state, 2);
//    }

//    public void fillWithBlocks(ISeedReader reader,BlockPos pos ,int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, BlockState BlockState) {
//        for(int i = yMin; i <= yMax; ++i) {
//            for(int j = xMin; j <= xMax; ++j) {
//                for(int k = zMin; k <= zMax; ++k) {
//                    this.setBlockState(reader,pos.add(j, i, k),BlockState);
//                }
//            }
//        }
//    }
}
