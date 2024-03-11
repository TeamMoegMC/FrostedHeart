package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import se.mickelus.tetra.blocks.TetraBlock;
import se.mickelus.tetra.blocks.rack.RackBlock;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

import static net.minecraft.block.PlantBlockHelper.isAir;

//矿场基地需要有铁轨连通到矿场，因此不做任何的密封性要求，有个顶就行。
public class MineBaseBlockScanner extends FloorBlockScanner {
    public MineBaseBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
    }
    private final HashSet<BlockPos> rails = new HashSet<>();
    public int area = 0;
    public int volume;
    public int chest = 0;
    public int rack = 0;
    public Set<BlockPos> linkedMines = new HashSet<>();

    @Override
    public boolean isValidFloor(BlockPos pos){
        BlockState state = world.getBlockState(pos);
        if(scanningBlocksNew.contains(pos)) return false;
        if(state.isIn(BlockTags.RAILS)){
            rails.add(pos);
            return false;
        }
        if(!isFloorBlock(pos)){
            return false;
        }
        FHMain.LOGGER.info("isFloorBlock " + pos);
        AbstractMap.SimpleEntry<Integer, Boolean> floorInformation = countBlocksAbove(pos,(pos1)->{
            if(isFloorBlock(pos1)) return true;
            if(world.getBlockState(pos1).isIn(Tags.Blocks.CHESTS)){
                chest++;
                return false;
            }
            if(world.getBlockState(pos1).getBlock().equals(RackBlock.instance)){
                rack++;
                return false;
            }
            return false;
        });
        if(floorInformation.getValue() && floorInformation.getKey() >= 2){
            volume += floorInformation.getKey();
            return true;
        } else return false;
    }

    public boolean scan(){
        this.scan(256, (blockPos) -> {
            area++;
            FHMain.LOGGER.info("Scanning pos: " + blockPos);
            }, BlockScanner.PREDICATE_FALSE);
        if(!this.rails.isEmpty()){
            RailScanner railScanner = new RailScanner();
            railScanner.scan(512, CONSUMER_NULL, PREDICATE_FALSE);
        }
        return this.area > 4 && this.volume > 8 && this.isValid;
    }

    class RailScanner extends FloorBlockScanner{
        public RailScanner(){
            super(MineBaseBlockScanner.this.world, MineBaseBlockScanner.this.rails.iterator().next());
            this.scanningBlocks = MineBaseBlockScanner.this.rails;
        }

        @Override
        public boolean isValidFloor(BlockPos pos){
            if(world.getBlockState(pos).isIn(BlockTags.RAILS)){
                return isAir(world.getBlockState(pos.up()));
            } else if(world.getBlockState(pos).getBlock().equals(FHBlocks.mine.get())){
                linkedMines.add(pos);
            }
            return false;
        }
    }
}
