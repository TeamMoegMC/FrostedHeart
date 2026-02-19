package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.teammoeg.chorda.block.CBlock;
import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.compat.ftb.FTBChunks;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.ItemStackMerger;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class GunpowderBarrelBlock extends CBlock implements ProperWaterloggedBlock, CEntityBlock<GunpowderBarrelBlockEntity>, Fallable {
    /**
     * 爆炸范围
     */
    public static final String RANGE = "range";
    /**
     * 时运等级
     */
    public static final String FORTUNE = "fortuneLevel";
    /**
     * 作为方块点燃后是否会下落
     */
    public static final String WILL_FALL = "willFall";
    /**
     * 是否会破坏方块
     */
    public static final String SAFE_EXPLODE = "wontDestroyBlock";
    private static final VoxelShape shape = Block.box(2, 0, 2, 14, 16, 14);

    public GunpowderBarrelBlock(Properties blockProps) {
        super(blockProps);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public static int getRange(ItemStack stack) {
        if (!(stack.getItem() instanceof GunpowderBarrelItem)) return 0;
        var tag = stack.getTag();
        return tag != null ? Math.max(tag.getInt(RANGE), 1) : 1;
    }

    public static int getFortuneLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof GunpowderBarrelItem)) return 0;
        var tag = stack.getTag();
        return tag != null ? tag.getInt(FORTUNE) : 0;
    }

    public static boolean willFall(ItemStack stack) {
        if (!(stack.getItem() instanceof GunpowderBarrelItem)) return false;
        var tag = stack.getTag();
        return tag != null && tag.getBoolean(WILL_FALL);
    }

    public static boolean willDestroyBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof GunpowderBarrelItem)) return false;
        var tag = stack.getTag();
        if (tag != null) {
            return !tag.getBoolean(SAFE_EXPLODE);
        }
        return true;
    }

    /**
     * @return 爆炸范围，如果对应坐标不是炸药桶则返回 0
     */
    public static int getRange(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GunpowderBarrelBlockEntity be) {
            return be.range;
        }
        return 0;
    }

    public static int getFortuneLevel(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GunpowderBarrelBlockEntity be) {
            return be.fortuneLevel;
        }
        return 0;
    }

    public static boolean willFall(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GunpowderBarrelBlockEntity be) {
            return be.willFall;
        }
        return false;
    }

    public static boolean willDestroyBlock(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GunpowderBarrelBlockEntity be) {
            return be.destroyBlock;
        }
        return false;
    }

    /**
     * 造成一次爆炸，范围和时运读取自世界中已有方块
     */
    public static void explode(Level level, BlockPos pos, @Nullable Entity exploder) {
        explode(level, pos, getRange(level, pos), getFortuneLevel(level, pos), willDestroyBlock(level, pos), true, exploder);
    }

    /**
     * 造成一次爆炸，范围和时运读取自传入的物品
     */
    public static void explode(Level level, BlockPos pos, ItemStack stack, boolean isFromBlock, @Nullable Entity exploder) {
        explode(level, pos, getRange(stack), getFortuneLevel(stack), willDestroyBlock(stack), isFromBlock, exploder);
    }

    /**
     * 造成一次爆炸
     * @param level Level
     * @param pos 爆炸坐标
     * @param range 爆炸半径，为 0 时不会爆炸
     * @param fortuneLevel 时运等级
     * @param exploder 造成爆炸的实体
     * @param isFromBlock 爆炸是否由方块产生，true 时会移除对应坐标的方块
     */
    public static void explode(Level level, BlockPos pos, int range, int fortuneLevel, boolean destroyBlock, boolean isFromBlock, @Nullable Entity exploder) {
        if (range == 0) return;
        // 移除本体
        if (isFromBlock && level.getBlockState(pos).getBlock() instanceof GunpowderBarrelBlock) {
            level.removeBlock(pos, false);
        }
        if (level instanceof ServerLevel sl) {
            // 生成一次原版无破坏爆炸
            var center = pos.getCenter();
            level.explode(exploder, center.x, center.y, center.z, range+1, Level.ExplosionInteraction.NONE);
            if (!destroyBlock) return;

            range = Mth.clamp(range, 1, 7);
            fortuneLevel = Mth.clamp(fortuneLevel, 0, 10);
            var positions = BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range));
            List<ItemStack> drops = new ArrayList<>();
            int exp = 0;
            // 模拟工具 TODO 找到更好的方法处理时运效果
            var tool = Items.NETHERITE_PICKAXE.getDefaultInstance();
            tool.enchant(Enchantments.BLOCK_FORTUNE, fortuneLevel);
            for (BlockPos pos1 : positions) {
                var state = level.getBlockState(pos1);
                if (state.isAir()) continue;
                // 是否被阻止
                if (exploder instanceof Player player ? FTBChunks.playerCanEdit(player, pos1) : FTBChunks.getClaimedChunk(level, pos1) != null) {
                    // 在认领区块中
                    continue;
                }
                if (exploder instanceof Player player && MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos1, state, player))) {
                    // 被事件阻止
                    continue;
                }
                // 连锁爆炸
                if (state.getBlock() instanceof GunpowderBarrelBlock) {
                    if (willFall(level, pos1)) {
                        GunpowderBarrelEntity.fall(level, pos1, getRange(level, pos1), getFortuneLevel(level, pos1), willDestroyBlock(level, pos1), exploder);
                    } else {
                        explode(level, pos1, exploder);
                    }
                    continue;
                }
                // 尝试破坏方块
                if (state.getDestroySpeed(level, pos1) < 0) continue;
                level.removeBlock(pos1, false);
                drops.addAll(state.getDrops((new LootParams.Builder(sl))
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos1))
                        .withParameter(LootContextParams.TOOL, tool)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos1))));
                exp += state.getExpDrop(level, level.getRandom(), pos1, fortuneLevel, 0);
            }
            // 掉落物品和经验
            if (drops.size() > 16) {
                drops = ItemStackMerger.mergeItemStacks(drops);
            }
            for (ItemStack drop : drops) {
                Block.popResource(level, pos, drop);
            }
            ExperienceOrb.award(sl, pos.getCenter(), exp);
        }
    }

    @Override
    public void onLand(Level pLevel, BlockPos pPos, BlockState pState, BlockState pReplaceableState, FallingBlockEntity pFallingBlock) {
        var data = pFallingBlock.blockData;
        if (pLevel instanceof ServerLevel level && data != null && pState.getBlock() instanceof GunpowderBarrelBlock) {
            explode(pLevel, pPos, data.getInt(RANGE), data.getInt(FORTUNE), !data.getBoolean(SAFE_EXPLODE), true, CUtils.getEntity(level, data.getUUID("owner")));
            return;
        }
        explode(pLevel, pPos, null);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (willFall(level, pos)) {
            GunpowderBarrelEntity.fall(level, pos, getRange(level, pos), getFortuneLevel(level, pos), willDestroyBlock(level, pos), explosion.getIndirectSourceEntity());
            return;
        }
        explode(level, pos, explosion.getIndirectSourceEntity());
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GunpowderBarrelBlockEntity be) {
            stack = GunpowderBarrelItem.create(be.range, be.fortuneLevel, be.willFall, be.destroyBlock);
        }
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return GunpowderBarrelItem.create(getRange(level, pos), getFortuneLevel(level, pos), willFall(level, pos), willDestroyBlock(level, pos));
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        if (!pOldState.is(pState.getBlock())) {
            if (pLevel.hasNeighborSignal(pPos)) {
                onCaughtFire(pState, pLevel, pPos, null, null);
            }
        }
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (pLevel.getBlockEntity(pPos) instanceof GunpowderBarrelBlockEntity be) {
            be.range = getRange(pStack);
            be.fortuneLevel = getFortuneLevel(pStack);
            be.willFall = willFall(pStack);
            be.destroyBlock = willDestroyBlock(pStack);
            be.setOwner(pPlacer);
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(ItemTags.CREEPER_IGNITERS)) {
            return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
        } else {
            onCaughtFire(pState, pLevel, pPos, pHit.getDirection(), pPlayer);
            Item item = itemstack.getItem();
            if (!pPlayer.isCreative()) {
                if (itemstack.isDamageableItem()) {
                    itemstack.hurtAndBreak(1, pPlayer, (p_57425_) -> {
                        p_57425_.broadcastBreakEvent(pHand);
                    });
                } else {
                    itemstack.shrink(1);
                }
            }

            pPlayer.awardStat(Stats.ITEM_USED.get(item));
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pLevel.getBlockEntity(pPos) instanceof GunpowderBarrelBlockEntity be && be.lit) {
            var pos = pPos.getCenter();
            pLevel.addParticle(ParticleTypes.SMOKE, pos.x, pos.y+0.6F, pos.z, 0.0D, 0.0D, 0.0D);
            pLevel.addParticle(ParticleTypes.FLAME, pos.x, pos.y+0.6F, pos.z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        if (!pLevel.isClientSide) {
            BlockPos blockpos = pHit.getBlockPos();
            Entity entity = pProjectile.getOwner();
            if (pProjectile.isOnFire() && pProjectile.mayInteract(pLevel, blockpos)) {
                onCaughtFire(pState, pLevel, blockpos, null, entity instanceof LivingEntity ? (LivingEntity)entity : null);
            }
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide()) {
            explode(level, pos, explosion.getExploder());
        }
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
        if (level.getBlockEntity(pos) instanceof GunpowderBarrelBlockEntity be) {
            be.lit();
        }
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (pLevel.hasNeighborSignal(pPos)) {
            onCaughtFire(pState, pLevel, pPos, null, null);
        }
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(WATERLOGGED));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withWater(super.getStateForPlacement(context), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world,
                                  BlockPos pos, BlockPos neighbourPos) {
        updateWater(world, state, pos);
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return shape;
    }

    @Override
    public Supplier<BlockEntityType<GunpowderBarrelBlockEntity>> getBlock() {
        return FHBlockEntityTypes.GUNPOWDER_BARREL;
    }
}
