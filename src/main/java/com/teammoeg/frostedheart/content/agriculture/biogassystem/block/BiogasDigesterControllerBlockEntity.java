package com.teammoeg.frostedheart.content.agriculture.biogassystem.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.biogassystem.screen.BiogasDigesterControllerMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BiogasDigesterControllerBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    public final ContainerData propertyDelegate;
    private int yCounter = 1;
    private int maxXCounter = 0;
    private int maxZCounter = 0;
    private int minXCounter = 0;
    private int minZCounter = 0;
    private int checked = 0;
    private int size = 0;
    @Getter
    private int gasValue = 0;
    private int maxGasValue = 0;
    private int shortGasValue = 0;
    private int isSplit = 0;

    public BiogasDigesterControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.BIOGAS_DIGESTER_CONTROLLER.get(), pos, state);
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index){
                    case 0 -> BiogasDigesterControllerBlockEntity.this.checked;
                    case 1 -> BiogasDigesterControllerBlockEntity.this.size;
                    case 2 -> BiogasDigesterControllerBlockEntity.this.shortGasValue;
                    case 3 -> BiogasDigesterControllerBlockEntity.this.isSplit;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public boolean isChecked(){
        return checked != 0;
    }

    public void addGas(int value){
        gasValue += value;
    }
    public void reduceGas(int value){
        if (gasValue > value){
            gasValue -= value;
        } else {
            gasValue = 0;
        }
    }
    public int getCurrentSize(){
        return size;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt("GasValue",gasValue);
        pTag.putInt("MaxGasValue",maxGasValue);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        gasValue = pTag.getInt("GasValue");
        maxGasValue = pTag.getInt("MaxGasValue");
    }
    @Override
    public @NotNull Component getDisplayName() {
        return FHBlocks.BIOGAS_DIGESTER_CONTROLLER.get().getName();
    }
    @Override
    public @NotNull Component getName() {
        return FHBlocks.BIOGAS_DIGESTER_CONTROLLER.get().getName();
    }
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, @NotNull Inventory inventory, @NotNull Player player) {
        return new BiogasDigesterControllerMenu(syncId, inventory,this, this.propertyDelegate);
    }
    private int time = 60;

    public void tick(Level world, BlockPos pos) {
        time--;
        if (time == 10) {
            if (check(world)){
                checked = 1;
            } else {
                checked = 0;
                size = 0;
            }
            restAll();
        }
        if (checked==1){
            maxGasValue = size * 1000;
        } else {
            maxGasValue = 0;
        }
        if (gasValue >= Short.MAX_VALUE){
            shortGasValue = gasValue/19;
            isSplit = 1;
        } else {
            shortGasValue = gasValue;
            isSplit = 0;
        }
        if (gasValue > maxGasValue){
            if (time == 1){
                world.explode(null,pos.getX(),pos.getY(),pos.getZ(),3.5f,true, Level.ExplosionInteraction.BLOCK);
            }
        }
        if (time <= 0){
            time = 60;
        }
    }
    private boolean check(Level world){
        BlockPos maxPos;
        BlockPos minPos;
        int length;
        int width;
        if (world.getBlockState(getBlockPos().below()).isAir()){
            int maxValue = 5;
            do {
                yCounter++;
                if (!world.getBlockState(getBlockPos().below(yCounter)).isAir()){
                    yCounter--;
                    break;
                }
            } while (yCounter <= maxValue);
            if (!world.getBlockState(getBlockPos().below(yCounter+1)).canOcclude()){
                return false;
            } else {
                do {
                    maxXCounter++;
                    if (!world.getBlockState(getBlockPos().east(maxXCounter).below(yCounter)).isAir()){
                        maxXCounter--;
                        break;
                    }
                } while (maxXCounter <= maxValue);
                if (!world.getBlockState(getBlockPos().east(maxXCounter+1).below(yCounter)).canOcclude()){
                    return false;
                } else {
                    do {
                        maxZCounter++;
                        if (!world.getBlockState(getBlockPos().south(maxZCounter).below(yCounter)).isAir()){
                            maxZCounter--;
                            break;
                        }
                    } while (maxZCounter <= maxValue);
                    if (!world.getBlockState(getBlockPos().south(maxZCounter+1).below(yCounter)).canOcclude()){
                        return false;
                    } else {
                        maxPos = new BlockPos(getBlockPos().getX()+maxXCounter,
                                getBlockPos().getY()-yCounter,getBlockPos().getZ()+maxZCounter);
                        do {
                            minXCounter++;
                            if (!world.getBlockState(getBlockPos().west(minXCounter).below()).isAir()){
                                minXCounter--;
                                break;
                            }
                        } while (minXCounter <= maxValue);
                        if (!world.getBlockState(getBlockPos().west(minXCounter+1).below()).canOcclude()){
                            return false;
                        } else {
                            do {
                                minZCounter++;
                                if (!world.getBlockState(getBlockPos().north(minZCounter).below()).isAir()){
                                    minZCounter--;
                                    break;
                                }
                            } while (minZCounter <= maxValue);
                            if (!world.getBlockState(getBlockPos().north(minZCounter+1).below()).canOcclude()){
                                return false;
                            } else {
                                minPos = new BlockPos(getBlockPos().getX()-minXCounter,
                                        getBlockPos().getY()-1,getBlockPos().getZ()-minZCounter);
                                length = maxZCounter + minZCounter+1;
                                width = maxXCounter + minXCounter+1;
                                BlockPos up = minPos.above();
                                for (int s = 0;s < length;s++){
                                    for (int e = 0;e < width;e++){
                                        if (world.getBlockState(new BlockPos(up.getX()+e,up.getY(),up.getZ()+s)).getBlock() == FHBlocks.BIOGAS_DIGESTER_CONTROLLER.get()){
                                            if (!(up.getX()+ e == getBlockPos().getX()&&up.getZ() + s==getBlockPos().getZ())){
                                                return false;
                                            }
                                        }
                                        if (!world.getBlockState(new BlockPos(up.getX()+e,up.getY(),up.getZ()+s)).canOcclude()){
                                            return false;
                                        }
                                    }
                                }
                                BlockPos north = minPos.north();
                                for (int e = 0;e < length;e++){
                                    for (int d = 0;d < yCounter;d++){
                                        if (!world.getBlockState(new BlockPos(north.getX()+e,north.getY()-d,north.getZ())).canOcclude()){
                                            return false;
                                        }
                                    }
                                }
                                BlockPos west = minPos.west();
                                for (int d = 0;d < yCounter;d++){
                                    for (int s = 0;s < length;s++){
                                        if (!world.getBlockState(new BlockPos(west.getX(),west.getY()-d,west.getZ()+s)).canOcclude()){
                                            return false;
                                        }
                                    }
                                }
                                BlockPos down = maxPos.below();
                                for (int n = 0;n < length;n++){
                                    for (int w = 0;w < width;w++){
                                        if (!world.getBlockState(new BlockPos(down.getX()-w,down.getY(),down.getZ()-n)).canOcclude()){
                                            return false;
                                        }
                                    }
                                }
                                BlockPos east = maxPos.east();
                                for (int n = 0;n < length;n++){
                                    for (int u = 0;u < yCounter;u++){
                                        if (!world.getBlockState(new BlockPos(east.getX(),east.getY()+u,east.getZ()-n)).canOcclude()){
                                            return false;
                                        }
                                    }
                                }
                                BlockPos south = maxPos.south();
                                for (int u = 0;u < yCounter;u++){
                                    for (int w = 0;w <width;w++){
                                        if (!world.getBlockState(new BlockPos(south.getX()-w,south.getY()+u,south.getZ())).canOcclude()){
                                            return false;
                                        }
                                    }
                                }
                                for (int u = 0;u < yCounter;u++){
                                    for (int e = 0;e < width;e++){
                                        for (int n = 0;n <length;n++){
                                            if (!world.getBlockState(new BlockPos(maxPos.getX()-e,maxPos.getY()+u,maxPos.getZ()-n)).isAir()){
                                                return false;
                                            }
                                        }
                                    }
                                }
                                size = length * width * yCounter;
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            return false;
        }
    }
    private void restAll(){
        yCounter = 1;
        maxXCounter = 0;
        maxZCounter = 0;
        minXCounter = 0;
        minZCounter = 0;
    }
}
