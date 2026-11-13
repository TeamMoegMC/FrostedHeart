package com.teammoeg.frostedresearch.knowledge.furnace;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;
public final class ControlledFurnaceMenu extends CBlockEntityMenu<ControlledFurnaceBlockEntity>{
    private final net.minecraft.world.entity.player.Player operator;
    public static final int[][] POSITIONS={{44,90},{88,90},{184,90}};
    public static final int INVENTORY_X=57,INVENTORY_Y=148,HOTBAR_Y=206;
    public ControlledFurnaceMenu(int id,Inventory inv,ControlledFurnaceBlockEntity f){
        super(FRContents.MenuTypes.CONTROLLED_FURNACE.get(),f,id,inv.player,3);operator=inv.player;
        for(int i=0;i<3;i++){final int slot=i;addSlot(new SlotItemHandler(f.inventory(),i,POSITIONS[i][0],POSITIONS[i][1]){
            @Override public boolean mayPlace(ItemStack s){return slot!=2 && super.mayPlace(s);}
        });}
        for(int i=0;i<36;i++)addSlot(new Slot(inv,i,INVENTORY_X+(i%9)*18,i<9?HOTBAR_Y:INVENTORY_Y+(i/9-1)*18));
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){
        return super.stillValid(p) && (p.level().isClientSide || com.teammoeg.chorda.dataholders.team.CTeamDataManager.get(p).getId().equals(getBlock().team())) && p.level()==getBlock().getLevel() && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(getBlock().getBlockPos()))<=64;
    }
    @Override public void broadcastChanges(){
        if(operator instanceof net.minecraft.server.level.ServerPlayer player)getBlock().refreshTargets(player);
        super.broadcastChanges();
    }
    /** The ordinary container screen submits the mode and target displayed with its start button. */
    public boolean startSeen(net.minecraft.server.level.ServerPlayer player,boolean trial,java.util.Optional<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> target){
        if(!stillValid(player) || !getBlock().claim(player))return false;
        getBlock().refreshTargets(player);
        if(getBlock().trial()!=trial || trial && !getBlock().selectedTarget().equals(target)) {
            getBlock().syncData();return false;
        }
        getBlock().start(player);return true;
    }
    @Override public boolean clickMenuButton(net.minecraft.world.entity.player.Player p,int id){
        if(!(p instanceof net.minecraft.server.level.ServerPlayer player) || !stillValid(p) || !getBlock().claim(player))return false;
        switch(id){case 0->getBlock().adjust(-1);case 1->getBlock().adjust(1);case 2->getBlock().mode();case 3->getBlock().start(player);case 4->getBlock().cancel();case 5->getBlock().cycleTarget(player);default->{return false;}}
        return true;
    }
}
