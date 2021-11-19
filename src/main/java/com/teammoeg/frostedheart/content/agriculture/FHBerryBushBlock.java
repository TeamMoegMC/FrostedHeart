package com.teammoeg.frostedheart.content.agriculture;

import java.util.Random;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class FHBerryBushBlock extends SweetBerryBushBlock {
    public final String name;
    private int growTemperature;
    protected int growSpeed = 100;//0<growSpeed<100,100growSpeed相当于原版浆果丛的生长速度

    public FHBerryBushBlock(String name, int growTemperature, Properties properties) {
        super(properties);
        this.name = name;
        this.growTemperature = growTemperature;
        FHContent.registeredFHBlocks.add(this);
        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);
    }//if you don't want to set growSpeed
    public FHBerryBushBlock(String name, int growTemperature, Properties properties, int growSpeed) {
        super(properties);
        this.name = name;
        this.growTemperature = growTemperature;
        FHContent.registeredFHBlocks.add(this);
        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);
        this.growSpeed = growSpeed;
    }
    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }
    public int getGrowTemperature() {
        return growTemperature;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        int i = state.get(AGE);
        float temp = ChunkData.getTemperature(worldIn, pos);
        if (temp < this.growTemperature) {
            if (worldIn.getRandom().nextInt(3) == 0) {
                worldIn.setBlockState(pos, this.getDefaultState(), 2);
            }
            //我也不知道这玩意干啥用的，我看FHCropBlock里有就加上了
        }else if (i < 3 && worldIn.getLightSubtracted(pos.up(), 0) >= 9 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos, state,random.nextInt(5) == 0) && this.growSpeed > random.nextInt(100)) {
            worldIn.setBlockState(pos, state.with(AGE, Integer.valueOf(i + 1)), 2);
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state);
        }
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, BlockState state) {
        float temp = ChunkData.getTemperature(worldIn, pos);
        return temp >= growTemperature;
    }
    @Override
    public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
        if (entityIn instanceof LivingEntity && entityIn.getType() != EntityType.FOX && entityIn.getType() != EntityType.BEE) {
            entityIn.setMotionMultiplier(state, new Vector3d((double)0.8F, 0.75D, (double)0.8F));
            if (!worldIn.isRemote && state.get(AGE) > 0 && (entityIn.lastTickPosX != entityIn.getPosX() || entityIn.lastTickPosZ != entityIn.getPosZ())) {
                double d0 = Math.abs(entityIn.getPosX() - entityIn.lastTickPosX);
                double d1 = Math.abs(entityIn.getPosZ() - entityIn.lastTickPosZ);
                if (d0 >= (double)0.003F || d1 >= (double)0.003F) {
                    entityIn.attackEntityFrom(DamageSource.SWEET_BERRY_BUSH, 0.0F);//remove damage
                }
            }

        }
    }

}
