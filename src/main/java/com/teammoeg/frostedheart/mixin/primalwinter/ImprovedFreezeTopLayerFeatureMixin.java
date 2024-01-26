package com.teammoeg.frostedheart.mixin.primalwinter;

import java.util.Arrays;
import java.util.Random;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.alcatrazescapee.primalwinter.common.ModBlocks;
import com.alcatrazescapee.primalwinter.world.ImprovedFreezeTopLayerFeature;
import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import snownee.snow.block.ModSnowBlock;

@Mixin(ImprovedFreezeTopLayerFeature.class)
public abstract class ImprovedFreezeTopLayerFeatureMixin extends Feature<NoFeatureConfig>{


	public ImprovedFreezeTopLayerFeatureMixin(Codec<NoFeatureConfig> codec) {
		super(codec);
		// TODO Auto-generated constructor stub
	}

	@Overwrite(remap = false)
	int countExposedFaces(IWorld world, BlockPos pos)    {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BlockPos posAt = pos.offset(direction);
            ChunkPos cpa=new ChunkPos(posAt);
            if(world.chunkExists(cpa.x, cpa.z)) {
	            if (!world.getBlockState(posAt).isSolidSide(world, posAt, direction.getOpposite()))
	            {
	                count++;
	            }
            }else {
            	//count++;
            }
        }
        return count;
    }

	@Shadow(remap = false)
	abstract void extendSkyLights(int[] skyLights, int startX, int startZ);

	/*@Overwrite
	@Override
	public boolean generate(ISeedReader worldIn, ChunkGenerator chunkGenerator, Random rand, BlockPos pos, NoFeatureConfig config) {
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();

		// First, find the highest and lowest exposed y pos in the chunk
		int maxY = 0;
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				int y = worldIn.getHeight(Type.WORLD_SURFACE, pos.getX() + x, pos.getZ() + z);
				if (maxY < y) {
					maxY = y;
				}
			}
		}

		// Then, step downwards, tracking the exposure to sky at each step
		int[] skyLights = new int[16 * 16], prevSkyLights = new int[16 * 16];
		Arrays.fill(prevSkyLights, 7);
		for (int y = maxY; y >= 0; y--) {
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					int skyLight = prevSkyLights[x + 16 * z];
					mutablePos.setPos(pos.getX() + x, y, pos.getZ() + z);
					BlockState state = worldIn.getBlockState(mutablePos);
					if (state.isAir(worldIn, mutablePos)) {
						// Continue sky light downwards
						skyLights[x + 16 * z] = prevSkyLights[x + 16 * z];
						extendSkyLights(skyLights, x, z);
					}
					if (skyLight > 0) {
						placeSnowAndIce(worldIn, mutablePos, state, rand, skyLight);
					}
				}
			}

			// Break early if all possible sky light is gone
			boolean hasSkyLight = false;
			for (int i = 0; i < 16 * 16; i++) {
				if (skyLights[i] > 0) {
					hasSkyLight = true;
					break; // exit checking loop, continue with y loop
				}
			}
			if (!hasSkyLight) {
				break; // exit y loop
			}

			// Copy sky lights into previous and reset current sky lights
			System.arraycopy(skyLights, 0, prevSkyLights, 0, skyLights.length);
			Arrays.fill(skyLights, 0);
		}
		return true;
	}*/

	@Overwrite(remap = false)
	private void placeSnowAndIce(IWorld worldIn, BlockPos pos, BlockState state, Random random, int skyLight) {
		FluidState fluidState = worldIn.getFluidState(pos);
		BlockPos posDown = pos.down();
		BlockState stateDown = worldIn.getBlockState(posDown);

		// First, possibly replace the block below. This may have impacts on being able
		// to add snow on top
		if (state.isAir(worldIn, pos)) {
			Block replacementBlock = ModBlocks.SNOWY_SPECIAL_TERRAIN_BLOCKS.getOrDefault(stateDown.getBlock(), () -> null).get();
			if (replacementBlock != null) {
				BlockState replacementState = replacementBlock.getDefaultState();
				worldIn.setBlockState(posDown, replacementState, 2);
			}
			
		}

		// Then, try and place snow layers / ice at the current location
		if (fluidState.getFluid() == Fluids.WATER && (state.getBlock() instanceof FlowingFluidBlock || state.getMaterial().isReplaceable())) {
			worldIn.setBlockState(pos, Blocks.ICE.getDefaultState(), 2);
			if (!(state.getBlock() instanceof FlowingFluidBlock)) {
				worldIn.getPendingBlockTicks().scheduleTick(pos, Blocks.ICE, 0);
			}else {
				worldIn.setBlockState(pos.up(), Blocks.SNOW.getDefaultState().with(BlockStateProperties.LAYERS_1_8, random.nextInt(3)+1), 2);
			}
		} else if (fluidState.getFluid() == Fluids.LAVA && state.getBlock() instanceof FlowingFluidBlock) {
			worldIn.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState(), 2);
		} else if (Blocks.SNOW.getDefaultState().isValidPosition(worldIn, pos) && state.getMaterial().isReplaceable()) {
			BlockPos cpos=pos;
			//World worldIn, @Nullable PlayerEntity playerIn, Hand handIn, ItemStack stackIn, BlockRayTraceResult rayTraceResultIn
			//BlockItemUseContext buc=new BlockItemUseContext(worldIn, null,Hand.MAIN_HAND ,new ItemStack(Blocks.SNOW_BLOCK.asItem()),BlockRayTraceResult);
			int layers=0;
			if(worldIn.getHeight(Type.WORLD_SURFACE, pos).getY()-1<=pos.getY()) {
				layers=16+skyLight- random.nextInt(3);
				//layers-=countExposedFaces(worldIn, cpos)*2;
			}else {
				layers=skyLight- random.nextInt(3)-countExposedFaces(worldIn, cpos);
			}
			if(state.isIn(BlockTags.LEAVES)||stateDown.isIn(BlockTags.LEAVES))
				while(layers>=16)
					layers-=8;
			if(layers<=0)
				layers=1;
			while(layers>0) {
				BlockPos ccpos=cpos;
				cpos=cpos.up();
				BlockState cstate=worldIn.getBlockState(ccpos);

				int clayers =0;
				if(layers>8) {
					layers-=8;
					clayers=8;
					/*if(layers<=2) {
						clayers-=random.nextInt(3)+1;
						layers=0;
					}*/
				}else {
					clayers=layers;
					if(clayers>=8) {
						clayers--;
					}
					layers=0;
				}
				if(cstate.matchesBlock(Blocks.SNOW)) {
					BlockState upstate=worldIn.getBlockState(cpos);
					if(upstate.matchesBlock(Blocks.SNOW))
						continue;
					int crlayers=cstate.get(BlockStateProperties.LAYERS_1_8);
				
					clayers+=crlayers;
					if(clayers>8) {
						layers+=clayers-8;
						clayers=8;
					}
				}
				if(!cstate.getMaterial().isReplaceable()/*cstate.getBlock()!=Blocks.SNOW&&!cstate.isAir()&&!cstate.getCollisionShape(worldIn, cpos).isEmpty()*/) {
					if(!ModSnowBlock.convert(worldIn, ccpos, cstate, clayers, 4)) {
						layers+=8;
					}
					continue;
				}
				// Special exceptions
				BlockPos posUp = cpos;

				if (state.getBlock() instanceof DoublePlantBlock && worldIn.getBlockState(posUp).getBlock() == state.getBlock()) {
					// Remove the above plant
					worldIn.removeBlock(posUp, false);
				}

				
				worldIn.setBlockState(ccpos, Blocks.SNOW.getDefaultState().with(BlockStateProperties.LAYERS_1_8, clayers), 3);

				// Replace the below block as well
				Block replacementBlock = ModBlocks.SNOWY_TERRAIN_BLOCKS.getOrDefault(stateDown.getBlock(), () -> null).get();
				if (replacementBlock != null) {
					BlockState replacementState = replacementBlock.getDefaultState();
					worldIn.setBlockState(posDown, replacementState, 2);
				}
				
			}
		}
	}

}
