package com.teammoeg.frostedheart.town.Farm;

import com.alcatrazescapee.primalwinter.common.ModBlocks;
import com.ibm.icu.impl.Pair;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.ClientUtils;
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
    public static int MAX_SIZE = 576;
    public static int MIN_SIZE = 4;
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
        Map<Long, BlockPos> blockMap = new HashMap<>();
        queue.add(this.pos.add(0, -1, 0));
        blockMap.put(this.pos.add(0, -1, 0).toLong(), this.pos.add(0, -1, 0));
        BlockPos tempPos;
        List<Pair<Integer, Integer>> py = Arrays.asList(Pair.of(1, 0), Pair.of(-1, 0), Pair.of(0, 1), Pair.of(0, -1));
        while (!queue.isEmpty()){
            if(blockMap.size() > MAX_SIZE)break;
            tempPos = queue.poll();
            for (Pair<Integer, Integer> p : py) {
                if(world.isAirBlock(tempPos.add(p.first, 1, p.second)) || !isUsefulBlock(tempPos.add(p.first, 1, p.second))){
                    if(!world.isAirBlock(tempPos.add(p.first, 0, p.second))){
                        BlockPos pos = tempPos.add(p.first, 0, p.second);
                        Long key = pos.toLong();
                        if(!blockMap.containsKey(key)){
                            queue.add(pos);
                            blockMap.put(key, pos);
                            //world.setBlockState(pos, Blocks.GOLD_BLOCK.getDefaultState());
                        }
                    }
                }
            }
        }
        if(blockMap.size() >= MAX_SIZE || blockMap.size() <= MIN_SIZE){
            blockMap.clear();
        }else{
            this.size = blockMap.size();
            this.blocks.clear();
            this.blocks.putAll(blockMap);
            blockMap.clear();
            return true;
        }
        return false;
    }

    private boolean isUsefulBlock(BlockPos pos){
        //fence wall
        String name = world.getBlockState(pos).getBlock().getTranslationKey();
        if(name.contains("fence") || name.contains("wall"))return true;
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
        if(checkFarm()){
            if (world != null) {
                if(Math.random() < 0.05){
                    System.out.println("Size:" + this.blocks.size());
                    if(this.blocks.size() > 0){
                        List<BlockPos> list = new ArrayList(blocks.values());
                        int pc = (int) (list.size() * Math.random());
                        System.out.println("Change block" + pc + "/" + list.size());
                        doFarm(list.get(pc));
                    }
                }
                //ClientUtils.spawnSteamParticles(world, pos.add(0, 1, 0));
            }
        }
    }

    private void doFarm(BlockPos pos){
        Map<Block, Pair<Block, Double>> blocksMap = new HashMap<>();
        blocksMap.put(Blocks.GOLD_BLOCK, Pair.of(Blocks.DIRT, 1.0));
        blocksMap.put(Blocks.SANDSTONE, Pair.of(Blocks.DIRT, 1.0));

        blocksMap.put(Blocks.DIRT, Pair.of(Blocks.FARMLAND, 1.0));
        blocksMap.put(Blocks.COARSE_DIRT, Pair.of(Blocks.DIRT, 1.0));
        blocksMap.put(ModBlocks.SNOWY_COARSE_DIRT.get(), Pair.of(Blocks.DIRT, 1.0));
        blocksMap.put(ModBlocks.SNOWY_DIRT.get(), Pair.of(Blocks.DIRT, 1.0));

        if(blocksMap.containsKey(world.getBlockState(pos).getBlock())){
            if(Math.random() < blocksMap.get(world.getBlockState(pos).getBlock()).second){
                world.setBlockState(pos, blocksMap.get(world.getBlockState(pos).getBlock()).first.getDefaultState());
            }
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
