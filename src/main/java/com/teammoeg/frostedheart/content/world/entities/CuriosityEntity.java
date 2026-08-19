/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.world.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.SphereHeatArea;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Curiosity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 「雪原深处的好奇心」：匍匐纳米机器人集群 Boss。
 * <p>
 * "Curiosity of the Deep Frostland": the crawling nanite cluster boss. A
 * single-entity multi-phase state machine drives the whole fight; the cold
 * field is a negative {@link SphereHeatArea} so the mod's temperature system
 * handles the threat and the player's hot drinks / heating backpack / heat
 * sources handle the counterplay. The exposed core can only be dispersed by
 * fire. See docs/boss/curiosity-boss-design.md.
 */
public class CuriosityEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(CuriosityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_MUSIC = SynchedEntityData.defineId(CuriosityEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
    private final CuriosityArena arena = new CuriosityArena();

    // ==================== 服务器侧状态 / server-side state ====================
    private CuriosityPhase phase = CuriosityPhase.DORMANT;
    private int round;
    @Nullable
    private BlockPos arenaCenter;
    private int surfaceY;
    private int stateTimer;
    private int lingerTicks;
    private int fleeTicks;
    private int huntTicksLeft;
    private int mazeTicksLeft;
    private int burnTicks;
    private int trailCooldown;
    private int moundCooldown;
    private int retargetCooldown;
    @Nullable
    private UUID target;
    @Nullable
    private CuriosityMaze maze;
    @Nullable
    private List<BlockPos> mazeColumns;
    private int raiseCursor;
    private long mazeSeed;
    private int mazeEntranceCX, mazeEntranceCZ, mazeBorderSide, mazeCoreCX, mazeCoreCZ;
    private boolean coldApplied;
    @Nullable
    private BlockPos corePos;

    public CuriosityEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        var config = FHConfig.SERVER.CURIOSITY;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(config.coreHealth.get());
        this.setHealth(this.getMaxHealth());
        this.noPhysics = true;
        this.setInvisible(true);
        this.setNoGravity(true);
        this.bossEvent.setVisible(false);
    }

    @Override
    protected void registerGoals() {
        // 全部行为由 customServerAiStep 状态机驱动 / all behavior is driven by the state machine
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, CuriosityPhase.DORMANT.ordinal());
        this.entityData.define(DATA_MUSIC, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // ==================== 阶段访问 / phase access ====================

    private void setPhase(CuriosityPhase next) {
        this.phase = next;
        this.entityData.set(DATA_PHASE, next.ordinal());
        this.entityData.set(DATA_MUSIC, FHConfig.SERVER.CURIOSITY.bossMusic.get() && next.isCombat());
        this.bossEvent.setVisible(next.isCombat());
    }

    /** 客户端读阶段 / client-side phase view (synced). */
    public CuriosityPhase getClientPhase() {
        CuriosityPhase[] values = CuriosityPhase.values();
        return values[Mth.clamp(this.entityData.get(DATA_PHASE), 0, values.length - 1)];
    }

    public boolean wantsMusic() {
        return this.entityData.get(DATA_MUSIC);
    }

    // ==================== 持久化 / persistence ====================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("phase", this.phase.name());
        tag.putInt("round", this.round);
        if (this.arenaCenter != null) tag.putLong("arenaCenter", this.arenaCenter.asLong());
        tag.putInt("surfaceY", this.surfaceY);
        tag.putInt("stateTimer", this.stateTimer);
        tag.putInt("linger", this.lingerTicks);
        tag.putInt("flee", this.fleeTicks);
        tag.putInt("hunt", this.huntTicksLeft);
        tag.putInt("mazeTicks", this.mazeTicksLeft);
        tag.putInt("burn", this.burnTicks);
        tag.putInt("raiseCursor", this.raiseCursor);
        tag.putLong("mazeSeed", this.mazeSeed);
        tag.putInt("mazeEntranceCX", this.mazeEntranceCX);
        tag.putInt("mazeEntranceCZ", this.mazeEntranceCZ);
        tag.putInt("mazeBorderSide", this.mazeBorderSide);
        tag.putInt("mazeCoreCX", this.mazeCoreCX);
        tag.putInt("mazeCoreCZ", this.mazeCoreCZ);
        if (this.corePos != null) tag.putLong("corePos", this.corePos.asLong());
        this.arena.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phase = parsePhase(tag.getString("phase"));
        this.round = tag.getInt("round");
        this.arenaCenter = tag.contains("arenaCenter") ? BlockPos.of(tag.getLong("arenaCenter")) : null;
        this.surfaceY = tag.getInt("surfaceY");
        this.stateTimer = tag.getInt("stateTimer");
        this.lingerTicks = tag.getInt("linger");
        this.fleeTicks = tag.getInt("flee");
        this.huntTicksLeft = tag.getInt("hunt");
        this.mazeTicksLeft = tag.getInt("mazeTicks");
        this.burnTicks = tag.getInt("burn");
        this.raiseCursor = tag.getInt("raiseCursor");
        this.mazeSeed = tag.getLong("mazeSeed");
        this.mazeEntranceCX = tag.getInt("mazeEntranceCX");
        this.mazeEntranceCZ = tag.getInt("mazeEntranceCZ");
        this.mazeBorderSide = tag.getInt("mazeBorderSide");
        this.mazeCoreCX = tag.getInt("mazeCoreCX");
        this.mazeCoreCZ = tag.getInt("mazeCoreCZ");
        this.corePos = tag.contains("corePos") ? BlockPos.of(tag.getLong("corePos")) : null;
        this.arena.load(tag, this.level().registryAccess());
        this.coldApplied = false;
        this.entityData.set(DATA_PHASE, this.phase.ordinal());
        this.entityData.set(DATA_MUSIC, FHConfig.SERVER.CURIOSITY.bossMusic.get() && this.phase.isCombat());
        this.bossEvent.setVisible(this.phase.isCombat());
        // Entity.load 对 NBT 缺键无条件 setNoGravity(getBoolean(...))=false、setInvisible(false)，
        // /summon 的空标签会抹掉构造器里设好的地下状态；这里按阶段重新断言。
        // Entity.load unconditionally resets noGravity/invisible to false for missing NBT keys
        // (getBoolean returns false), which wipes the underground state set in the constructor
        // when spawned via /summon; re-assert it per phase here.
        boolean underground = this.phase != CuriosityPhase.EXPOSED;
        this.noPhysics = underground;
        this.setNoGravity(underground);
        this.setInvisible(underground);
        // 重载后按阶段恢复瞬态结构 / restore transient structures after reload
        if (this.phase == CuriosityPhase.MAZE || this.phase == CuriosityPhase.EXPOSED) {
            this.maze = rebuildMaze();
            if (this.maze != null) {
                this.mazeColumns = this.maze.orderedWallColumns(RandomSource.create(this.mazeSeed));
            }
        }
    }

    private static CuriosityPhase parsePhase(String name) {
        try {
            return CuriosityPhase.valueOf(name);
        } catch (IllegalArgumentException e) {
            return CuriosityPhase.DORMANT;
        }
    }

    // ==================== 通用行为 / common behavior ====================

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 虚空伤害放行：坠出世界的实体应正常死亡，避免永久残留 / let out-of-world kills through
        if ("outOfWorld".equals(source.getMsgId())) {
            return super.hurt(source, amount);
        }
        if (this.phase != CuriosityPhase.EXPOSED) {
            // 允许越过无敌的伤害（/kill、创造）用于测试清理 / allow bypass damage for cleanup
            return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
        }
        if (!source.is(DamageTypeTags.IS_FIRE)) {
            if (!this.level().isClientSide && source.getEntity() != null) {
                this.level().playSound(null, this, SoundEvents.SNOW_HIT, SoundSource.HOSTILE, 0.6F, 1.5F);
            }
            return false;
        }
        this.setSecondsOnFire(5); // 火伤命中即点燃，驱动燃尽流程 / ignite on hit, drives the burn-down
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.phase != CuriosityPhase.EXPOSED || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        // 打火石/火焰弹右键直接点燃核心（原版 1.20.1 的 FlintAndSteelItem/FireChargeItem
        // 没有 interactLivingEntity，右键实体不会有任何效果，这里补上）
        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            if (!this.level().isClientSide) {
                this.setSecondsOnFire(5);
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                } else if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.level().playSound(null, this, SoundEvents.FLINTANDSTEEL_USE, SoundSource.HOSTILE,
                        1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemovedFromWorld() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && this.arenaCenter != null) {
            ChunkHeatData.removeTempAdjust(sl, this.arenaCenter);
        }
        super.onRemovedFromWorld();
    }

    // ==================== 客户端表现 / client visuals ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CuriosityClientEffects.updateMusic(this));
            CuriosityPhase p = getClientPhase();
            if (p == CuriosityPhase.DORMANT) {
                if (this.tickCount % 15 == 0 && this.level().isLoaded(this.blockPosition())) {
                    int h = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, this.blockPosition().getX(), this.blockPosition().getZ());
                    this.level().addParticle(ParticleTypes.SNOWFLAKE,
                            this.getX() + (this.random.nextDouble() - 0.5) * 10.0,
                            h + 0.5,
                            this.getZ() + (this.random.nextDouble() - 0.5) * 10.0,
                            0, 0.01, 0);
                }
            } else if (p == CuriosityPhase.RISING) {
                if (this.tickCount % 2 == 0 && this.level().isLoaded(this.blockPosition())) {
                    int h = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, this.blockPosition().getX(), this.blockPosition().getZ());
                    this.level().addParticle(ParticleTypes.POOF,
                            this.getX() + (this.random.nextDouble() - 0.5) * 14.0,
                            h + 0.3,
                            this.getZ() + (this.random.nextDouble() - 0.5) * 14.0,
                            (this.random.nextDouble() - 0.5) * 0.1, 0.05, (this.random.nextDouble() - 0.5) * 0.1);
                }
            } else if (p == CuriosityPhase.EXPOSED) {
                this.level().addParticle(ParticleTypes.POOF,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.6,
                        this.getY() + 0.5 + this.random.nextDouble() * 0.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.6,
                        0, 0.02, 0);
            }
        }
    }

    // ==================== 状态机 / state machine ====================

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Curiosity config = FHConfig.SERVER.CURIOSITY;
        if (this.phase.isColdActive() && !this.coldApplied) {
            applyColdField(sl);
        }
        switch (this.phase) {
            case DORMANT -> tickDormant(sl, config);
            case RISING -> tickRising(sl, config);
            case HUNT -> tickHunt(sl, config);
            case MAZE -> tickMaze(sl, config);
            case EXPOSED -> tickExposed(sl, config);
            case BURROW -> tickBurrow(sl, config);
            case DISPERSED -> { /* discarded */ }
        }
    }

    // ---------- DORMANT ----------

    private void tickDormant(ServerLevel sl, Curiosity config) {
        if (this.arenaCenter == null) {
            this.arenaCenter = this.blockPosition();
            this.surfaceY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, this.arenaCenter.getX(), this.arenaCenter.getZ());
            this.teleportTo(this.arenaCenter.getX() + 0.5, this.surfaceY - 1.0, this.arenaCenter.getZ() + 0.5);
            this.lingerTicks = 0;
            return;
        }
        if (nearestPlayer(sl, config.lingerRadius.get(), this.arenaCenter) != null) {
            this.lingerTicks++;
            if (this.lingerTicks >= config.lingerSeconds.get() * 20) {
                beginRising(sl, config);
            }
        } else {
            this.lingerTicks = 0;
        }
    }

    private void beginRising(ServerLevel sl, Curiosity config) {
        setPhase(CuriosityPhase.RISING);
        this.stateTimer = config.risingTicks.get();
        this.lingerTicks = 0;
        applyColdField(sl);
        sl.playSound(null, this.arenaCenter, FHSoundEvents.WIND.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    private void tickRising(ServerLevel sl, Curiosity config) {
        this.stateTimer--;
        if (this.stateTimer <= 0) {
            beginHunt(sl, config);
        }
    }

    // ---------- HUNT ----------

    private void beginHunt(ServerLevel sl, Curiosity config) {
        setPhase(CuriosityPhase.HUNT);
        this.huntTicksLeft = config.huntDurationTicks.get();
        this.target = null;
        this.retargetCooldown = 0;
        this.trailCooldown = 0;
        this.moundCooldown = 0;
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
        if (this.arenaCenter != null) {
            this.teleportTo(this.arenaCenter.getX() + 0.5, this.surfaceY - 1.0, this.arenaCenter.getZ() + 0.5);
        }
    }

    private void tickHunt(ServerLevel sl, Curiosity config) {
        if (checkFleeOrReset(sl, config)) return;
        this.huntTicksLeft--;
        if (this.huntTicksLeft <= 0) {
            beginMaze(sl, config);
            return;
        }
        if (this.target == null || --this.retargetCooldown <= 0) {
            Player p = nearestPlayer(sl, config.arenaRadius.get(), this.arenaCenter);
            this.target = p == null ? null : p.getUUID();
            this.retargetCooldown = 20;
        }
        Player tp = this.target == null ? null : sl.getPlayerByUUID(this.target);
        if (tp != null) {
            double speed = Math.min(config.trackerSpeed.get() + this.round * config.trackerSpeedPerRound.get(),
                    config.trackerSpeedCap.get());
            Vec3 delta = tp.position().subtract(this.position());
            double len = Math.hypot(delta.x, delta.z);
            if (len > 0.05) {
                Vec3 dir = new Vec3(delta.x / len, 0, delta.z / len);
                this.move(MoverType.SELF, dir.scale(speed));
                this.setYRot((float) (Mth.atan2(dir.x, dir.z) * (180D / Math.PI)));
                this.yBodyRot = this.getYRot();
            }
            if (--this.moundCooldown <= 0) {
                spawnMound(sl);
                this.moundCooldown = config.moundIntervalTicks.get();
            }
            if (config.powderSnowEnabled.get() && --this.trailCooldown <= 0) {
                placeTrail(sl, config);
                this.trailCooldown = config.powderSnowIntervalTicks.get();
            }
            if (len < 2.0) {
                placeTrail(sl, config);
            }
        }
        this.bossEvent.setProgress(Mth.clamp(1.0F - (float) this.huntTicksLeft / config.huntDurationTicks.get(), 0F, 1F));
    }

    private void spawnMound(ServerLevel sl) {
        BlockPos pos = surfaceAbove();
        CuriosityMoundEntity mound = new CuriosityMoundEntity(FHEntityTypes.CURIOSITY_MOUND.get(), sl);
        mound.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sl.addFreshEntity(mound);
    }

    private void placeTrail(ServerLevel sl, Curiosity config) {
        int h = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, this.blockPosition().getX(), this.blockPosition().getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                this.arena.placePowderSnow(sl, new BlockPos(this.blockPosition().getX() + dx, h, this.blockPosition().getZ() + dz));
            }
        }
        while (this.arena.powderSnowCount() > config.powderSnowMaxPatches.get()) {
            this.arena.restoreOldestPowderSnow(sl);
        }
    }

    // ---------- MAZE ----------

    private void beginMaze(ServerLevel sl, Curiosity config) {
        setPhase(CuriosityPhase.MAZE);
        applyColdField(sl);
        this.mazeSeed = sl.getRandom().nextLong();
        this.maze = new CuriosityMaze(config.mazeCells.get());
        this.maze.generate(RandomSource.create(this.mazeSeed));
        if (this.arenaCenter != null) {
            int half = this.maze.footprint / 2;
            this.maze.setOrigin(this.arenaCenter.offset(-half, 0, -half));
            Player p = nearestPlayer(sl, config.arenaRadius.get(), this.arenaCenter);
            Vec3 at = p != null ? p.position() : this.position();
            this.maze.setEntrance(Mth.floor(at.x), Mth.floor(at.z));
            this.maze.chooseCore(RandomSource.create(this.mazeSeed ^ 0x9E3779B97F4A7C15L));
            this.corePos = this.maze.coreWorldPos();
            this.mazeEntranceCX = this.maze.entranceCellX();
            this.mazeEntranceCZ = this.maze.entranceCellZ();
            this.mazeBorderSide = this.maze.borderSide();
            this.mazeCoreCX = this.maze.coreCellX();
            this.mazeCoreCZ = this.maze.coreCellZ();
            this.mazeColumns = this.maze.orderedWallColumns(RandomSource.create(this.mazeSeed));
            this.raiseCursor = 0;
        }
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
        if (this.arenaCenter != null) {
            this.teleportTo(this.arenaCenter.getX() + 0.5, this.surfaceY - 1.0, this.arenaCenter.getZ() + 0.5);
        }
    }

    private void tickMaze(ServerLevel sl, Curiosity config) {
        if (checkFleeOrReset(sl, config)) return;
        if (this.mazeColumns == null) {
            beginExposed(sl, config);
            return;
        }
        int total = this.mazeColumns.size();
        if (this.raiseCursor >= total) {
            beginExposed(sl, config);
            return;
        }
        int perTick = Math.max(1, total / config.mazeRaiseTicks.get());
        for (int i = 0; i < perTick && this.raiseCursor < total; i++, this.raiseCursor++) {
            placeColumn(sl, this.mazeColumns.get(this.raiseCursor));
        }
    }

    private void placeColumn(ServerLevel sl, BlockPos col) {
        int h = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ());
        for (int y = h; y < h + 3; y++) {
            this.arena.placeWall(sl, col.atY(y));
        }
    }

    private void beginExposed(ServerLevel sl, Curiosity config) {
        if (this.corePos == null) {
            // 极端兜底：核心取场地中心 / extreme fallback: core at arena center
            this.corePos = this.arenaCenter != null ? this.arenaCenter : this.blockPosition();
        }
        int h = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, this.corePos.getX(), this.corePos.getZ());
        this.setNoGravity(false);
        this.noPhysics = false;
        this.setInvisible(false);
        this.setHealth(this.getMaxHealth());
        this.clearFire();
        this.burnTicks = 0;
        this.setDeltaMovement(Vec3.ZERO);
        this.teleportTo(this.corePos.getX() + 0.5, h, this.corePos.getZ() + 0.5);
        this.setPhase(CuriosityPhase.EXPOSED);
        this.mazeTicksLeft = config.mazeDurationTicks.get();
        sl.playSound(null, this.blockPosition(), SoundEvents.SNOW_STEP, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    // ---------- EXPOSED ----------

    private void tickExposed(ServerLevel sl, Curiosity config) {
        if (checkFleeOrReset(sl, config)) return;
        this.setDeltaMovement(0, Math.min(0, this.getDeltaMovement().y), 0);
        this.mazeTicksLeft--;
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.isOnFire()) {
            this.burnTicks++;
            if (this.burnTicks >= config.coreBurnTicks.get()) {
                disperse(sl, config);
                return;
            }
        }
        if (this.mazeTicksLeft <= 0) {
            beginBurrow(sl, config);
        }
    }

    // ---------- BURROW / RESET / DISPERSED ----------

    private void beginBurrow(ServerLevel sl, Curiosity config) {
        setPhase(CuriosityPhase.BURROW);
        this.stateTimer = config.burrowTicks.get();
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.clearFire();
        this.burnTicks = 0;
        this.arena.restoreMaze(sl);
        sl.playSound(null, this.arenaCenter, FHSoundEvents.WIND.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
        if (this.arenaCenter != null) {
            this.teleportTo(this.arenaCenter.getX() + 0.5, this.surfaceY - 1.0, this.arenaCenter.getZ() + 0.5);
        }
    }

    private void tickBurrow(ServerLevel sl, Curiosity config) {
        this.stateTimer--;
        if (this.stateTimer <= 0) {
            this.round++;
            applyColdField(sl); // 按新轮次更新冷场阶梯 / refresh the cold field tier for the new round
            beginHunt(sl, config);
        }
    }

    /**
     * 脱战检查：战斗范围内所有玩家死亡 → 立即重置；无人（逃离）累计 10s → 重置。
     * <p>
     * Flee/death check: if every player in combat range is dead, reset at once;
     * if nobody is around (fled), reset after the configured grace period.
     */
    private boolean checkFleeOrReset(ServerLevel sl, Curiosity config) {
        BlockPos center = this.arenaCenter != null ? this.arenaCenter : this.blockPosition();
        if (nearestPlayer(sl, config.escapeRadius.get(), center) != null) {
            this.fleeTicks = 0;
            return false;
        }
        boolean anyDeadNear = false;
        for (Player p : sl.players()) {
            if (p.isAlive() || p.isSpectator()) continue;
            if (p.distanceToSqr(center.getCenter()) <= (double) config.escapeRadius.get() * config.escapeRadius.get()) {
                anyDeadNear = true;
                break;
            }
        }
        if (anyDeadNear) {
            reset(sl, config);
            return true;
        }
        this.fleeTicks++;
        if (this.fleeTicks >= config.escapeSeconds.get() * 20) {
            reset(sl, config);
            return true;
        }
        return false;
    }

    private void reset(ServerLevel sl, Curiosity config) {
        this.arena.restoreAll(sl);
        removeColdField(sl);
        this.round = 0;
        this.huntTicksLeft = 0;
        this.mazeTicksLeft = 0;
        this.burnTicks = 0;
        this.lingerTicks = 0;
        this.fleeTicks = 0;
        this.target = null;
        this.maze = null;
        this.mazeColumns = null;
        this.corePos = null;
        this.clearFire();
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setHealth(this.getMaxHealth());
        if (this.arenaCenter != null) {
            this.teleportTo(this.arenaCenter.getX() + 0.5, this.surfaceY - 1.0, this.arenaCenter.getZ() + 0.5);
        }
        setPhase(CuriosityPhase.DORMANT);
    }

    private void disperse(ServerLevel sl, Curiosity config) {
        setPhase(CuriosityPhase.DISPERSED); // 同步给客户端：停止音乐与 Boss 条 / syncs to clients: stops music & boss bar
        sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 80, 2.5, 1.5, 2.5, 0.05);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0, this.getZ(), 60, 3.0, 2.0, 3.0, 0.1);
        sl.playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 1.0F);
        this.arena.restoreAll(sl);
        removeColdField(sl);
        dropLoot(sl, config);
        ExperienceOrb.award(sl, this.position(), config.oreFrostDropXp.get());
        this.discard();
    }

    private void dropLoot(ServerLevel sl, Curiosity config) {
        List<Item> pool = null;
        var reg = sl.registryAccess().registry(Registries.ITEM);
        if (reg.isPresent()) {
            var named = reg.get().getTag(FHTags.Items.CONDENSED_BALLS.tag);
            if (named.isPresent()) {
                pool = new ArrayList<>();
                for (Holder<Item> holder : named.get()) {
                    pool.add(holder.value());
                }
            }
        }
        if (pool == null || pool.isEmpty()) {
            pool = List.of(FHItems.CONDENSED_BALL_IRON_ORE.get());
        }
        int count = config.oreFrostDropCount.get();
        for (int i = 0; i < count; i++) {
            ItemStack stack = new ItemStack(pool.get(sl.random.nextInt(pool.size())));
            ItemEntity item = new ItemEntity(sl, this.getX(), this.getY() + 0.5, this.getZ(), stack);
            item.setDeltaMovement(sl.random.nextDouble() * 0.4 - 0.2, 0.3, sl.random.nextDouble() * 0.4 - 0.2);
            item.setInvulnerable(true); // 火焰免疫：核心常被岩浆/火矢击杀，掉落物不应被烧毁 / drops survive lava & fire
            sl.addFreshEntity(item);
        }
    }

    // ---------- 冷场 / cold field ----------

    private void applyColdField(ServerLevel sl) {
        if (this.arenaCenter == null) return;
        Curiosity config = FHConfig.SERVER.CURIOSITY;
        int base = (this.phase == CuriosityPhase.MAZE || this.phase == CuriosityPhase.EXPOSED)
                ? config.coldTier2.get() : config.coldTier1.get();
        int tier = Math.max(config.coldCap.get(), base + this.round * config.coldPerRound.get());
        ChunkHeatData.addTempAdjust(sl, new SphereHeatArea(this.arenaCenter, config.arenaRadius.get(), tier));
        this.coldApplied = true;
    }

    private void removeColdField(ServerLevel sl) {
        if (this.arenaCenter == null) return;
        ChunkHeatData.removeTempAdjust(sl, this.arenaCenter);
        this.coldApplied = false;
    }

    // ---------- 工具 / helpers ----------

    @Nullable
    private CuriosityMaze rebuildMaze() {
        if (this.arenaCenter == null) return null;
        CuriosityMaze m = new CuriosityMaze(FHConfig.SERVER.CURIOSITY.mazeCells.get());
        m.generate(RandomSource.create(this.mazeSeed));
        int half = m.footprint / 2;
        m.setOrigin(this.arenaCenter.offset(-half, 0, -half));
        m.setEntranceCell(this.mazeEntranceCX, this.mazeEntranceCZ, this.mazeBorderSide);
        m.setCoreCell(this.mazeCoreCX, this.mazeCoreCZ);
        return m;
    }

    @Nullable
    private Player nearestPlayer(ServerLevel sl, double radius, @Nullable BlockPos center) {
        Player best = null;
        double bestD = radius * radius;
        for (Player p : sl.players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            double d;
            if (center != null) {
                d = p.distanceToSqr(center.getX() + 0.5, p.getY(), center.getZ() + 0.5);
            } else {
                d = p.distanceToSqr(this);
            }
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private BlockPos surfaceAbove() {
        int h = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, this.blockPosition().getX(), this.blockPosition().getZ());
        return new BlockPos(this.blockPosition().getX(), h, this.blockPosition().getZ());
    }

    // ---------- 生成规则 / spawn rules ----------

    public static boolean canSpawn(EntityType<CuriosityEntity> type, ServerLevelAccessor level,
                                   MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) return false;
        ResourceLocation biomeId = level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId == null || !FHConfig.SERVER.CURIOSITY.spawnBiomes.get().contains(biomeId.toString())) {
            return false;
        }
        // 平整度检查：只查询区域内/已加载的相邻区块——世界生成期间的 WorldGenRegion 越界查询会抛异常
        // Flatness check: only query neighbors inside the region/loaded chunks; WorldGenRegion
        // throws "chunk out of bound" for out-of-region queries during chunk generation.
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                int h2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX() + dx, pos.getZ() + dz);
                if (Math.abs(h2 - h) > 1) return false;
            }
        }
        BlockState ground = level.getBlockState(pos.atY(h - 1));
        if (!(ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.SNOW) || ground.is(Blocks.GRASS_BLOCK)
                || ground.is(Blocks.DIRT) || ground.is(Blocks.POWDER_SNOW))) {
            return false;
        }
        // 距玩家 >32 才生成；世界生成期间没有玩家，跳过 / keep away from players; none exist during worldgen
        if (!(level instanceof WorldGenRegion)) {
            List<Player> nearby = level.getLevel().getEntities(EntityTypeTest.forClass(Player.class),
                    new AABB(pos).inflate(32.0), p -> !p.isSpectator());
            if (!nearby.isEmpty()) return false;
        }
        return true;
    }
}
