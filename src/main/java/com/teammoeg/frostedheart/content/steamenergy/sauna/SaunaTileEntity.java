package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.TempEvent;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.NetworkHolder;
import com.teammoeg.frostedheart.util.SerializeUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

public class SaunaTileEntity extends IEBaseTileEntity implements
        INetworkConsumer, ITickableTileEntity, FHBlockInterfaces.IActiveState {

    private static final float POWER_CAP = 400;
    public static final float REFILL_THRESHOLD = 200;
    private static final int RANGE = 3;
    public static final int WALL_HEIGHT = 3;
    public static final Direction[] HORIZONTALS =
            new Direction[] { Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };

    private float power = 0;
    private boolean refilling = false;
    private boolean formed = false;
    Set<BlockPos> floor = new HashSet<>();
    Set<BlockPos> edges = new HashSet<>();
    NetworkHolder network = new NetworkHolder();

    public SaunaTileEntity() {
        super(FHTileTypes.SAUNA.get());
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        power = nbt.getFloat("power");
        refilling = nbt.getBoolean("refilling");
        formed = nbt.getBoolean("formed");
        ListNBT floorNBT = nbt.getList("floor", Constants.NBT.TAG_COMPOUND);
        floor.clear();
        for (int i = 0; i < floorNBT.size(); i++) {
            BlockPos pos = NBTUtil.readBlockPos(floorNBT.getCompound(i));
            floor.add(pos);
        }
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        nbt.putFloat("power", power);
        nbt.putBoolean("refilling", refilling);
        nbt.putBoolean("formed", formed);
        ListNBT floorNBT = new ListNBT();
        for (BlockPos pos : floor) {
            CompoundNBT posNBT = NBTUtil.writeBlockPos(pos);
            floorNBT.add(posNBT);
        }
        nbt.put("floor", floorNBT);
    }

    @Override
    public boolean connect(Direction to, int dist) {
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof EnergyNetworkProvider) {
            network.connect(((EnergyNetworkProvider) te).getNetwork(), dist);
            return true;
        }
        return false;
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return to == Direction.UP;
    }

    @Override
    public NetworkHolder getHolder() {
        return network;
    }

    @Override
    public void tick() {
        // server side logic
        if (!world.isRemote) {
            // power logic
            if (network.isValid()) {
                network.tick();
                // start refill if power is below REFILL_THRESHOLD
                // keep refill until network is full
                if (refilling || power < REFILL_THRESHOLD) {
                    float actual = network.drainHeat(Math.min(200, (POWER_CAP - power) / 0.8F));
                    // during refill, grow power and show steam
                    if (actual > 0) {
                        refilling = true;
                        power += actual * 0.8F;
                        this.setActive(true);
                    }
                    // finished refill, check structure, grant effects, happens every 200 ticks
                    else {
                        formed = structureIsValid();
                        refilling = false;
                        this.setActive(false);
                    }
                    markDirty();
                    this.markContainingBlockForUpdate(null);
                }
                // if not refilling, consume power
                else {
                    power--;
                }
            } else this.setActive(false);

            // grant player effect if structure is valid
            if (formed && power > 0) {
                for (PlayerEntity p : this.getWorld().getPlayers()) {
                    if (floor.contains(p.getPosition().offset(Direction.DOWN))) {
                        p.giveExperiencePoints(10);
                        float currentBodyTemp = TemperatureCore.getBodyTemperature(p);
                        float targetBodyTemp = currentBodyTemp >= 0 ? currentBodyTemp : currentBodyTemp + 0.5F;
                        float targetEnvTemp = 30;
                        TemperatureCore.setTemperature(p, targetBodyTemp, targetEnvTemp);
                    }
                }
            }
        }
        // client side render
        else if (getIsActive()) {
            ClientUtils.spawnSteamParticles(this.getWorld(), pos);
        }
    }

    public ActionResultType onClick(PlayerEntity player) {
        if (!player.world.isRemote) {
            if (formed) {
                player.sendStatusMessage(GuiUtils.translateMessage("structure_formed"), true);
            } else {
                player.sendStatusMessage(GuiUtils.translateMessage("structure_not_formed"), true);
            }
        }
        return ActionResultType.SUCCESS;
    }

    private boolean dist(BlockPos crn, BlockPos orig) {
        return MathHelper.abs(crn.getX() - orig.getX()) <= RANGE && MathHelper.abs(crn.getZ() - orig.getZ()) <= RANGE;
    }

    private void findNext(World l, BlockPos crn, BlockPos orig, Set<BlockPos> poss, Set<BlockPos> edges) {
        if (dist(crn, orig)) {
            if (poss.add(crn)) {
                for (Direction dir : HORIZONTALS) {
                    BlockPos act = crn.offset(dir);
                    // if crn connected to plank
                    if (l.isBlockPresent(act) && (l.getBlockState(act).isIn(BlockTags.PLANKS) || l.getBlockState(act).getBlock().matchesBlock(FHBlocks.sauna))) {
                        findNext(l, act, orig, poss, edges);
                    }
                    // otherwise, crn is an edge block
                    else {
                        edges.add(crn);
                    }
                }
            }
        }
    }

    private boolean structureIsValid() {
        floor.clear();
        edges.clear();
        // collect connected floor and edges
        findNext(this.getWorld(), this.getPos(), this.getPos(), floor, edges);
        // check wall exist for each edge block
        for (BlockPos pos : edges) {
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                BlockState wall = world.getBlockState(pos.offset(Direction.UP, y));
                if (!wall.isIn(BlockTags.PLANKS) && !wall.isIn(BlockTags.DOORS)) {
                    return false;
                }
            }
            // remove edges from the actual floor player can stand on, since play can't stand on wall
            floor.remove(pos);
        }
        // check ceiling exist for each floor block
        for (BlockPos pos : floor) {
            BlockState ceiling = world.getBlockState(pos.offset(Direction.UP, WALL_HEIGHT));
            if (!ceiling.isIn(BlockTags.PLANKS) && !ceiling.isIn(BlockTags.TRAPDOORS)) {
                return false;
            }
        }
        return true;
    }
}
