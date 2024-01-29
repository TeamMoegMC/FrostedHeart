package com.teammoeg.frostedheart.town.Farm;

import com.ibm.icu.impl.Pair;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.scheduler.IScheduledTaskTE;
import com.teammoeg.frostedheart.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.town.ITownBlockTE;
import com.teammoeg.frostedheart.town.TownWorkerType;
import io.netty.handler.codec.sctp.SctpOutboundByteStreamHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.Tags;

import java.util.*;


public class FarmBlockTileEntity extends TileEntity implements ITownBlockTE, IScheduledTaskTE, ITickableTileEntity {
    private boolean isAdd;
    public static int MAX_SIZE = 1000;
    public int size;//Size of the farm
    public int temperature;
    public Map<Long, BlockPos> blocks;
    public FarmBlockTileEntity() {
        super(FHTileTypes.FARM.get());
        this.isAdd = false;
        this.blocks = new HashMap<>();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT data = new CompoundNBT();
        data.putInt("size", size);
        data.putInt("temperature", temperature);
        return data;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.FARM;
    }

    @Override
    public boolean isWorkValid() {
        return checkFarm();
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        size = data.getInt("size");
        temperature = data.getInt("temperature");
    }

    private boolean checkFarm(){
        Queue<BlockPos> queue = new PriorityQueue<>();
        queue.add(this.pos.add(0, -1, 0));
        blocks.put(this.pos.add(0, -1, 0).toLong(), this.pos.add(0, -1, 0));
        BlockPos tempPos;
        List<Pair<Integer, Integer>> py = Arrays.asList(Pair.of(1, 0), Pair.of(-1, 0), Pair.of(0, 1), Pair.of(0, -1));
        while (!queue.isEmpty()){
            if(blocks.size() > MAX_SIZE)break;
            tempPos = queue.poll();
            for (Pair<Integer, Integer> p : py) {
                if(true){
                    if(world.isAirBlock(tempPos.add(p.first, 1, p.second))){
                        BlockPos pos = tempPos.add(p.first, 0, p.second);
                        Long key = pos.toLong();
                        if(!blocks.containsKey(key)){
                            queue.add(pos);
                            blocks.put(key, pos);
                            world.updateBlock(pos, Blocks.STONE);
                        }
                    }
                }
            }
        }
        if(blocks.size() >= MAX_SIZE){
            blocks.clear();
        }else{
            blocks.clear();
            return true;
        }
        return false;
    }

    public ActionResultType onClick(PlayerEntity pe, ItemStack is){
        if(is != null){
            System.out.println("Farm Clicked");
            if(this.checkFarm()){
                System.out.println("Complete");
            }else{
                System.out.println("UnComplete");
            }
        }
        return ActionResultType.PASS;
    }

    @Override
    public void executeTask() {
        if(true){
            //System.out.println("Working");
        }
    }

    @Override
    public boolean isStillValid() {
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void tick() {
        if(!isAdd){
            isAdd = true;
            SchedulerQueue.add(this);
        }
    }
}
