package com.teammoeg.frostedheart.content.utility.seld;

import com.google.common.collect.Lists;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Supplier;

public class SledEntity extends Entity {
    //All codes are from snowy spirit
    private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_HURT_DIR = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<String> DATA_ID_TYPE = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SEAT_TYPE = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.INT);

    //varInt is as efficient as byte
    private static final EntityDataAccessor<Byte> DATA_WOLF_INDEX = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Float> DATA_ADDITIONAL_Y = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_SYNCED_DX = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SYNCED_DY = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SYNCED_DZ = SynchedEntityData.defineId(SledEntity.class, EntityDataSerializers.FLOAT);

    private float deltaRotation;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    //client one
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputDown;

    //friction
    private float landFriction;
    private GroundStatus groundStatus;

    //cached references. both updated every tick
    @Nullable
    private Animal sledPuller = null;
    @Nullable
    private ContainerHolderEntity chest = null;

    public SledEntity(EntityType<? extends SledEntity> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
        this.setMaxUpStep(1);
    }

    public SledEntity(Level level, double x, double y, double z) {
        this(FHEntityTypes.SLED.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    /*@Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }*/

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
//        tag.putString("Type", this.getWoodType().toString());
        if (this.getSeatType() != null) {
            tag.putInt("Seat", this.getSeatType().getId());
        }
        if (this.sledPuller != null) {
            tag.putByte("PullerIndex", (byte) getPullerIndex());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
//        if (tag.contains("Type", 8)) {
//            this.setWoodType(WoodTypeRegistry.fromNBT(tag.getString("Type")));
//        }
        if (tag.contains("Seat", 99)) {
            this.setSeatType(DyeColor.byId(tag.getInt("Seat")));
        }
        if (tag.contains("PullerIndex")) {
            this.setPullerIndex(tag.getByte("PullerIndex"));
        }
    }

 /*   @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        //fabric is having issues for some reason... sometimes wood is set sometimes not
        buffer.writeUtf(this.getWoodType().toString());
    }

    //all of this to sync that damn wolf
    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        if (level().isClientSide) {
            SledSoundInstance.playAt(this);
        }
        this.setWoodType(WoodTypeRegistry.fromNBT(additionalData.readUtf()));
    }*/

    @Override
    protected void defineSynchedData() {
//        this.entityData.define(DATA_ID_TYPE, WoodTypeRegistry.OAK_TYPE.toString());
        this.entityData.define(DATA_SEAT_TYPE, 0);
        this.entityData.define(DATA_ID_HURT, 0);
        this.entityData.define(DATA_ID_HURT_DIR, 1);
        this.entityData.define(DATA_ID_DAMAGE, 0.0F);
        this.entityData.define(DATA_WOLF_INDEX, (byte) -1);

        this.entityData.define(DATA_ADDITIONAL_Y, 0.0F);

        this.entityData.define(DATA_SYNCED_DX, 0.0F);
        this.entityData.define(DATA_SYNCED_DY, 0.0F);
        this.entityData.define(DATA_SYNCED_DZ, 0.0F);
    }


    //should control sounds i think
    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return Boat.canVehicleCollide(this, entity);
    }

    //portal stuff
    @Override
    protected Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle rectangle) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, rectangle));
    }

    //boat code here
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Level level = this.level();
            if (!level.isClientSide && !this.isRemoved()) {
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.setDamage(this.getDamage() + amount * 10.0F);
                this.markHurt();
                this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
                boolean isCreative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
                if (isCreative || this.getDamage() > 40.0F) {
                    if (!isCreative && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                        this.spawnAtLocation(this.getSledItem());
                        DyeColor seat = this.getSeatType();
                        if (seat != null) {
                            Item carpet = Items.AIR/*BlocksColorAPI.getColoredItem("carpet", seat)*/;
                            if (carpet != null) this.spawnAtLocation(carpet);
                        }
                        if (this.hasPuller()) {
                            this.spawnAtLocation(Items.LEAD);
                        }
                    }
                    this.discard();
                }
            }
        }
        return true;
    }

    //still boat code
    @Override
    public void animateHurt(float hitYaw) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }

    /**
     * Applies a velocity to the entities, to push them away from eachother.
     */
    @Override
    public void push(Entity pEntity) {
        if (pEntity instanceof Boat) {
            if (pEntity.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.push(pEntity);
            }
        } else if (pEntity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(pEntity);
        }
    }

    /**
     * Sets a target for the client to interpolate towards over the next few ticks
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int posRotationIncrements, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        //ticks it takes to lerp to (10)
        this.lerpSteps = 5;
    }

    /**
     * Gets the horizontal facing direction of this Entity, adjusted to take specially-treated entity types into account.
     */
    @Override
    public Direction getMotionDirection() {
        return this.getDirection().getClockWise();
    }

    //magic slope detection code

    //all values are relative

    public float getAdditionalY() {
        return this.entityData.get(DATA_ADDITIONAL_Y);
    }

    @Nullable
    public void setDataAdditionalY(float additionalY) {
        this.entityData.set(DATA_ADDITIONAL_Y, additionalY);
    }

    //movement coming from another client
    public Vec3 getSyncedMovement() {
        return new Vec3(this.entityData.get(DATA_SYNCED_DX), this.entityData.get(DATA_SYNCED_DY), this.entityData.get(DATA_SYNCED_DZ));
    }

    public void setSyncedMovement(Vec3 deltaMovement) {
        setSyncedMovement((float) deltaMovement.x, (float) deltaMovement.y, (float) deltaMovement.z);
    }

    public void setSyncedMovement(float dx, float dy, float dz) {
        this.entityData.set(DATA_SYNCED_DX, dx);
        this.entityData.set(DATA_SYNCED_DY, dy);
        this.entityData.set(DATA_SYNCED_DZ, dz);
    }

    //public double additionalY = 0;

    public float cachedAdditionalY = 0;
    public double prevAdditionalY = 0;
    //used only for renderer
    public Vec3 projectedPos = Vec3.ZERO;
    public Vec3 prevProjectedPos = Vec3.ZERO;
    public Vec3 prevDeltaMovement = Vec3.ZERO;
    public boolean boost = false;

    //how much movement direction is misaligned from sled direction. determines actual fcition
    public double misalignedFrictionFactor = 1;

    public Vec3 pullerPos = Vec3.ZERO;
    public Vec3 prevPullerPos = Vec3.ZERO;
    public AABB pullerAABB = new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    private final EntityDimensions pullerDimensions = new EntityDimensions(0.8f, 2.1f, false);


    private AABB resetPullerAABB() {
        return this.pullerDimensions.makeBoundingBox(this.position());
    }

    @Override
    public void move(MoverType pType, Vec3 wantedPosIncrement) {

        boolean isMoving = wantedPosIncrement != Vec3.ZERO;


        //old move


        this.prevProjectedPos = this.projectedPos;
        this.projectedPos = Vec3.ZERO;

        //considered on ground even when in air but with block below
        if (!this.onGround() && isMoving) {
            float belowCheck = -1.25f;
            Vec3 blockBelow = this.calculateSlopePosition(new Vec3(0, belowCheck, 0), this.getBoundingBox(), this::makeBoundingBox, -1);
            if (blockBelow.y > belowCheck + 0.01) {
                this.setOnGround(true);
            }
        }


        if (this.onGround()) {

            //this.projectedPos = this.calculateSlopePosition(this.getLookAngle().scale(this.getDeltaMovement().length()).scale(6));

            this.projectedPos = !isMoving ? Vec3.ZERO :
                    this.calculateSlopePosition(this.getDeltaMovement().scale(6),
                            this.getBoundingBox(), this::makeBoundingBox, -1);
            double y = Mth.clamp(this.projectedPos.y, -1, 1);
            if (y == 0) {
                //reset
                this.setXRot(this.getXRot() + -this.getXRot() * 0.3f);
            } else if (y > 0) {
                //up
                this.setXRot((float) Math.max(this.getXRot() - 6f, -30 * y));
            } else {
                //down
                this.setXRot((float) Math.min(this.getXRot() + 3f, -30 * y));
            }
        }

        float localAdditionalY = this.getAdditionalY();
        this.prevAdditionalY = localAdditionalY;
        //if wants to go up raises y
        if (this.projectedPos.y > 0) {
            double slopeIncrement = (projectedPos.y + 0.01) / 2.5d;
            localAdditionalY = (float) Math.min(projectedPos.y, localAdditionalY + slopeIncrement);
        } else {
            //adjust bounding box
            //if (localAdditionalY > 0) {
            //bb set in move
            // this.setBoundingBox(this.makeBoundingBox());
            //}
            localAdditionalY = 0;
        }

        final float snowLayerHeight = 0.0625f;

        //raise when on snow layer
        if (this.groundStatus == GroundStatus.ON_SNOW_LAYER && localAdditionalY < 0.0625) {
            localAdditionalY += snowLayerHeight;
        }

        this.setDataAdditionalY(localAdditionalY);
        this.cachedAdditionalY = localAdditionalY;

        Vec3 oldPos = this.position();

        //bb and pos is set here
        super.move(pType, wantedPosIncrement);


        //reset additionalY when has just stepped up

        if (this.cachedAdditionalY > 0 && oldPos.y < this.getY()) {

            float newHeight = this.groundStatus == GroundStatus.ON_SNOW_LAYER ? snowLayerHeight : 0;
            //adjust bounding box
            this.setDataAdditionalY(newHeight);
            this.cachedAdditionalY = newHeight;
            this.setBoundingBox(this.makeBoundingBox());
        }

        //wolf stuff

        //this is a mess. hacks everywhere
        this.prevPullerPos = this.pullerPos;

        //so wolf can climb up

        if (this.hasPuller()) {
            this.pullerAABB = this.pullerDimensions.makeBoundingBox(this.position().add(0, 0, 0));

            //this is extremely inefficent

            Vec3 wantedPullerPos = this.calculateSlopePosition(wantedPosIncrement.add(this.getLookAngle().scale(2)), this.pullerAABB,
                    this::resetPullerAABB, -1.25f);

            //at most half a block increment is allowed
            double pxInc = Mth.clamp(wantedPullerPos.x - prevPullerPos.x, -0.75, 0.75);
            double pyInc;
            //only slows when going lower than the sled itself. idk why but it overshoots up otherwise and this fixes it
            if (wantedPullerPos.y < 0 && wantedPullerPos.y < prevPullerPos.y) {
                pyInc = Mth.clamp(wantedPullerPos.y - prevPullerPos.y, -0.15, 1);
            } else pyInc = wantedPullerPos.y - prevPullerPos.y;
            double pzInc = Mth.clamp(wantedPullerPos.z - prevPullerPos.z, -0.75, 0.75);
            this.pullerPos = prevPullerPos.add(pxInc, pyInc, pzInc);

            this.pullerAABB = this.pullerDimensions.makeBoundingBox(this.position().add(this.pullerPos));
        }

        //end wolf stuff
    }

    //modified collide method to take into account puller AABB


    @Override
    public void tick() {
        Level level = this.level();
        /*if (!level.isClientSide && boost
                && this.getSyncedMovement().lengthSqr() > 0.09) {
            for (var p : getPassengers()) {
                if (p instanceof ServerPlayer sp) {
                    Utils.awardAdvancement(sp, SnowySpirit.res("adventure/ride_sled_fast"));
                }
            }
        }*/

        if (this.chest != null && chest.isRemoved()) this.chest = null;
        this.updatePuller();

        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        var newStatus = GroundStatus.computeFriction(this);
        this.groundStatus = newStatus.getFirst();
        this.landFriction = newStatus.getSecond();

        //movement stuff start

        this.prevDeltaMovement = this.getDeltaMovement();

        super.tick();
        this.tickLerp();

        Vec3 movement = this.getDeltaMovement();


        //decrese step height with low speed
        // this.maxUpStep = (float) Mth.clamp(speed * 8 + this.additionalY * 1, 0.5, 1);
        this.boost = false;
        //slope deceleration/ acceleration
        //check if on next pos is down or up and if has block relatively near below (ie on ground but with more leeway)
        if (this.projectedPos.y != 0 && this.onGround()) {
            double k = Mth.clamp(this.projectedPos.y, -1, 1);
            if (k > 0) {
                //decelerate uphill if doesnt have wolf
                if (!this.hasPuller())
                    this.setDeltaMovement(movement.scale(1 + -0.06 * k));
            } else {
                //boost downhill
                this.boost = true;
                //gives downward velocity to keep on the slope
                this.setDeltaMovement(movement.add(movement.normalize().scale(k * -0.01f)).add(0, -0.2, 0));
            }
        }

        boolean controlledByLocalInstance = this.isControlledByLocalInstance();
        //local player controlling code

        if (controlledByLocalInstance) {

            this.applyFriction();
            if (level.isClientSide) {
                this.controlSled();
            }

        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        //always move. if movement is zero it still has some calculations to do for wolf and hitbox
        this.move(MoverType.SELF, this.getDeltaMovement());

        this.checkInsideBlocks();

        //interact with nearby entities and adds passengers

        List<Entity> list = level.getEntities(this, this.getBoundingBox().inflate(0.15F, 0.01F, 0.15F),
                EntitySelector.pushableBy(this));

        if (!list.isEmpty()) {
            boolean notLocalPlayerControlled = !level.isClientSide && !(this.getControllingPassenger() instanceof Player);

            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    if (notLocalPlayerControlled && !entity.isPassenger() &&
                            entity.getBbWidth() < this.getBbWidth() &&
                            entity instanceof LivingEntity &&
                            !(entity instanceof WaterAnimal) &&
                            !(entity instanceof Player) &&
                            ((this.hasPuller() && this.canAddPassenger(entity)) || this.getPassengers().size() < 2)) {

                        entity.startRiding(this);
                    } else {
                        this.push(entity);
                    }
                }
            }
        }


        //spawn particles

        if (level.isClientSide) {

            //if it's local player use current movement. otherwise send packet to server which will update all other clients to have syncedDeltaMovement
            if (controlledByLocalInstance) {
                movement = this.getDeltaMovement();
                double horizontalSpeed = movement.x * movement.x + movement.z * movement.z;
                if (horizontalSpeed > 0.001) {
                    //updates same synced data also on client cause u never know
                    this.setSyncedMovement(movement);
                    //send cient movement to other clients
                    FHNetwork.INSTANCE.sendToServer(new FHServerBoundUpdateSledState(movement));
//                    NetworkHandler.CHANNEL.sendToServer(new ServerBoundUpdateSledState(movement));
                    this.spawnTrailParticles(movement, horizontalSpeed);
                }
            } else {
                movement = this.getSyncedMovement();
                double horizontalSpeed = movement.x * movement.x + movement.z * movement.z;
                if (horizontalSpeed > 0.001) {
                    this.spawnTrailParticles(movement, horizontalSpeed);
                    //reset every tick (this might break)
                    // this.setSyncedMovement(Vec3.ZERO);
                }
            }
        } else {
            //resets synced movement
            if (!controlledByLocalInstance) {
                if (this.getSyncedMovement() != Vec3.ZERO) this.setSyncedMovement(Vec3.ZERO);
            }
        }
    }

    //hardcoded to ignore powder snow
    @Override
    protected void checkInsideBlocks() {
        AABB aabb = this.getBoundingBox();
        BlockPos blockpos = BlockPos.containing(aabb.minX + 0.001D, aabb.minY + 0.001D, aabb.minZ + 0.001D);
        BlockPos blockpos1 = BlockPos.containing(aabb.maxX - 0.001D, aabb.maxY - 0.001D, aabb.maxZ - 0.001D);
        Level level = this.level();
        if (level.hasChunksAt(blockpos, blockpos1)) {
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

            for (int i = blockpos.getX(); i <= blockpos1.getX(); ++i) {
                for (int j = blockpos.getY(); j <= blockpos1.getY(); ++j) {
                    for (int k = blockpos.getZ(); k <= blockpos1.getZ(); ++k) {
                        blockPos.set(i, j, k);
                        BlockState blockstate = level.getBlockState(blockPos);
                        if (!(blockstate.getBlock() instanceof PowderSnowBlock)) {
                            try {
                                blockstate.entityInside(level, blockPos, this);
                                this.onInsideBlock(blockstate);
                            } catch (Exception throwable) {
                                CrashReport crashreport = CrashReport.forThrowable(throwable, "Colliding entity with block");
                                CrashReportCategory crashreportcategory = crashreport.addCategory("Block being collided with");
                                CrashReportCategory.populateBlockDetails(crashreportcategory, level, blockPos, blockstate);
                                throw new ReportedException(crashreport);
                            }
                        }
                    }
                }
            }
        }
    }


    private void spawnTrailParticles(Vec3 movement, double horizontalSpeed) {
        if (this.groundStatus.onSnow() && this.onGround()) {
            float xRot = this.getXRot();
            float yRot = this.getYRot();
            Vec3 left = null;
            Vec3 right = null;
            Level level = level();
            if (this.random.nextFloat() * 0.16f < horizontalSpeed) {
                float up = (float) Math.min(horizontalSpeed * 0.6, 0.3);
                Vec3 a = this.calculateViewVector(xRot, yRot + 24);
                Vec3 b = this.calculateViewVector(xRot, yRot - 24);
                left = a.scale(-1f).add(this.position());
                right = b.scale(-1f).add(this.position());

                this.spawnSnowFlakeParticle(level, left,
                        Mth.randomBetween(random, -1.0F, 1.0F) * 0.083,
                        0.015 + random.nextFloat() * 0.1f + up,
                        Mth.randomBetween(random, -1.0F, 1.0F) * 0.083);

                this.spawnSnowFlakeParticle(level, right,
                        Mth.randomBetween(random, -1.0F, 1.0F) * 0.083,
                        0.015 + random.nextFloat() * 0.1f + up,
                        Mth.randomBetween(random, -1.0F, 1.0F) * 0.083);

            }
            if (Math.abs(xRot) < 0.01) {
                Vec3 v = new Vec3(0, 0, 1);
                v = v.yRot((float) (-yRot / 180 * Math.PI));

                double cross = v.cross(new Vec3(movement.x, 0, movement.z).normalize()).y;
                //more particles!
                for (int j = 0; j < 2; j++) {
                    if (random.nextFloat() < (cross * cross) - 0.1) {
                        Vec3 forward = this.calculateViewVector(xRot, yRot);
                        float up = (float) Math.min(horizontalSpeed * 0.6, 0.3);
                        if (cross > 0) {
                            if (left == null) {
                                Vec3 a = this.calculateViewVector(xRot, yRot + 24);
                                left = a.scale(-1f).add(this.position());
                                Vec3 p = left.add(forward.scale(random.nextFloat() * 1.85f));
                                this.spawnSnowFlakeParticle(level, p,
                                        movement.x * 0.75 + forward.x * 0.25,
                                        movement.y + 0.017 + up,
                                        movement.z * 0.75 + forward.z * 0.25);
                            }
                        } else {
                            if (right == null) {
                                Vec3 b = this.calculateViewVector(xRot, yRot - 24);
                                right = b.scale(-1f).add(this.position());
                                Vec3 p = right.add(forward.scale(random.nextFloat() * 1.85f));
                                this.spawnSnowFlakeParticle(level, p,
                                        movement.x * 0.75 + forward.x * 0.25,
                                        movement.y + 0.017 + up,
                                        movement.z * 0.75 + forward.z * 0.25);
                            }
                        }
                    }
                }
            }
        }
    }

    public void spawnSnowFlakeParticle(Level level, Vec3 pos, double dx, double dy, double dz) {
        RandomSource random = level.random;
        level.addParticle(ParticleTypes.SNOWFLAKE,
                pos.x + Mth.randomBetween(random, -0.125F, 0.125F),
                pos.y + 0.2,
                pos.z + Mth.randomBetween(random, -0.125F, 0.125F),
                dx, dy, dz);
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return !this.groundStatus.onSnow() && super.canSpawnSprintParticle();
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        float additionalY = this.getAdditionalY();
        if (additionalY > 0) {
            //is this needed on server side? cant we yeet that additiona y data?
            return super.makeBoundingBox().expandTowards(0, additionalY, 0);
        }
        return super.makeBoundingBox();
    }

    /**
     * Given a motion vector, return an updated vector that takes into account restrictions such as collisions (from all directions) and step-up from stepHeight
     */
   /* @Override
    public Vec3 collide(Vec3 pVec) {
        AABB aabb = this.getBoundingBox();
        Level level = this.level();
        List<VoxelShape> list = new ArrayList<>(level.getEntityCollisions(this, aabb.expandTowards(pVec)));

        double lengthSqr = pVec.lengthSqr();

        //todo: maye re enable. this pretty much disable collisions with wolf at all times since it causes desync and glitchynes when going uphill
        //if (this.hasWolf() && lengthSqr < 0.08) list.add(Shapes.create(this.pullerAABB));

        Vec3 vec3 = lengthSqr == 0.0D ? pVec : collideBoundingBox(this, pVec, aabb, level, list);
        Vec3 vec31 = maybeClimbUp(pVec, aabb, list, vec3);
        if (vec31 != null) return vec31;

        return vec3;
    }*/

    //copied from original collide method
    @Nullable
    private Vec3 maybeClimbUp(Vec3 originalMot, AABB aabb, List<VoxelShape> voxelShapes, Vec3 horizonalMot) {
        boolean restrictedX = originalMot.x != horizonalMot.x;
        boolean restrictedY = originalMot.y != horizonalMot.y;
        boolean restrictedZ = originalMot.z != horizonalMot.z;
        boolean onGround = this.onGround() || restrictedY && originalMot.y < 0.0D;
        float maxUpStep = this.maxUpStep();
        if (maxUpStep > 0.0F && onGround && (restrictedX || restrictedZ)) {
            Level level = this.level();
            Vec3 vec31 = collideBoundingBox(this, new Vec3(originalMot.x, maxUpStep, originalMot.z), aabb, level, voxelShapes);
            Vec3 vec32 = collideBoundingBox(this, new Vec3(0.0D, maxUpStep, 0.0D), aabb.expandTowards(originalMot.x, 0.0D, originalMot.z), level, voxelShapes);
            if (vec32.y < maxUpStep) {
                Vec3 vec33 = collideBoundingBox(this, new Vec3(originalMot.x, 0.0D, originalMot.z), aabb.move(vec32), level, voxelShapes).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalDistanceSqr() > horizonalMot.horizontalDistanceSqr()) {
                return vec31.add(collideBoundingBox(this, new Vec3(0.0D, -vec31.y + originalMot.y, 0.0D), aabb.move(vec31), level, voxelShapes));
            }
        }
        return null;
    }

    /**
     * Modified version of collide. calculates another collision for projected bb to calculate slope
     */
    private Vec3 calculateSlopePosition(Vec3 pVec, AABB aabb, Supplier<AABB> aabbResetter, float maxDownStep) {
        Level level = this.level();
        List<VoxelShape> list = level.getEntityCollisions(this, aabb.expandTowards(pVec));
        Vec3 vec3 = pVec.lengthSqr() == 0.0D ? pVec : collideBoundingBox(this, pVec, aabb, level, list);
        Vec3 vec31 = maybeClimbUp(pVec, aabb, list, vec3);
        if (vec31 != null) return vec31;
        //hack to get down pos
        Vec3 cached = this.position();
        Vec3 newPos = cached.add(vec3);
        this.setPosRaw(newPos.x, newPos.y, newPos.z);
        AABB aa = aabbResetter.get();
        this.setBoundingBox(aa);
        Vec3 down = collideBoundingBox(this, new Vec3(0, maxDownStep, 0), aa, level, list); //getAABB
        this.setPos(cached);
        return vec3.add(down);
    }


    // no clue what's happening here
    private void tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
            //this.setPacketCoordinates(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            double d0 = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
            double d1 = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
            double d2 = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
            double d3 = Mth.wrapDegrees(this.lerpYRot - this.getYRot());
            this.setYRot(this.getYRot() + (float) d3 / this.lerpSteps);
            this.setXRot(this.getXRot() + (float) (this.lerpXRot - this.getXRot()) / this.lerpSteps);
            --this.lerpSteps;
            this.setPos(d0, d1, d2);
            this.setRot(this.getYRot(), this.getXRot());
        }
    }

    public GroundStatus getCurrentStatus() {
        return this.groundStatus;
    }

    private void applyFriction() {
        double gravity = this.isNoGravity() ? 0.0D : (double) -0.04F;

        float invFriction = 0.05F;

        switch (this.groundStatus) {
            case IN_AIR -> invFriction = 0.9F;
            case IN_WATER -> invFriction = 0.45f;
            case ON_SNOW, ON_SNOW_LAYER, ON_LAND -> {
                invFriction = this.landFriction;
                if (this.getControllingPassenger() instanceof Player) {
                    this.landFriction /= 2.0F;
                }
            }
        }


        Vec3 movement = this.getDeltaMovement();

        //alters friction when not facing the right way. allows braking
        if (this.groundStatus.touchingGround()) {
            //max friction decrement cause by misaligned speed vector
            double inc = 0.825;
            if (this.inputUp || this.inputDown || movement.lengthSqr() > 0.001) {
                Vec3 v = new Vec3(0, 0, 1);
                v = v.yRot((float) ((-this.getYRot()) / 180 * Math.PI));

                double dot = v.dot(new Vec3(movement.x, 0, movement.z).normalize());
                inc = Mth.clamp(((dot + 3) / 4f) + 0.005, inc, 1);
            }
            //this is only visual
            this.misalignedFrictionFactor = (inc * 4 - 3);
            invFriction *= inc;
        }

        this.setDeltaMovement(movement.x * invFriction, movement.y + gravity, movement.z * invFriction);
        //rotation friction
        //increase rotation friction when going forward. Turning is hard!
        this.deltaRotation *= Math.min(invFriction, (this.inputUp ? /*rotation_slipperiness_on_forward_acceleration*/0.75 : /*rotation_slipperiness*/0.92));
    }

    private void controlSled() {
        if (this.isVehicle()) {
            float powah = 0.0F;
            Vec3 movement = this.getDeltaMovement();

            boolean canSteer = !(this.inputRight && this.inputLeft) && this.inputUp;
            boolean hasWolf = this.hasPuller();
            final double steerFactor = hasWolf ? /*steer_factor_with_wolf*/0.067 : /*steer_factor*/0.042;

            if (this.inputLeft) {
                --this.deltaRotation;
                //crappy steering
                if (this.groundStatus.touchingGround() && canSteer) {
                    Vec3 v = new Vec3(0, 0, 1);
                    v = v.yRot((float) ((-this.getYRot()) / 180 * Math.PI));

                    double dot = v.dot(movement.normalize());
                    if (dot > 0) {
                        this.setDeltaMovement(movement.yRot((float) (dot * steerFactor)));
                    }
                }
            }

            if (this.inputRight) {
                ++this.deltaRotation;
                //steering
                if (this.groundStatus.touchingGround() && canSteer) {
                    Vec3 v = new Vec3(0, 0, 1);
                    v = v.yRot((float) ((-this.getYRot()) / 180 * Math.PI));

                    double dot = v.dot(movement.normalize());
                    if (dot > 0.8) {
                        this.setDeltaMovement(movement.yRot((float) (-dot * steerFactor)));
                    }
                }
            }

            //side acceleration
            if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
                //backwards_acceleration
                powah += 0.005;
            }

            this.setYRot(this.getYRot() + this.deltaRotation);
            if (this.inputUp) {
                if (this.groundStatus.onSnow()) {
                    //forward_acceleration_with_wolf  ,  forward_acceleration
                    double acceleration = hasWolf ? 0.017 : 0.015;
                    powah += acceleration;//0.04F;
                } else //forward_acceleration_when_not_on_snow
                    powah += 0.03700000047683716;
            }

            //brake straight when pressing down
            if (this.inputDown) {
                //backwards_acceleration
                powah -= 0.005;
            }


            this.setDeltaMovement(this.getDeltaMovement().add(
                    Mth.sin(-this.getYRot() * ((float) Math.PI / 180F)) * powah,
                    0.0D,
                    Mth.cos(this.getYRot() * ((float) Math.PI / 180F)) * powah));

        }
    }

    protected void clampRotation(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -105.0F, 105.0F);
        entity.yRotO += f1 - f;
        entity.setYRot(entity.getYRot() + f1 - f);
        entity.setYHeadRot(entity.getYRot());
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {

        Level level = this.level();
        if (level.isClientSide && this.fallDistance > 0.5 && this.onGround()) {
            if (this.groundStatus.onSnow()) {
                float p = Mth.clamp(this.fallDistance * 4f, 5, 20);
                Vec3 front = this.position().add(this.getLookAngle().scale(0.8f));
                Vec3 mov = this.getDeltaMovement().scale(1.1);
                float ySpeed = (float) (mov.lengthSqr() * 0.06 + this.fallDistance * 0.005f);
                for (int i = 0; i < p; i++) {

                    level.addParticle(ParticleTypes.SNOWFLAKE,
                            front.x + Mth.randomBetween(random, -0.6F, 0.6F),
                            front.y + 0.2 + Mth.randomBetween(random, -0.1F, 0.2F),
                            front.z + Mth.randomBetween(random, -0.6F, 0.6F),
                            mov.x + Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F,
                            0.1 + random.nextFloat() * ySpeed,
                            mov.z + Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F);
                }
            }
        }
        //super code
        if (pOnGround) {
            if (this.fallDistance > 0.0F) {
                //pState.getBlock().fallOn(this.level, pState, pPos, this, this.fallDistance);
                //if (!pState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                //    this.gameEvent(GameEvent.HIT_GROUND);
                //}
            }

            this.resetFallDistance();
        } else if (pY < 0.0D) {
            this.fallDistance = (float) (this.fallDistance - pY);
        }
    }

    public void setDamage(float v) {
        this.entityData.set(DATA_ID_DAMAGE, v);
    }

    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE);
    }

    public void setHurtTime(int i) {
        this.entityData.set(DATA_ID_HURT, i);
    }

    public int getHurtTime() {
        return this.entityData.get(DATA_ID_HURT);
    }

    public void setPullerIndex(int i) {
        this.entityData.set(DATA_WOLF_INDEX, (byte) i);
    }

    public int getPullerIndex() {
        return this.entityData.get(DATA_WOLF_INDEX);
    }

    public void setHurtDir(int i) {
        this.entityData.set(DATA_ID_HURT_DIR, i);
    }

    public int getHurtDir() {
        return this.entityData.get(DATA_ID_HURT_DIR);
    }

    public void setWoodType(WoodType type) {
        this.entityData.set(DATA_ID_TYPE, type.toString());
    }

    /*public WoodType getWoodType() {
        return WoodTypeRegistry.fromNBT(this.entityData.get(DATA_ID_TYPE));
    }*/

    @Nullable
    public DyeColor getSeatType() {
        int d = this.entityData.get(DATA_SEAT_TYPE);
        if (d == 0) return null;
        return DyeColor.byId(d - 1);
    }

    @Nullable
    public void setSeatType(@Nullable DyeColor seatColor) {
        this.entityData.set(DATA_SEAT_TYPE, seatColor == null ? 0 : seatColor.getId() + 1);
    }

    //----passenger stuff-----

    @Override
    public double getPassengersRidingOffset() {
        return 0.125D + this.getAdditionalY() + (this.getSeatType() != null ? 0.0615 : 0);
    }

    @Nullable
    public ContainerHolderEntity tryAddingChest(ItemStack stack) {
        if (ContainerHolderEntity.isValidContainer(stack) && this.canAddChest()) {
            Level level = level();
            stack = stack.split(1);
            ContainerHolderEntity container = new ContainerHolderEntity(level, this, stack);
            level.addFreshEntity(container);
            Block b = ((BlockItem) stack.getItem()).getBlock();
            this.playSound(b.getSoundType(b.defaultBlockState()).getPlaceSound());
            return container;
        }
        return null;
    }

    //TOdo:cleanup
    //this is only called by one player on server so any state change needs to be notified
    @Override
    public InteractionResult interact(Player player, InteractionHand pHand) {
        if (!player.isSecondaryUseActive()) {
            ItemStack stack = player.getItemInHand(pHand);
            //carpet
            Level level = player.level();
            if (stack.is(ItemTags.WOOL_CARPETS) && this.getSeatType() == null) {
                DyeColor col = DyeColor.WHITE;
                if (col != null) {
                    this.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 1.0F);
                    this.setSeatType(col);
                    stack.shrink(1);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            } else if (this.tryAddingChest(stack) != null) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!this.hasPuller()) {
                double radius = 7.0D;
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                Mob found = null;

                for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(x - radius, y - radius, z - radius,
                        x + radius, y + radius, z + radius))) {
                    if (mob.getLeashHolder() == player) {
                        found = mob;
                        break;
                    }
                }
                if (found != null) {
                    //discards non-owned animals
                    boolean owned = (found instanceof TamableAnimal ta && ta.getOwner() == player) /*||
                            found instanceof Fox fox && fox.trusts(player.getUUID())*/;
                    if (owned && this.tryConnectingPuller(found)) {
                        this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
                        /*if (this.chest != null && player instanceof ServerPlayer sp) {
                            Utils.awardAdvancement(sp,SnowySpirit.res( "adventure/sled_with_wolf"));
                        }*/
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                    return InteractionResult.FAIL;
                }
            }
            if (!level.isClientSide) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            } else {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(Entity entity) {
        if (this.isEyeInFluid(FluidTags.WATER)) return false;

        int maxAllowed = this.getMaxPassengersSize();

        if (this.getPassengers().size() >= maxAllowed) return false;
        //has space
        return !hasChest() || !(entity instanceof ContainerHolderEntity);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        var v = this.getFirstPassenger();
        return v instanceof LivingEntity le ? le : null;
    }


//    @Override
    public void onInputUpdate(boolean left, boolean right, boolean up, boolean down, boolean sprint, boolean jumping) {
        this.inputLeft = left;
        this.inputRight = right;
        this.inputUp = up;
        this.inputDown = down;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        // Forge: Fix MC-119811 by instantly completing lerp on board (boat code)
        if (this.isControlledByLocalInstance() && this.lerpSteps > 0) {
            this.lerpSteps = 0;
            this.absMoveTo(this.lerpX, this.lerpY, this.lerpZ, (float) this.lerpYRot, (float) this.lerpXRot);
        }
        updatePullerIndex();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide && DATA_WOLF_INDEX.equals(key)) {
            int ind = getPullerIndex();
            //on client accepts possible wolf. update immediately, prevents 1 tick delay since we update on tick
            if (ind != -1) {
                if (this.getPassengers().size() > ind) {
                    Entity wolf = this.getPassengers().get(ind);
                    this.tryConnectingPuller(wolf);
                }
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction setPos) {
        if (this.hasPassenger(passenger)) {

            //can only have 1 chest so the rider that is chest is THE chest
            if (this.chest == null && passenger instanceof ContainerHolderEntity container) {
                this.chest = container;
            }

            if (this.isMyPuller(passenger)) {
                Animal animal = (Animal) passenger;
                passenger.setYRot(passenger.getYRot() + this.deltaRotation);
                this.clampRotation(passenger);
                passenger.setYBodyRot(animal.yBodyRot + this.deltaRotation * 10);
                passenger.setYHeadRot(animal.yBodyRot);
                //powder snow check here
                setPos.accept(passenger, this.getX() + pullerPos.x, this.getY() + pullerPos.y, this.getZ() + pullerPos.z);

                this.updatePullerAnimations();
            } else {
                float zPos = 0.1F;
                float yPos = (float) ((this.isRemoved() ? 0.01 : this.getPassengersRidingOffset()) + passenger.getMyRidingOffset());

                boolean isMoreThanOneOnBoard = false;
                if (this.isChestEntity(passenger)) {

                    passenger.xRotO = this.xRotO;
                    passenger.setXRot(this.getXRot());
                    passenger.yRotO = this.yRotO;
                    passenger.setYRot(this.getYRot());

                    //passenger.yRotO = this.yRotO;
                    zPos = -0.4f;
//                    yPos += 0.3;
                    float cos = Mth.sin((float) (this.getXRot() * Math.PI / 180f));
//                    yPos -= cos * zPos;

                } else {

                    //this is an utter mess
                    isMoreThanOneOnBoard = this.getPassengers().size() > this.getMaxPassengersSize() - 1;
                    if (isMoreThanOneOnBoard) {
                        int i = 0;
                        for (Entity p : this.getPassengers()) {
                            if (p == passenger) break;
                            if (!isMyPuller(p) && !isChestEntity(p)) i++;
                        }

                        float cos = Mth.sin((float) (this.getXRot() * Math.PI / 180f));
                        if (i == 0) {
                            zPos = 0.1F;
                        } else {
                            zPos = -0.8F;
                        }
                        yPos -= cos * zPos;
                    }

                    if (passenger instanceof Animal) {
                        if (isMoreThanOneOnBoard) {
                            zPos += 0.2D;
                        }
                        yPos += 0.125;
                    }
                    passenger.setYRot(passenger.getYRot() + this.deltaRotation);
                    passenger.setYHeadRot(passenger.getYHeadRot() + this.deltaRotation);
                    this.clampRotation(passenger);
                }
                Vec3 vec3 = (new Vec3(zPos, 0.0D, 0.0D)).yRot(-this.getYRot() * ((float) Math.PI / 180F) - ((float) Math.PI / 2F));
                setPos.accept(passenger, this.getX() + vec3.x, this.getY() + yPos, this.getZ() + vec3.z);


                if (passenger instanceof Animal animal && isMoreThanOneOnBoard) {
                    int yRot = passenger.getId() % 2 == 0 ? 90 : 270;
                    passenger.setYBodyRot(animal.yBodyRot + yRot);
                    passenger.setYHeadRot(passenger.getYHeadRot() + yRot);
                }
            }
        }
    }

    @Override
    public void dismountTo(double pX, double pY, double pZ) {
        this.setDataAdditionalY(0);
        this.projectedPos = Vec3.ZERO;
        super.dismountTo(pX, pY, pZ);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity entity) {
        Vec3 vec3 = getCollisionHorizontalEscapeVector(this.getBbWidth() * Mth.SQRT_OF_TWO, entity.getBbWidth(), entity.getYRot());
        double d0 = this.getX() + vec3.x;
        double d1 = this.getZ() + vec3.z;
        BlockPos blockpos = BlockPos.containing(d0, this.getBoundingBox().maxY, d1);
        BlockPos below = blockpos.below();
        Level level = this.level();
        if (!level.isWaterAt(below)) {
            List<Vec3> list = Lists.newArrayList();
            double d2 = level.getBlockFloorHeight(blockpos);
            if (DismountHelper.isBlockFloorValid(d2)) {
                list.add(new Vec3(d0, blockpos.getY() + d2, d1));
            }

            double d3 = level.getBlockFloorHeight(below);
            if (DismountHelper.isBlockFloorValid(d3)) {
                list.add(new Vec3(d0, below.getY() + d3, d1));
            }

            for (Pose pose : entity.getDismountPoses()) {
                for (Vec3 vec31 : list) {
                    if (DismountHelper.canDismountTo(level, vec31, entity, pose)) {
                        entity.setPose(pose);
                        return vec31;
                    }
                }
            }
        }
        return super.getDismountLocationForPassenger(entity);
    }

    @Override
    public void ejectPassengers() {
        if (this.sledPuller != null) this.disconnectPuller();
        this.chest = null;
        super.ejectPassengers();
    }

    @Override
    protected void removePassenger(Entity pPassenger) {
        if (this.sledPuller == pPassenger) this.disconnectPuller();
        if (this.chest == pPassenger) this.chest = null;
        super.removePassenger(pPassenger);
        updatePullerIndex();
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        this.clampRotation(entity);
    }

    //----end passenger stuff-----

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(this.getSledItem());
    }

    public Item getSledItem() {
        return FHItems.SLED.get();
    }

    //if it can prevent freezing
    public boolean isComfy() {
        return true;
    }


    public boolean isChestEntity(Entity entity) {
        return entity == this.chest;
    }

    public boolean hasChest() {
        return this.chest != null;
    }

    //wolf towing (god help me)

    public boolean hasPuller() {
        return this.sledPuller != null;
    }

    public boolean isMyPuller(Entity entity) {
        return entity == this.sledPuller;
    }

    @Nullable
    public Animal getSledPuller() {
        return sledPuller;
    }

    //on server checks if this entity is a valid wold. on client its lenient and simply casts to animal
    //only called when adding from leash as this can fail
    public boolean tryConnectingPuller(Entity entity) {
        if (entity instanceof Animal wolf) {
            if (entity.level().isClientSide) {
                this.sledPuller = wolf;
                return true;

            } else if (entity.getType().is(FHTags.FHEntityTags.SLED_PULLERS.tag) && entity.getBbWidth() < /*max_sled_puller_size*/1.25) {
                //serverside logic
                //need to remove leash, or he'll drop it itself
                wolf.dropLeash(true, false);
                if (wolf.startRiding(this) && this.hasPassenger(wolf)) {
                    //update wolf on other clients.
                    setPullerIndex((byte) this.getPassengers().indexOf(wolf));
                    this.sledPuller = wolf;
                    return true;
                } else {
                    wolf.spawnAtLocation(Items.LEAD);
                    return false;
                }
            }
        }
        return false;
    }

    public void updatePuller() {
        int ind = this.getPullerIndex();

        //validate
        if (this.sledPuller != null) {
            if (sledPuller.isRemoved()) {
                this.sledPuller = null;
                this.setPullerIndex(-1);
                return;
            }
            if (ind == -1) {
                sledPuller = null;
                return;
            }
            this.sledPuller.setInvulnerable(true);

            //validate current one
            if (this.getPassengers().size() > ind) {
                Entity wolf = this.getPassengers().get(ind);
                if (wolf != sledPuller) {
                    //if it changed we remove directly
                    sledPuller = null;
                    this.setPullerIndex(-1);
                }
            }

            //if this is false it means pullers is yet to be received
        } else if (ind >= 0 && this.getPassengers().size() > ind) {
            Entity wolf = this.getPassengers().get(ind);
            if (wolf instanceof Animal a) {
                this.sledPuller = a;
            }
        }
    }

    public void disconnectPuller() {
        if (this.sledPuller != null) {
            if (!this.level().isClientSide) {
                this.setPullerIndex(-1);
                if (this.sledPuller instanceof TamableAnimal tamableAnimal) {
                    tamableAnimal.setInSittingPose(false);
                } else if (this.sledPuller instanceof Fox fox) {
                    fox.setSitting(false);
                }
                this.sledPuller.setInvulnerable(false);
                this.sledPuller = null;
                //this is bad... calling on server side only
            }
        }
    }

    protected void updatePullerIndex() {
        //fixes up wolf index if passenger list changed
        for (var p : this.getPassengers()) {
            if (p == sledPuller) {
                this.setPullerIndex(this.getPassengers().indexOf(p));
            }
        }
    }

    //i need this because wolf does update its own...
    private final WalkAnimationState internalPullerAnimation = new WalkAnimationState();

    protected void updatePullerAnimations() {
        if (this.sledPuller != null) {
            double travelX = sledPuller.getX() - sledPuller.xo;
            double travelY = 0.0D;
            double travelZ = sledPuller.getZ() - sledPuller.zo;
            float speed = (float) Mth.length(travelX, travelY, travelZ) * 4.0F;
            if (speed > 1.0F) {
                speed = 1.0F;
            }
            //sets the 2 equal. dont ask why, updating it normally causes jitter. not updating it makes it not animate...
            internalPullerAnimation.update(speed, 0.4f);
            this.sledPuller.walkAnimation.setSpeed(internalPullerAnimation.speed());
//            this.sledPuller.walkAnimation.position = internalPullerAnimation.position;
//            this.sledPuller.walkAnimation.speedOld = internalPullerAnimation.speedOld;
            Vec3 m = this.isControlledByLocalInstance() ? this.getDeltaMovement() : this.getSyncedMovement();
            boolean sit = m.lengthSqr() < 0.00001;
            if (this.sledPuller instanceof TamableAnimal tamableAnimal) {
                if (tamableAnimal.isInSittingPose() != sit) {
                    tamableAnimal.setInSittingPose(sit);
                }
            } else if (this.sledPuller instanceof Fox fox) {
                if (fox.isSitting() != sit)
                    fox.setSitting(sit);
            }
        }
    }

    //chest madness
    private boolean canAddChest() {
        return this.getPassengers().size() < this.getMaxPassengersSize() && !this.hasChest();
    }

    private int getMaxPassengersSize() {
        return this.hasPuller() ? 3 : 2;
    }

    //precaution so it's always immune to powder snow if the mixin fails to apply
    @Override
    public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
        if (pState.is(FHTags.Blocks.SLED_SNOW.tag) || pState.getBlock() instanceof SnowLayerBlock) return;
        super.makeStuckInBlock(pState, pMotionMultiplier);
    }
}

