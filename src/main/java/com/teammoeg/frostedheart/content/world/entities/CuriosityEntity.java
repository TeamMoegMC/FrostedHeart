/*
 * Copyright (c) 2024 TeamMoeg
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

public class CuriosityEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(CuriosityEntity.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = (ServerBossEvent)(new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true).setPlayBossMusic(true);
    private static final Predicate<LivingEntity> LIVING_ENTITY_SELECTOR = (entity) -> entity instanceof Player;
    public CuriosityEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.xpReward = 50;
        this.setInvulnerableTicks(220);
        this.bossEvent.setProgress(0.0F);
        this.setHealth(this.getMaxHealth() / 3);
        // FHMain.LOGGER.debug("CuriosityEntity constructor: InvTicks = " + this.getInvulnerableTicks());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new CuriosityEntity.CuriosityDoNothingGoal());
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_INV, 0);
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Invul", this.getInvulnerableTicks());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if ((pCompound.contains("Invul", 99))) {
            this.setInvulnerableTicks(pCompound.getInt("Invul"));
        }
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.WITHER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    public void aiStep() {
//        FHMain.LOGGER.debug("CuriosityEntity presuper aiStep: InvTicks = " + this.getInvulnerableTicks());
        super.aiStep();
//        FHMain.LOGGER.debug("CuriosityEntity aftsuper aiStep: InvTicks = " + this.getInvulnerableTicks());
        if (this.getInvulnerableTicks() > 0) {
            for(int i1 = 0; i1 < 3; ++i1) {
                this.level().addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + this.random.nextGaussian(), this.getY() + (double)(this.random.nextFloat() * 3.3F), this.getZ() + this.random.nextGaussian(), (double)0.7F, (double)0.7F, (double)0.9F);
            }
        } else {
            for(int i1 = 0; i1 < 3; ++i1) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX() + this.random.nextGaussian(), this.getY() + (double)(this.random.nextFloat() * 3.3F), this.getZ() + this.random.nextGaussian(), (double)0.7F, (double)0.7F, (double)0.9F);
            }
        }
    }

    protected void customServerAiStep() {
//        FHMain.LOGGER.debug("CuriosityEntity customServerAiStep: InvTicks = " + this.getInvulnerableTicks());
        if (this.getInvulnerableTicks() > 0) {

            int remTicks = this.getInvulnerableTicks() - 1;
            this.bossEvent.setProgress(1.0F - (float)remTicks / 220.0F);
            this.setInvulnerableTicks(remTicks);
            if (this.tickCount % 10 == 0) {
                this.heal(1.0F);
            }

        } else {
            super.customServerAiStep();

//            if (this.getTarget() != null) {
//                this.setAlternativeTarget(0, this.getTarget().getId());
//            } else {
//                this.setAlternativeTarget(0, 0);
//            }

            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    public boolean hurt(DamageSource pSource, float pAmount) {
        Entity entity1 = pSource.getEntity();
        // if the entity is invulnerable to the source, return false
        if (this.isInvulnerableTo(pSource)) {
            return false;
        // if the entity is not a CuriosityEntity
        } else {
            // if still counting and source is not bypassing invulnerability, return false
            if (this.getInvulnerableTicks() > 0 && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            } else if (pSource.is(DamageTypeTags.IS_FIRE)) {
                return super.hurt(pSource, pAmount);
            } else {
                return false;
            }
        }
    }

    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.bossEvent.addPlayer(pPlayer);
    }

    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.bossEvent.removePlayer(pPlayer);
    }

    public int getInvulnerableTicks() {
        return this.entityData.get(DATA_ID_INV);
    }

    public void setInvulnerableTicks(int pInvulnerableTicks) {
        this.entityData.set(DATA_ID_INV, pInvulnerableTicks);
    }

    public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {

    }

    protected boolean canRide(Entity pEntity) {
        return false;
    }

    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pEffectInstance) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.D).add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    public static boolean canSpawn(EntityType<CuriosityEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        return Monster.checkMonsterSpawnRules(entityType, level, spawnType, position, random);
    }

    class CuriosityDoNothingGoal extends Goal {
        public CuriosityDoNothingGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            return CuriosityEntity.this.getInvulnerableTicks() > 0;
        }
    }
}
