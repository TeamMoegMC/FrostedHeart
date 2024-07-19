package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.house.HouseBlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.ConfinedSpaceScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlockBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.Objects;

public class HuntingBaseBlockScanner extends HouseBlockScanner {
    private int bedNum = 0;
    private int chestNum = 0;
    private int tanningRackNum = 0;


    public HuntingBaseBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
    }

    public int getBedNum() {
        return bedNum;
    }

    public int getChestNum() {
        return chestNum;
    }

    public int getTanningRackNum() {
        return tanningRackNum;
    }

    protected void addSpecialBlock(BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        addDecoration(pos);
        if(blockState.isIn(BlockTags.BEDS)) bedNum++;
        if(blockState.isIn(Tags.Blocks.CHESTS)) chestNum++;
        if(Objects.requireNonNull(blockState.getBlock().getRegistryName()).getPath().equals("tanning_rack") || blockState.getBlock() instanceof CommandBlockBlock) tanningRackNum++;
    }

    @Override
    public boolean scan() {//想了想还是叫scan更合适
        //第一次扫描，确定地板的位置，并判断是否有露天的地板
        FloorBlockScanner floorBlockScanner = new FloorBlockScanner(world, startPos);
        floorBlockScanner.scan(MAX_SCANNING_TIMES_FLOOR, (pos) -> {
            this.area++;
            this.occupiedArea.add(toColumnPos(pos));
            //FHMain.LOGGER.debug("HouseScanner: scanning floor pos " + pos);
        }, (pos) -> !this.isValid);
        //FHMain.LOGGER.debug("HouseScanner: first scan area: " + area);
        if (this.area < MINIMUM_AREA) this.isValid = false;
        if (!floorBlockScanner.isValid || !this.isValid) return false;
        //FHMain.LOGGER.debug("HouseScanner: first scan completed");

        //第二次扫描，判断房间是否密闭
        ConfinedSpaceScanner airScanner = new ConfinedSpaceScanner(world, startPos.up());
        airScanner.scan(MAX_SCANNING_TIMES_VOLUME, (pos) -> {//对每一个空气方块执行的操作：统计温度、统计体积、统计温度
                    this.temperature += ChunkHeatData.getTemperature(world, pos);
                    this.volume++;
                    this.occupiedArea.add(new ColumnPos(pos.getX(), pos.getZ()));
                    //FHMain.LOGGER.debug("scanning air pos:" + pos);
                }, this::addSpecialBlock,
                (useless) -> !this.isValid);
        temperature /= volume;
        if (this.volume < MINIMUM_VOLUME) this.isValid = false;
        return this.isValid;
    }
}
