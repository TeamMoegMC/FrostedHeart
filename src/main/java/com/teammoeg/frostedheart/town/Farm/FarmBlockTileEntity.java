package com.teammoeg.frostedheart.town.Farm;

import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import blusunrize.immersiveengineering.common.blocks.plant.HempBlock;
import com.alcatrazescapee.primalwinter.common.ModBlocks;
import com.ibm.icu.impl.Pair;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.climate.WorldTemperature;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.agriculture.FHBerryBushBlock;
import com.teammoeg.frostedheart.content.agriculture.FHCropBlock;
import com.teammoeg.frostedheart.content.agriculture.RyeBlock;
import com.teammoeg.frostedheart.content.agriculture.WhiteTurnipBlock;
import com.teammoeg.frostedheart.scheduler.IScheduledTaskTE;
import com.teammoeg.frostedheart.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.town.ITownBlockTE;
import com.teammoeg.frostedheart.town.TownWorkerType;
import io.netty.handler.codec.sctp.SctpOutboundByteStreamHandler;
import net.minecraft.block.*;
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
    public static double WORK_SPEED = 10.0;
    public Map<Block, Pair<Block, Double>> blockTransMap;
    public List<Pair<Block, Float>> blocksForPlant;
    public int size;//Size of the farm
    public int temperature;
    public Map<Long, BlockPos> blocks;
    public FarmBlockTileEntity() {
        super(FHTileTypes.FARM.get());
        this.isAdd = false;
        this.blocks = new HashMap<>();
        this.blockTransMap = new HashMap<>();
        this.blocksForPlant = new Stack<>();

        blockTransMap.put(Blocks.DIRT, Pair.of(Blocks.FARMLAND, 1.0));
        blockTransMap.put(Blocks.COARSE_DIRT, Pair.of(Blocks.DIRT, 1.0));
        blockTransMap.put(ModBlocks.SNOWY_COARSE_DIRT.get(), Pair.of(Blocks.DIRT, 1.0));
        blockTransMap.put(ModBlocks.SNOWY_DIRT.get(), Pair.of(Blocks.DIRT, 1.0));

        blocksForPlant.add(Pair.of(FHBlocks.white_turnip_block.get(), (float)((FHCropBlock)FHBlocks.white_turnip_block.get()).getGrowTemperature()));
        blocksForPlant.add(Pair.of(IEBlocks.Misc.hempPlant, WorldTemperature.HEMP_GROW_TEMPERATURE));
        blocksForPlant.add(Pair.of(FHBlocks.rye_block.get(), (float)((FHCropBlock)FHBlocks.rye_block.get()).getGrowTemperature()));

        blockTransMap.put(Blocks.GOLD_BLOCK, Pair.of(Blocks.DIRT, 1.0));
        blockTransMap.put(Blocks.SANDSTONE, Pair.of(Blocks.DIRT, 1.0));
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
        assert world != null : "Empty world";
        Block block = world.getBlockState(pos).getBlock();
        if(block instanceof FenceBlock || block instanceof WallBlock ||block instanceof FenceGateBlock)return true;
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
                if(Math.random() < 0.01 * WORK_SPEED){
                    if(this.blocks.size() > 0){
                        List<BlockPos> list = new ArrayList(blocks.values());
                        int pc = (int) (list.size() * Math.random());
                        doFarm(list.get(pc));
                    }
                }
            }
        }
    }

    private void doFarm(BlockPos pos){
        assert world != null;
        if(blockTransMap.containsKey(world.getBlockState(pos).getBlock())){
            if(Math.random() < blockTransMap.get(world.getBlockState(pos).getBlock()).second){
                world.setBlockState(pos, blockTransMap.get(world.getBlockState(pos).getBlock()).first.getDefaultState());
            }
        }
        if(world.getBlockState(pos).getBlock() instanceof FarmlandBlock && world.isAirBlock(pos.add(0, 1, 0))){
            Block plt = selectSeedForPlant();
            if(plt != null){
                System.out.println("Plant: " + plt.getTranslatedName());
                world.setBlockState(pos.add(0, 1, 0), plt.getDefaultState());
            }
        }
    }

    private Block selectSeedForPlant(){
        float temp = ChunkHeatData.getTemperature(world, pos) + 100;
        List<Block> blist = new Stack<>();
        for(Pair<Block, Float> bp : blocksForPlant){
            if(bp.second < temp){
                blist.add(bp.first);
            }
        }
        if(blist.size() <= 0)return null;
        return blist.get((int) (Math.random() * blist.size()));
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
