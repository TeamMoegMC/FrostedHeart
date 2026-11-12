package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** One post-pickup bridge and one menu transaction bridge; no duplicate craft/smelt observers. */
@Mod.EventBusSubscriber(modid = FRMain.MODID)
public final class KnowledgeBehaviorBridge {
    private static final ThreadLocal<List<IdeaAcquisitionService.Signal>> USE = new ThreadLocal<>();
    public static void beginUse() { USE.set(new ArrayList<>()); }
    public static void itemUsed(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !interested(IdeaAcquisitionService.ITEM_USED)) return;
        var signal = new IdeaAcquisitionService.Signal(IdeaAcquisitionService.ITEM_USED, ItemRetrieval.behaviorSample(player, stack));
        if (USE.get() != null) USE.get().add(signal);
        else IdeaAcquisitionService.fire(KnowledgeService.forPlayer(player), List.of(signal), player.getRandom()::nextDouble);
    }
    public static void finishUse(ServerPlayer player, Optional<KnowledgeElement> block) {
        var signals = USE.get(); USE.remove();
        if (signals == null) signals = new ArrayList<>();
        if (interested(IdeaAcquisitionService.BLOCK_INTERACTED) && block.isPresent()) signalsAdd(signals, block.get());
        if (!signals.isEmpty()) IdeaAcquisitionService.fire(KnowledgeService.forPlayer(player), signals, player.getRandom()::nextDouble);
    }
    private static void signalsAdd(List<IdeaAcquisitionService.Signal> signals, KnowledgeElement block) {
        signals.add(new IdeaAcquisitionService.Signal(IdeaAcquisitionService.BLOCK_INTERACTED, block));
    }
    public static boolean interested(net.minecraft.resources.ResourceLocation event) {
        return !com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions.current().behaviorIdeas(event).isEmpty();
    }
    @SubscribeEvent
    public static void pickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) obtained(player, List.of(event.getStack()));
    }
    /** Aggregate player-owned inventory AND cursor. Moving stacks within that ownership is not acquisition. */
    public static List<ItemStack> owned(Player player, ItemStack carried) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) add(list, player.getInventory().getItem(i));
        // Crafting inputs already belong to the player. Returning them to the cursor is not acquisition.
        Set<net.minecraft.world.Container> crafting = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var menu : List.of(player.inventoryMenu, player.containerMenu))
            for (var slot : menu.slots) if (slot.container instanceof net.minecraft.world.inventory.CraftingContainer && crafting.add(slot.container))
                for (int i = 0; i < slot.container.getContainerSize(); i++) add(list, slot.container.getItem(i));
        add(list, carried); return list;
    }
    private static void add(List<ItemStack> list, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (ItemStack old : list) if (ItemStack.isSameItemSameTags(old, stack)) { old.grow(stack.getCount()); return; }
        list.add(stack.copy());
    }
    public static List<ItemStack> gained(List<ItemStack> before, List<ItemStack> after) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : after) {
            int old = before.stream().filter(s -> ItemStack.isSameItemSameTags(s, stack)).mapToInt(ItemStack::getCount).sum();
            if (stack.getCount() > old) { ItemStack gain = stack.copy(); gain.setCount(stack.getCount() - old); result.add(gain); }
        }
        return result;
    }
    public static void obtained(ServerPlayer player, List<ItemStack> gains) {
        if (!interested(IdeaAcquisitionService.ITEM_OBTAINED)) return;
        List<IdeaAcquisitionService.Signal> signals = gains.stream().filter(s -> !s.isEmpty())
            .map(s -> new IdeaAcquisitionService.Signal(IdeaAcquisitionService.ITEM_OBTAINED, ItemRetrieval.behaviorSample(player, s))).toList();
        if (!signals.isEmpty()) IdeaAcquisitionService.fire(KnowledgeService.forPlayer(player), signals, player.getRandom()::nextDouble);
    }
}
