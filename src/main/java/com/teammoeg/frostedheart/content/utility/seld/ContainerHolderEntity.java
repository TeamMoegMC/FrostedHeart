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

package com.teammoeg.frostedheart.content.utility.seld;

import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class ContainerHolderEntity extends Entity implements Container, MenuProvider, IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(ContainerHolderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(ContainerHolderEntity.class, EntityDataSerializers.FLOAT);

    private ItemStack containerStack = ItemStack.EMPTY;

    //for client
//    private BlockState displayState = Blocks.AIR.defaultBlockState();
    private BlockState displayState = Blocks.CHEST.defaultBlockState();
    private BaseContainerBlockEntity innerBlockEntity;

    public ContainerHolderEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    protected ContainerHolderEntity(Level level, Entity sled, ItemStack containerStack) {
        this(FHEntityTypes.CONTAINER_ENTITY.get(), level);
        this.setContainerItem(containerStack);
        this.setPos(sled.position());
        if (this.startRiding(sled)) {
            //this causes issues
            sled.positionRider(this);
        }
    }

    public BlockState getDisplayState() {
        return displayState;
    }

    public void setContainerItem(ItemStack stack) {
        this.containerStack = stack;
        if (this.containerStack.getItem() instanceof BlockItem blockItem) {
            this.displayState = blockItem.getBlock().defaultBlockState();
        }
        if (stack.getItem() instanceof BlockItem bi) {
            Block block = bi.getBlock();

            if (block instanceof EntityBlock eb && eb.newBlockEntity(BlockPos.ZERO, block.defaultBlockState()) instanceof BaseContainerBlockEntity c) {
                innerBlockEntity = c;
                innerBlockEntity.setLevel(level());
            }
        }
        if (innerBlockEntity == null) {
            throw new IllegalStateException("block {} does not provide a valid container block entity");
        }
        if (isContainerWithNBT(stack) && stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) innerBlockEntity.load(tag);
        }
    }

/*
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return PlatHelper.getEntitySpawnPacket(this);
    }
*/

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeItem(this.containerStack);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.setContainerItem(additionalData.readItem());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setContainerItem(ItemStack.of(tag.getCompound("ContainerItem")));
        if (innerBlockEntity == null) {
            int aaa = 1;
        } else innerBlockEntity.load(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("ContainerItem", this.containerStack.save(new CompoundTag()));
        tag.merge(innerBlockEntity.saveWithoutMetadata());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_HURT, 0);
        this.entityData.define(DATA_ID_DAMAGE, 0.0F);
    }

    @Override
    public double getMyRidingOffset() {
        return 0;
    }

    @Override
    protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
        return pSize.height * 0.5F;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            if (this.isInvulnerableTo(pSource)) {
                return false;
            } else {
                this.setHurtTime(10);
                this.markHurt();
                this.setDamage(this.getDamage() + pAmount * 10.0F);
                this.gameEvent(GameEvent.ENTITY_DAMAGE, pSource.getEntity());
                boolean flag = pSource.getEntity() instanceof Player player && player.getAbilities().instabuild;
                if (flag || this.getDamage() > 15.0F) {
                    this.ejectPassengers();
                    if (flag && !this.hasCustomName()) {
                        this.discard();
                    } else {
                        this.destroy(pSource);
                    }
                }
            }
        }
        return true;
    }

    public void destroy(DamageSource pSource) {
        Level level = this.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            if (!level.isClientSide) {
                Entity entity = pSource.getDirectEntity();
                if (entity != null && entity.getType() == EntityType.PLAYER) {
                    PiglinAi.angerNearbyPiglins((Player) entity, true);
                }
            }
        }
        this.remove(Entity.RemovalReason.KILLED);
    }

    public void spawnDrops() {
        ItemStack stack = this.containerStack.copy();
        if (this.hasCustomName()) {
            stack.setHoverName(this.getCustomName());
        }
        //sacks and shulker. kind of ugly here
        if (isContainerWithNBT(this.containerStack)) {
            stack.addTagElement("BlockEntityTag", innerBlockEntity.saveWithoutMetadata());
        } else {
            Containers.dropContents(this.level(), this, innerBlockEntity);
        }
        this.spawnAtLocation(stack);
    }

    /**
     * Setups the entity to do the hurt animation. Only used by packets in multiplayer.
     */
    @Override
    public void animateHurt(float hurtYaw) {
        this.setHurtTime(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
    }

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick() {
        Entity v = this.getVehicle();
        if (v != null) {

            if (this.getHurtTime() > 0) {
                this.setHurtTime(this.getHurtTime() - 1);
            }

            if (this.getDamage() > 0.0F) {
                this.setDamage(this.getDamage() - 1.0F);
            }

            this.checkBelowWorld();
            this.handleNetherPortal();


            // this.xRotO = v.xRotO;
            // this.yRotO = v.yRotO;
            //this.setYRot(v.getYRot());
            super.tick();
            // this.xRotO = this.getXRot();
            //this.yRotO = this.getYRot();
        } else {
            this.destroy(this.damageSources().generic());
        }
    }

    /**
     * Sets the current amount of damage the minecart has taken. Decreases over time. The cart breaks when this is over
     * 40.
     */
    public void setDamage(float pDamage) {
        this.entityData.set(DATA_ID_DAMAGE, pDamage);
    }

    /**
     * Gets the current amount of damage the minecart has taken. Decreases over time. The cart breaks when this is over
     * 40.
     */
    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE);
    }

    /**
     * Sets the rolling amplitude the cart rolls while being attacked.
     */
    public void setHurtTime(int pRollingAmplitude) {
        this.entityData.set(DATA_ID_HURT, pRollingAmplitude);
    }

    /**
     * Gets the rolling amplitude the cart rolls while being attacked.
     */
    public int getHurtTime() {
        return this.entityData.get(DATA_ID_HURT);
    }

    //@Override
