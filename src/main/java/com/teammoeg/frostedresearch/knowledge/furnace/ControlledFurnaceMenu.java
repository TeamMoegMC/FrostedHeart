package com.teammoeg.frostedresearch.knowledge.furnace;
import com.teammoeg.chorda.client.cui.menu.*;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
public final class ControlledFurnaceMenu extends CBlockEntityMenu<ControlledFurnaceBlockEntity>{
    private final net.minecraft.world.entity.player.Player operator;
    public static final int[][] POSITIONS={{21,34},{61,34},{101,34}};
    public ControlledFurnaceMenu(int id,Inventory inv,ControlledFurnaceBlockEntity f){
        super(FRContents.MenuTypes.CONTROLLED_FURNACE.get(),f,id,inv.player,3);operator=inv.player;
        for(int i=0;i<3;i++){final int slot=i;addSlot(new CUISlotItemHandler(f.inventory(),i,POSITIONS[i][0],POSITIONS[i][1]){
            @Override public boolean mayPlace(ItemStack s){return slot!=2 && super.mayPlace(s);}
        });}
        for(int i=0;i<36;i++){int p=i<9?i+27:i-9;addSlot(new CUISlot(inv,i,10+(p%6)*17,93+(p/6)*17));}
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){
        return super.stillValid(p) && (p.level().isClientSide || com.teammoeg.chorda.dataholders.team.CTeamDataManager.get(p).getId().equals(getBlock().team())) && p.level()==getBlock().getLevel() && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(getBlock().getBlockPos()))<=64;
    }
    @Override public void broadcastChanges(){
        if(operator instanceof net.minecraft.server.level.ServerPlayer player)getBlock().refreshTargets(player);
        super.broadcastChanges();
    }
    /** Public M5 start/button APIs remain available; the real CUI also compares its displayed target. */
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
