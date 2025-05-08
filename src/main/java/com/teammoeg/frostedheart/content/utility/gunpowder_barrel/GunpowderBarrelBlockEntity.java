package com.teammoeg.frostedheart.content.utility.gunpowder_barrel;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class GunpowderBarrelBlockEntity extends BlockEntity {

    public GunpowderBarrelBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(FHBlockEntityTypes.GUNPOWDER_BARREL.get(), pPos, pBlockState);
    }
    int power = 1;
    int fortune = 0;
    int timer = 0;
    boolean isExploding = false;

    public boolean isExploding() {
        return isExploding;
    }

    public void setExploding(boolean exploding) {
        isExploding = exploding;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt("Power",power);
        pTag.putInt("Fortune",fortune);
        pTag.putInt("Timer", timer);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        power = pTag.getInt("Power");
        fortune = pTag.getInt("Fortune");
        timer = pTag.getInt("Timer");
    }
    public void drops() {
        if (!Objects.requireNonNull(getLevel()).isClientSide()){
            if (this.getBlockState().getValue(GunpowderBarrelBlock.LIT)){
                return;
            }
            ItemStack stack = new ItemStack(FHBlocks.GUNPOWDER_BARREL.get());
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Power",power);
            nbt.putInt("Fortune",fortune);
            BlockItem.setBlockEntityData(stack,FHBlockEntityTypes.GUNPOWDER_BARREL.get(),nbt);
            ItemEntity item = new ItemEntity(getLevel(),
                    getBlockPos().getX()+.5,getBlockPos().getY()+.5,getBlockPos().getZ()+.5,
                    stack);
            item.setNoPickUpDelay();
            getLevel().addFreshEntity(item);
        }
    }
    public static void breakBlockWithFortune(Level level, BlockPos pos, int fortune) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getExplosionResistance() > 1000f) {
            return;
        }
        ItemStack fakeTool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (fortune > 0){
            fakeTool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
        }
        LootParams.Builder lootBuilder = new LootParams.Builder((ServerLevel) level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, fakeTool)
                .withParameter(LootContextParams.THIS_ENTITY, null)
                .withParameter(LootContextParams.BLOCK_STATE, state);
        List<ItemStack> drops = state.getDrops(lootBuilder);
        for (ItemStack stack : drops) {
            Block.popResource(level, pos, stack);
        }
        level.removeBlock(pos, false);
    }
    public static void createSafeExplosion(Level level, BlockPos pos, float power) {
        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 center = Vec3.atCenterOf(pos);

        double radius = power * 2.0;
        AABB area = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius
        );

        for (Entity entity : serverLevel.getEntities(null, area)) {
            if (!(entity instanceof LivingEntity)) continue;

            Vec3 entityPos = entity.position();
            double distance = entityPos.distanceTo(center);

            double damageFactor = (1.0 - (distance / radius)) * power;
            if (damageFactor <= 0) continue;

            float damage = (float) ((damageFactor * damageFactor + damageFactor) * 7.0 + 1.0);
            entity.hurt(serverLevel.damageSources().explosion(null), damage);

            Vec3 knockbackVec = entityPos.subtract(center).normalize();
            double knockbackPower = damageFactor * 2.0;

            entity.setDeltaMovement(
                    entity.getDeltaMovement().add(
                            knockbackVec.x * knockbackPower,
                            knockbackVec.y * knockbackPower + 0.5,
                            knockbackVec.z * knockbackPower
                    )
            );
        }
        for (int i = 0; i < 20; i++) {
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    center.x + (level.random.nextDouble() - 0.5),
                    center.y + 0.5,
                    center.z + (level.random.nextDouble() - 0.5),
                    1,
                    0, 0, 0,
                    0.5
            );
        }
        serverLevel.playSound(
                null,
                pos,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F
        );
    }
    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide()){
            return;
        }
        if(pState.getValue(GunpowderBarrelBlock.LIT)){
            timer++;
            if (timer > 80){
                createSafeExplosion(pLevel,pPos,3.0f+power);
                for (int x = -power; x <= power; x++){
                    for (int y = -power; y <= power; y++){
                        for(int z = -power; z <= power; z++){
                            if (x==0&&y==0&&z==0) continue;
                            breakBlockWithFortune(pLevel,
                                    new BlockPos(pPos.getX()+x,pPos.getY()+y,pPos.getZ()+z),
                                    fortune);
                        }
                    }
                }
                pLevel.removeBlock(pPos,false);
            }
        }
    }
}
