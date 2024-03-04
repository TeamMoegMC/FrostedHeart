package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.util.BlockScanner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.AbstractMap;

import static net.minecraft.block.PlantBlockHelper.isAir;

//仓库的结构不考虑密封，体积仅统计地板上方的空气。
public class WarehouseBlockScanner extends BlockScanner.FloorBlockScanner {
    private int volume = 0;
    private int area = 0;
    private static final int MAX_SCANNING_TIMES = 512;

    public WarehouseBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
    }

    public int getVolume(){return this.volume;}
    public int getArea(){return this.area;}

    @Override
    public boolean isValidFloor(BlockPos pos){
        return isFloorBlock(pos) && isAirOrLadder(world, pos.up()) && isAirOrLadder(world, pos.up(2));
    }

    public boolean scan(){
        return scan(MAX_SCANNING_TIMES, (pos1)->{
            this.area++;
            AbstractMap.SimpleEntry<Integer, Boolean> floorInformation = countBlocksAbove(pos1, (pos2)->!isAir(world.getBlockState(pos2)));
            if(!floorInformation.getValue()) this.isValid=false;
            this.volume += floorInformation.getKey();
        },(useless)->false);
    }
}
