package com.teammoeg.frostedheart.world.feature;

import com.cannolicatfish.rankine.init.RankineBlocks;
import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.loot.LootTables;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorldWriter;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import org.apache.logging.log4j.Level;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpacecraftFeature extends Feature<NoFeatureConfig> {
    public Rotation rotation;
    public SpacecraftFeature(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, NoFeatureConfig config) {
        ChunkPos chunkpos = new ChunkPos(pos);
        this.rotation = Rotation.randomRotation(rand);

        List<Integer> list = IntStream.rangeClosed(chunkpos.getXStart(), chunkpos.getXEnd()).boxed().collect(Collectors.toList());
        Collections.shuffle(list, rand);
        List<Integer> list1 = IntStream.rangeClosed(chunkpos.getZStart(), chunkpos.getZEnd()).boxed().collect(Collectors.toList());
        Collections.shuffle(list1, rand);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(Integer integer : list) {
            for(Integer integer1 : list1) {
                blockpos$mutable.setPos(integer, 0, integer1);
                BlockPos blockpos = reader.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, blockpos$mutable);
                if (reader.isAirBlock(blockpos) || reader.getBlockState(blockpos).getCollisionShapeUncached(reader, blockpos).isEmpty()) {

                    this.fillWithBlocks(reader,blockpos.add(-3,-1,-3),1, 0, 1, 5, 0, 5, RankineBlocks.DIORITE_BRICKS.get().getDefaultState());

                    reader.setBlockState(blockpos, FHContent.FHBlocks.relic_chest.getDefaultState(), 2);

                    LockableLootTileEntity.setLootTable(reader, rand, blockpos, new ResourceLocation(FHMain.MODID,"chest/spacecraft"));

                    FHMain.LOGGER.log(Level.DEBUG, "spacecraft at " + (blockpos.getX()) + " " + blockpos.getY() + " " + (blockpos.getZ()));
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    protected void setBlockState(IWorldWriter world, BlockPos pos, BlockState state) {
        if (this.rotation != Rotation.NONE) {
            state = state.rotate(this.rotation);
        }
        world.setBlockState(pos, state, 2);
    }
    public void fillWithBlocks(ISeedReader worldIn,BlockPos centre ,int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, BlockState BlockState) {
        for(int i = yMin; i <= yMax; ++i) {
            for(int j = xMin; j <= xMax; ++j) {
                for(int k = zMin; k <= zMax; ++k) {
                            this.setBlockState(worldIn,centre.add(j, i, k),BlockState);
                        }
                }
            }
        }
}
