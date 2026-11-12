/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Action;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Method;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationSessions;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationStatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real server sessions and serialized replies; no client input is simulated by these checks. */
@GameTestHolder("frostedresearch_observation")
@PrefixGameTestTemplate(false)
public final class ObservationGameTests {
    @GameTest(template = "empty", timeoutTicks = 90)
    public static void itemsUseSharedSamplingAndOnlyMagnifiedItemsTakeHalfTime(GameTestHelper helper) {
        Observer key = new Observer(helper), glass = new Observer(helper), slot = new Observer(helper), drop = new Observer(helper), block = new Observer(helper);
        List<Observer> observers = List.of(key, glass, slot, drop, block);
        key.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 4));
        key.start(Action.HELD_ITEM, 0, Method.KEY);
        glass.player.setItemInHand(InteractionHand.MAIN_HAND, FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
        glass.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.PAPER, 4));
        glass.start(Action.HELD_ITEM, 1, Method.MAIN_HAND_GLASS);
        slot.player.getInventory().setItem(9, new ItemStack(Items.PAPER, 4));
        slot.player.containerMenu.setCarried(FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
        slot.start(Action.SLOT_ITEM, 9, Method.CURSOR_GLASS);
        ItemEntity dropped = new ItemEntity(helper.getLevel(), drop.player.getX(), drop.player.getY() + 1, drop.player.getZ(), new ItemStack(Items.PAPER, 4));
        dropped.setNoGravity(true);
        dropped.setNeverPickUp();
        helper.getLevel().addFreshEntity(dropped);
        drop.player.setItemInHand(InteractionHand.OFF_HAND, FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
        drop.start(Action.ENTITY, dropped.getId(), Method.OFF_HAND_GLASS);
        BlockPos stone = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        block.player.setItemInHand(InteractionHand.MAIN_HAND, FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
        ObservationSessions.handle(block.player, new ObservationActionPacket(Action.BLOCK, stone, -1, -1, Method.MAIN_HAND_GLASS));
        helper.assertTrue(key.last().durationTicks() == 60 && block.last().durationTicks() == 60, "key and magnified blocks retain three seconds");
        helper.assertTrue(glass.last().durationTicks() == 30 && slot.last().durationTicks() == 30 && drop.last().durationTicks() == 30, "all magnified item sources take 30 ticks");
        helper.onEachTick(() -> observers.forEach(Observer::tick));
        helper.runAfterDelay(29, () -> observers.forEach(observer -> helper.assertTrue(!observer.observed(), "no early completion")));
        helper.runAfterDelay(31, () -> {
            for (Observer observer : List.of(glass, slot, drop)) {
                helper.assertTrue(observer.observed(), "magnified item completes first: " + observer.replies);
                Observation observation = Observation.CODEC.parse(NbtOps.INSTANCE, observer.last().record()).result().orElseThrow();
                helper.assertTrue(observation.type() == Observation.Type.ITEM && observation.object().toString().equals("minecraft:paper"), "dropped/held/slot targets share item semantics");
                helper.assertTrue(observation.value("dimension").state() == com.teammoeg.frostedresearch.knowledge.model.ObservationValue.State.NOT_APPLICABLE, "item records do not acquire world fields");
            }
            helper.assertTrue(!key.observed() && !block.observed(), "ordinary duration is still running");
            helper.assertTrue(slot.player.containerMenu.getCarried().getCount() == 1 && slot.player.getInventory().getItem(9).getCount() == 4, "inventory inspection neither consumes nor moves items");
            helper.assertTrue(dropped.getItem().getCount() == 4, "dropped inspection does not consume the stack");
        });
        helper.runAfterDelay(61, () -> {
            helper.assertTrue(key.observed() && block.observed(), "ordinary targets complete after sixty ticks");
            observers.forEach(Observer::discard);
            dropped.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void targetAndToolChangesCancelAndMenuIdsDoNotSelectOtherSlots(GameTestHelper helper) {
        Observer hand = new Observer(helper);
        hand.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER));
        hand.player.getInventory().setItem(1, new ItemStack(Items.PAPER));
        hand.start(Action.HELD_ITEM, 0, Method.KEY);
        hand.player.getInventory().selected = 1;
        hand.tick();
        helper.assertTrue(hand.last().state() == ObservationStatePacket.State.CANCELLED, "switching hotbar slot cancels even with an identical stack");

        Observer slot = new Observer(helper);
        slot.player.getInventory().setItem(9, new ItemStack(Items.PAPER, 4));
        slot.player.containerMenu.setCarried(FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
        slot.start(Action.SLOT_ITEM, 9, Method.CURSOR_GLASS);
        slot.player.getInventory().getItem(9).shrink(1);
        slot.tick();
        helper.assertTrue(slot.last().state() == ObservationStatePacket.State.CANCELLED, "changing the physical stack cancels");
        slot.start(Action.SLOT_ITEM, 9, Method.CURSOR_GLASS);
        slot.player.containerMenu.setCarried(ItemStack.EMPTY);
        slot.tick();
        helper.assertTrue(slot.last().state() == ObservationStatePacket.State.CANCELLED, "removing the carried tool cancels");

        Observer chest = new Observer(helper);
        var contents = new SimpleContainer(27);
        contents.setItem(0, new ItemStack(Items.STICK));
        chest.player.containerMenu = ChestMenu.threeRows(7, chest.player.getInventory(), contents);
        chest.start(Action.SLOT_ITEM, 0, Method.KEY);
        helper.assertTrue(chest.last().state() == ObservationStatePacket.State.STARTED, "external container slots are observable");
        chest.player.containerMenu = chest.player.inventoryMenu;
        chest.tick();
        helper.assertTrue(chest.last().state() == ObservationStatePacket.State.CANCELLED, "closing a container cancels");
        ObservationSessions.handle(chest.player, new ObservationActionPacket(Action.SLOT_ITEM, BlockPos.ZERO, 0, 7, Method.KEY));
        helper.assertTrue(chest.last().state() == ObservationStatePacket.State.CANCELLED, "stale container ids do not observe a replacement slot");
        chest.player.setItemInHand(InteractionHand.MAIN_HAND, FRContents.Items.RESEARCH_NOTE.get().getDefaultInstance());
        chest.start(Action.HELD_ITEM, 0, Method.KEY);
        helper.assertTrue(chest.last().state() == ObservationStatePacket.State.CANCELLED, "notes retain the existing carrier restriction");
        List.of(hand, slot, chest).forEach(Observer::discard);
        helper.succeed();
    }

    private static final class Observer {
        final ServerPlayer player;
        final List<ObservationStatePacket> replies = new ArrayList<>();

        Observer(GameTestHelper helper) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "observation-test"));
            player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 1, 2)));
            Connection connection = new Connection(PacketFlow.SERVERBOUND) {
                @Override
                public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket payload) {
                        var data = payload.getData();
                        try {
                            data.readVarInt(); // Forge SimpleChannel discriminator; only observation replies are sent here.
                            replies.add(new ObservationStatePacket(data));
                        } finally {
                            data.release();
                        }
                    }
                }
            };
            player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player);
            player.setGameMode(GameType.SURVIVAL);
        }

        void start(Action action, int target, Method method) {
            ObservationSessions.handle(player, new ObservationActionPacket(action, BlockPos.ZERO, target, player.containerMenu.containerId, method));
        }

        ObservationStatePacket last() { return replies.get(replies.size() - 1); }
        boolean observed() { return replies.stream().anyMatch(packet -> packet.state() == ObservationStatePacket.State.OBSERVED); }
        void tick() {
            // These isolated observers never log in to FTB. Supply their real Chorda test team while dispatching
            // the completed action; normal logged-in-player behavior is covered by IdeaAcquisitionGameTests.
            var previous = com.teammoeg.chorda.dataholders.team.TeamsAPI.getAPI();
            com.teammoeg.chorda.dataholders.team.TeamsAPI.register(new com.teammoeg.chorda.dataholders.team.SinglePlayerTeamAPIProvider());
            try { ObservationSessions.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player)); }
            finally { com.teammoeg.chorda.dataholders.team.TeamsAPI.register(previous); }
        }
        void discard() { ObservationSessions.handle(player, new ObservationActionPacket(Action.DISCARD)); }
    }
}
