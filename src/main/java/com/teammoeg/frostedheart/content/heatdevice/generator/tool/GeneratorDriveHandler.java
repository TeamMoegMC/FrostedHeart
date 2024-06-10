package com.teammoeg.frostedheart.content.heatdevice.generator.tool;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.stone.AlloySmelterTileEntity;
import blusunrize.immersiveengineering.common.blocks.stone.BlastFurnaceTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.content.heatdevice.generator.t1.T1GeneratorTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GeneratorDriveHandler {
    protected World world;

    T1GeneratorTileEntity T1master;

    public BlockPos lastSupportPos;

    public GeneratorDriveHandler(World world, T1GeneratorTileEntity T1master, BlockPos lastSupportPos) {
        this.world = world;
        this.T1master = T1master;
        this.lastSupportPos = lastSupportPos;
    }

    public boolean isExistNeighborTileEntity() {
        Vector3i vec = T1master.getMultiblockInstance().getSize(world);
        Set<Vector3i> hasCheckMaster = new HashSet<Vector3i>();
        int xLow = -1, xHigh = vec.getX(), yLow = 0, yHigh = vec.getY(), zLow = -1, zHigh = vec.getZ();
        for (int x = xLow; x <= xHigh; x++) {
            for (int z = zLow; z <= zHigh; z++) {
                if (!((x > xLow && x < xHigh && z > zLow && z < zHigh) || (x == xLow && z == zLow) || (x == xLow && z == zHigh) || (x == xHigh && z == zLow) || (x == xHigh && z == zHigh))) {
                    for (int y = yLow; y < yHigh; y++) {
                        /** Enum a seamless NoUpandDown hollow cube */
                        BlockPos actualPos = T1master.getBlockPosForPos(new BlockPos(x, y, z));
                        TileEntity te = Utils.getExistingTileEntity(this.world, actualPos);
                        /* why /(ㄒoㄒ)/~~ */
                        if (te instanceof BlastFurnaceTileEntity) {
                            BlastFurnaceTileEntity blastFurnaceMaster = ((BlastFurnaceTileEntity) te).master();
                            BlockPos actualMasterPos = blastFurnaceMaster.getBlockPosForPos(blastFurnaceMaster.offsetToMaster);
                            if (!hasCheckMaster.contains(actualMasterPos)) {
                                CheckEachBlock checkEachBlock = new CheckEachBlock(blastFurnaceMaster, T1master.getBlockPosForPos(new BlockPos(1,0,1)));
                                hasCheckMaster.add(actualMasterPos);
                                if (checkEachBlock.isEachBlastFurnaceBlockInScope() ) {
                                    if (updateBlastFurnace(blastFurnaceMaster, actualMasterPos)) return true;
                                }
                            }
                        }
                        if (te instanceof AlloySmelterTileEntity) {
                            AlloySmelterTileEntity alloySmelterMaster = ((AlloySmelterTileEntity) te).master();
                            BlockPos actualMasterPos = alloySmelterMaster.getBlockPosForPos(alloySmelterMaster.offsetToMaster);
                            if (!hasCheckMaster.contains(actualMasterPos)) {
                                CheckEachBlock checkEachBlock = new CheckEachBlock(alloySmelterMaster, T1master.getBlockPosForPos(new BlockPos(1,0,1)));
                                hasCheckMaster.add(actualMasterPos);
                                if (checkEachBlock.isEachAlloySmelterBlockInScope() ) {
                                    if (updateAlloySmelter(alloySmelterMaster, actualMasterPos)) return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    public boolean hasSupported () {
        TileEntity te = Utils.getExistingTileEntity(this.world, this.lastSupportPos);
        if (te instanceof BlastFurnaceTileEntity) {
            if (((BlastFurnaceTileEntity) te).getInventory().get(0).isEmpty() ) {
                BlastFurnaceTileEntity blastFurnaceMaster = ((BlastFurnaceTileEntity) te).master();
                if (updateBlastFurnace(blastFurnaceMaster, this.lastSupportPos)) return true;
            }
        }
        else if (te instanceof AlloySmelterTileEntity) {
            if (((AlloySmelterTileEntity) te).getInventory().get(0).isEmpty() ) {
                AlloySmelterTileEntity alloySmelterMaster = ((AlloySmelterTileEntity) te).master();
                if (updateAlloySmelter(alloySmelterMaster, this.lastSupportPos)) return true;
            }
        }
        return false;
    }
    protected boolean updateBlastFurnace(BlastFurnaceTileEntity blastFurnaceTileEntity, BlockPos lastSupportPos) {
        if (! blastFurnaceTileEntity.getInventory().get(0).isEmpty()) {
            blastFurnaceTileEntity.burnTime = 1600;
            this.lastSupportPos = lastSupportPos;
            return true;
        }
        else {
            this.lastSupportPos = new BlockPos(0,0,0);
            return false;
        }
    }
    protected boolean updateAlloySmelter(AlloySmelterTileEntity alloySmelterTileEntity, BlockPos lastSupportPos) {
        if (! alloySmelterTileEntity.getInventory().get(0).isEmpty()) {
            alloySmelterTileEntity.burnTime = 1600;
            this.lastSupportPos = lastSupportPos;
            return true;
        }
        else {
            this.lastSupportPos = new BlockPos(0,0,0);
            return false;
        }
    }
    protected class CheckEachBlock {
        protected boolean allBlockInScope = true;
        protected MultiblockPartTileEntity actualMaster;
        protected BlockPos T1MasterPos;
        BlockPos size;
        public CheckEachBlock (MultiblockPartTileEntity actualMaster, BlockPos T1MasterPos) {
            this.actualMaster = actualMaster;
            this.T1MasterPos = T1MasterPos;
        }
        public boolean isEachBlastFurnaceBlockInScope() {
            this.size = new BlockPos(3,3,3);
            forEachBlock(this::checkWithScope);
            return this.allBlockInScope;
        }
        public boolean isEachAlloySmelterBlockInScope() {
            this.size = new BlockPos(2,2,2);
            forEachBlock(this::checkWithScope);
            return this.allBlockInScope;
        }
        protected void checkWithScope(BlockPos offset) {
            int Scope = Math.max(this.size.getX(), this.size.getY());
            BlockPos actualMasterPos = actualMaster.getBlockPosForPos(offset);
            int distanceX = Math.abs( actualMasterPos.getX() -  T1MasterPos.getX());
            int distanceY = actualMasterPos.getY() -  T1MasterPos.getY();
            int distanceZ = Math.abs( actualMasterPos.getZ() -  T1MasterPos.getZ());
            if ( (distanceY>=0 && distanceY<=3) && (
                    (distanceX>=0&&distanceX<=1 && distanceZ>=2&&distanceZ<=Scope+1) || (distanceZ>=0&&distanceZ<=1 && distanceX>=2&&distanceX<=Scope+1)
            ) ) {
                ;
            }
            else {
                allBlockInScope = false;
                return;
            }
        }
        protected void forEachBlock(Consumer<BlockPos> consumer) {
            for (int x = 0; x < this.size.getX(); ++x)
                for (int y = 0; y < this.size.getY(); ++y)
                    for (int z = 0; z < this.size.getZ(); ++z) {
                        consumer.accept(new BlockPos(x, y, z));
                    }
        }
    }
}