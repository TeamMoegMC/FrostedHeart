package com.teammoeg.frostedresearch.knowledge.furnace;

import com.teammoeg.chorda.block.entity.*;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import java.util.*;

/** Loaded ticks only; one durable batch owns either refundable input or a finished output, never both. */
public final class ControlledFurnaceBlockEntity extends CBlockEntity implements CTickableBlockEntity,MenuProvider {
    private final ItemStackHandler inventory=new ItemStackHandler(3){
        @Override protected void onContentsChanged(int slot){setChanged();}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot!=2;}
    };
    private UUID team;
    private boolean trial=true;
    private int temperature=1000,lockedTemperature=1000,remaining,duration;
    private ItemStack refund=ItemStack.EMPTY,planned=ItemStack.EMPTY,pending=ItemStack.EMPTY;
    private ResearchSampleBinding binding,selectedTarget;
    private Component targetTitle=Component.empty();
    private int targetCount;
    private List<ResearchSampleBinding> targetOptions=List.of();
    private long targetsRevision=-1,targetsDefinitions=-1;
    private Object targetsRecipes;
    private String message="idle",outcome="complete";
    public ControlledFurnaceBlockEntity(BlockPos pos,BlockState state){super(FRContents.BlockEntityTypes.CONTROLLED_FURNACE.get(),pos,state);}
    public ItemStackHandler inventory(){return inventory;}
    public UUID team(){return team;}
    public boolean trial(){return trial;}
    public int temperature(){return temperature;}
    public int lockedTemperature(){return lockedTemperature;}
    public int remaining(){return remaining;}
    public int duration(){return duration;}
    public String message(){return message;}
    public boolean busy(){return !refund.isEmpty() || !pending.isEmpty();}
    public Optional<ResearchSampleBinding> binding(){return Optional.ofNullable(binding);}
    public Optional<ResearchSampleBinding> selectedTarget(){return Optional.ofNullable(selectedTarget);}
    public Component targetTitle(){return targetTitle;}
    public int targetCount(){return targetCount;}
    /** Recompute on knowledge/definition/recipe changes, not on each menu broadcast. */
    public void refreshTargets(ServerPlayer player){
        if(!claim(player) || level==null || level.isClientSide)return;
        var service=KnowledgeService.forPlayer(player);
        long revision=service.state().mutationRevision(),definitions=KnowledgeDefinitions.current().revision();
        Object recipes=level.getRecipeManager();
        if(targetsRevision==revision && targetsDefinitions==definitions && targetsRecipes==recipes)return;
        targetsRevision=revision;targetsDefinitions=definitions;targetsRecipes=recipes;
        java.util.Set<net.minecraft.resources.ResourceLocation> experiments=new HashSet<>();
        level.getRecipeManager().getAllRecipesFor(FRContents.Recipes.CONTROLLED_FIRING.get()).forEach(r -> r.experiment().ifPresent(experiments::add));
        targetOptions=service.observationTargets(experiments);targetCount=targetOptions.size();
        if((selectedTarget==null || !targetOptions.contains(selectedTarget)) && targetCount==1)selectedTarget=targetOptions.get(0);
        targetTitle=selectedTarget!=null && (targetOptions.contains(selectedTarget) || busy()) ? service.targetLabel(selectedTarget) : Component.empty();
        changed();
    }
    public Optional<ResearchSampleBinding> cycleTarget(ServerPlayer player){
        refreshTargets(player);
        if(!claim(player) || busy() || targetOptions.isEmpty())return Optional.empty();
        int current=selectedTarget==null?-1:targetOptions.indexOf(selectedTarget);selectedTarget=targetOptions.get((current+1)%targetOptions.size());
        targetTitle=KnowledgeService.forPlayer(player).targetLabel(selectedTarget);changed();return selectedTarget();
    }
    public boolean claim(ServerPlayer player){
        UUID current=CTeamDataManager.get(player).getId();
        if(team==null){team=current;changed();}return team.equals(current);
    }
    public void adjust(int steps){temperature=Math.max(600,Math.min(1400,temperature+Integer.signum(steps)*50));changed();}
    public void mode(){if(!busy()){trial=!trial;changed();}}
    public static boolean managed(ControlledFiringRecipe recipe){
        return KnowledgeDefinitions.current().results().values().stream().anyMatch(d -> d.enabled()
            && d.result() instanceof ResearchResult.Design design && design.recipes().contains(recipe.id()));
    }
    public List<ControlledFiringRecipe> recipes(){
        return level==null ? List.of() : level.getRecipeManager().getAllRecipesFor(FRContents.Recipes.CONTROLLED_FIRING.get()).stream()
            .filter(r -> r.experiment().isPresent()==trial).sorted(Comparator.comparing(r -> r.id().toString())).toList();
    }
    public String start(ServerPlayer player){
        if(!claim(player))return say("not_owned");
        if(busy())return say("busy");
        if(!inventory.getStackInSlot(2).isEmpty())return say("output_full");
        refreshTargets(player);
        var service=KnowledgeService.forPlayer(player);
        if(trial && targetCount>1 && (selectedTarget==null || !targetOptions.contains(selectedTarget)))return say("choose_target");
        boolean eligible=false;
        for(var recipe:recipes()){
            ResearchSampleBinding target=null;
            if(trial){
                if(selectedTarget==null || !targetOptions.contains(selectedTarget) || !recipe.experiment().orElseThrow().equals(selectedTarget.experiment()))continue;
                target=selectedTarget;
            }
            else if(!managed(recipe) || !ResearchHooks.canUseRecipe(team,recipe))continue;
            eligible=true;
            if(!recipe.matches(inventory.getStackInSlot(0),inventory.getStackInSlot(1)))continue;
            refund=inventory.extractItem(0,recipe.input().getCount(),false);
            inventory.extractItem(1,recipe.fuel().getCount(),false);
            lockedTemperature=temperature;outcome=temperature<recipe.temperatureMin()?"underfired":temperature>recipe.temperatureMax()?"overfired":"complete";remaining=duration=recipe.duration();planned=recipe.outcome(lockedTemperature);binding=target;
            if(binding!=null && outcome.equals("complete"))binding.write(planned);
            return say("running");
        }
        return say(eligible ? "materials" : trial ? "no_trial" : "need_design");
    }
    private String say(String value){message=value;changed();return value;}
    private void changed(){setChanged();if(level!=null && !level.isClientSide)syncData();}
    public void cancel(){
        if(refund.isEmpty()){if(!pending.isEmpty())say("output_full");return;}
        ItemStack rest=inventory.insertItem(0,refund,false);
        if(!rest.isEmpty() && level!=null)net.minecraft.world.level.block.Block.popResource(level,worldPosition,rest);
        refund=ItemStack.EMPTY;planned=ItemStack.EMPTY;remaining=0;binding=null;say("cancelled");
    }
    @Override public void tick(){
        if(level==null || level.isClientSide || !busy())return;
        if(!refund.isEmpty()){
            if(remaining>0)remaining--;
            if(remaining==0){pending=planned;planned=ItemStack.EMPTY;refund=ItemStack.EMPTY;message="output_full";}
            setChanged();if(remaining%20==0)syncData();
        }
        if(!pending.isEmpty()){
            ItemStack output=inventory.getStackInSlot(2);
            if(output.isEmpty() || ItemStack.isSameItemSameTags(output,pending) && output.getCount()+pending.getCount()<=output.getMaxStackSize()){
                ItemStack merged=pending.copy();merged.grow(output.getCount());inventory.setStackInSlot(2,merged);
                pending=ItemStack.EMPTY;binding=null;say(outcome);
            }
        }
    }
    /** Called once by real block removal. Clearing before drops makes repeated settlement harmless. */
    public List<ItemStack> settle(){
        List<ItemStack> drops=new ArrayList<>();
        for(int i=0;i<3;i++){var s=inventory.getStackInSlot(i);if(!s.isEmpty())drops.add(s.copy());inventory.setStackInSlot(i,ItemStack.EMPTY);}
        if(!pending.isEmpty())drops.add(pending.copy());else if(!refund.isEmpty())drops.add(refund.copy());
        pending=refund=planned=ItemStack.EMPTY;binding=null;remaining=0;setChanged();return drops;
    }
    @Override public void writeCustomNBT(CompoundTag n,boolean desc){
        if(!desc)n.put("inventory",inventory.serializeNBT());
        if(team!=null)n.putUUID("team",team);n.putBoolean("trial",trial);n.putInt("temperature",temperature);n.putInt("locked_temperature",lockedTemperature);
        n.putString("outcome",outcome);n.putInt("remaining",remaining);n.putInt("duration",duration);n.putString("message",message);
        if(!desc){n.put("refund",refund.save(new CompoundTag()));n.put("planned",planned.save(new CompoundTag()));n.put("pending",pending.save(new CompoundTag()));
            if(binding!=null)n.put("binding",ResearchSampleBinding.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,binding).result().orElseThrow());}
        if(selectedTarget!=null)n.put("selected_target",ResearchSampleBinding.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,selectedTarget).result().orElseThrow());
        n.putString("target_title",Component.Serializer.toJson(targetTitle));n.putInt("target_count",targetCount);
        n.putBoolean("busy",busy());
    }
    private boolean clientBusy;
    @Override public void readCustomNBT(CompoundTag n,boolean desc){
        if(!desc){inventory.deserializeNBT(n.getCompound("inventory"));refund=ItemStack.of(n.getCompound("refund"));planned=ItemStack.of(n.getCompound("planned"));pending=ItemStack.of(n.getCompound("pending"));
            binding=ResearchSampleBinding.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,n.getCompound("binding")).result().orElse(null);}
        selectedTarget=n.contains("selected_target") ? ResearchSampleBinding.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,n.get("selected_target")).result().orElse(null) : binding;
        targetTitle=n.getString("target_title").isBlank()?Component.empty():Component.Serializer.fromJson(n.getString("target_title"));
        if(targetTitle==null)targetTitle=Component.empty();targetCount=n.getInt("target_count");targetsRevision=-1;
        team=n.hasUUID("team")?n.getUUID("team"):null;trial=!n.contains("trial") || n.getBoolean("trial");
        temperature=n.contains("temperature")?Math.max(600,Math.min(1400,n.getInt("temperature"))):1000;
        outcome=n.contains("outcome")?n.getString("outcome"):"complete";lockedTemperature=n.getInt("locked_temperature");remaining=n.getInt("remaining");duration=n.getInt("duration");message=n.getString("message");clientBusy=n.getBoolean("busy");
    }
    public boolean displayedBusy(){return level!=null && level.isClientSide ? clientBusy : busy();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new ControlledFurnaceMenu(id,inv,this);}
    @Override public Component getDisplayName(){return Component.translatable("block.frostedresearch.controlled_furnace");}
}