/*
    @PlatformOnly(PlatformOnly.FORGE)
    public ItemStack getPickedResult(HitResult target) {
        return this.containerStack.copy();
    }
*/

    @Override
    public Component getDisplayName() {
        return Component.translatable("message.frostedheart.container_entity_name",
                this.containerStack.getItem().getDescription().getString());
    }

    @Override
    public void remove(Entity.RemovalReason pReason) {
        if (!this.level().isClientSide && pReason.shouldDestroy()) {
            if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                this.spawnDrops();
            }
        }
        super.remove(pReason);
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        InteractionResult ret = super.interact(pPlayer, pHand);
        if (ret.consumesAction()) return ret;
        if (!pPlayer.level().isClientSide) {
            NetworkHooks.openScreen((ServerPlayer) pPlayer, this, b -> {
                b.writeBoolean(false);
                b.writeVarInt(this.getId());
            });
            this.gameEvent(GameEvent.CONTAINER_OPEN, pPlayer);
            PiglinAi.angerNearbyPiglins(pPlayer, true);
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    //TODO: apply slow

    /**
     * from 0 to 1. How much should it slow down the sled
     */
    public float getWeightFromItems() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(innerBlockEntity) / 15f;
    }

    public static boolean isValidContainer(ItemStack stack) {
        return isNormalContainer(stack) || isContainerWithNBT(stack);
    }

    private static boolean isNormalContainer(ItemStack stack) {
        return stack.is(FHTags.Items.SLED_CONTAINER.tag) && stack.getItem() instanceof BlockItem;
    }

    private static boolean isContainerWithNBT(ItemStack stack) {
        Item i = stack.getItem();
        return /*isShulkerBox(i) || isSack(i)*/false;
    }

    private static boolean isShulkerBox(Item i) {
        return i instanceof BlockItem bi && (bi.getBlock() instanceof ShulkerBoxBlock);
    }



    //just delegates to inner container
    @Override
    public int getContainerSize() {
        return innerBlockEntity.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return innerBlockEntity.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return innerBlockEntity.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return innerBlockEntity.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return innerBlockEntity.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        innerBlockEntity.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    //just for this..
    @Override
    public boolean stillValid(Player player) {
        if (this.isRemoved()) {
            return false;
        } else {
            return player.distanceToSqr(this) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        innerBlockEntity.clearContent();
    }

    public void setLootTable(ResourceLocation res, long seed) {
        if (innerBlockEntity instanceof RandomizableContainerBlockEntity r) {
            r.setLootTable(res, seed);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory pPlayerInventory, Player player) {
        //hardcoded since we need to pass this, not the tile...
/*        if (isSack(containerStack.getItem())) {
//            return new SackContainerMenu(id, pPlayerInventory, this);
        } else if (!isNormalContainer(containerStack)) {
            return new ShulkerBoxMenu(id, pPlayerInventory, this);
        }*/
        return ChestMenu.threeRows(id, pPlayerInventory, this);

    }
}
