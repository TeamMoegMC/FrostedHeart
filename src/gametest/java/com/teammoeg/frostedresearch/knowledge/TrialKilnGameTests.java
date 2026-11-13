package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.*;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("frostedresearch_m3")
@PrefixGameTestTemplate(false)
public final class TrialKilnGameTests {
    private static ResourceLocation id(String name) { return new ResourceLocation("example", name); }
    private static <T> T fixture(String kind, String name, Codec<T> codec) {
        try (var reader = new java.io.InputStreamReader(Objects.requireNonNull(TrialKilnGameTests.class.getResourceAsStream("/m3-fixtures/" + kind + "/" + name + ".json")), java.nio.charset.StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(false, error -> {});
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static void install() {
        var snapshot = new KnowledgeDefinitions.Snapshot(0,
            Map.of(id("trial_kiln"), fixture("ideas", "trial_kiln", IdeaDefinition.CODEC)),
            Map.of(id("trial_kiln_finding"), fixture("results", "trial_kiln_finding", ResultDefinition.CODEC)),
            Map.of(), Map.of(), Map.of(id("trial_kiln"), fixture("projects", "trial_kiln", ProjectDefinition.CODEC)), List.of());
        if (!KnowledgeDefinitions.validate(snapshot).isEmpty()) throw new IllegalStateException(KnowledgeDefinitions.validate(snapshot).toString());
        KnowledgeDefinitions.install(snapshot);
    }
    private static KnowledgeService service() { return new KnowledgeService(UUID.randomUUID(), new KnowledgeState(), 42, () -> {}); }
    private static UUID begin(KnowledgeService service, ServerPlayer player) {
        var source = new AcquisitionSource("test_external", Optional.empty(), 42);
        service.learn(KnowledgeElement.idea(id("trial_kiln")), source, KnowledgeService.Grant.NORMAL);
        for (Item item : List.of(Items.CLAY_BALL, Items.SAND)) service.learn(ItemRetrieval.observe(player, new ItemStack(item)), source, KnowledgeService.Grant.NORMAL);
        return service.startResearch(id("trial_kiln")).orElseThrow();
    }
    private static TrialKilnBlockEntity kiln(GameTestHelper h, BlockPos local, Block block) {
        h.setBlock(local, block); return (TrialKilnBlockEntity) h.getBlockEntity(local);
    }
    private static void ticks(TrialKilnBlockEntity kiln, int count) {
        for (int i = 0; i < count; i++) TrialKilnBlockEntity.serverTick(kiln.getLevel(), kiln.getBlockPos(), kiln.getBlockState(), kiln);
    }
    private static int drops(GameTestHelper h, BlockPos local, Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(local)).inflate(1)).stream()
            .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
    private static final class PlayerFixture implements AutoCloseable {
        final ServerPlayer player;
        final io.netty.channel.embedded.EmbeddedChannel channel;
        PlayerFixture(GameTestHelper h) {
            var server = h.getLevel().getServer(); player = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), "m3-kiln"));
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel = new io.netty.channel.embedded.EmbeddedChannel(connection); connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
            server.getPlayerList().placeNewPlayer(connection, player); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var pos = h.absolutePos(new BlockPos(1, 2, 2)); player.teleportTo(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), Set.of(), 0, 0);
        }
        @Override public void close() { player.server.getPlayerList().remove(player); channel.finishAndReleaseAll(); }
    }

    @GameTest(batch = "m3_trial_kiln", template = "trial_kiln_empty", timeoutTicks = 480)
    public static void threeMixturesUseRealTicksAndResumeSavedProgress(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current(); install(); var actor = new PlayerFixture(h);
        try {
            var service = KnowledgeService.forPlayer(actor.player); UUID run = begin(service, actor.player);
            List<BlockPos> positions = List.of(new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1));
            List<TrialKilnBlockEntity> kilns = new ArrayList<>();
            List<Block> blocks = List.of(FRContents.Blocks.TRIAL_KILN_CLAY_RICH.get(), FRContents.Blocks.TRIAL_KILN_SAND_RICH.get(), FRContents.Blocks.TRIAL_KILN_BALANCED.get());
            for (int i = 0; i < 3; i++) {
                var kiln = kiln(h, positions.get(i), blocks.get(i)); kilns.add(kiln);
                actor.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 3));
                kiln.getBlockState().use(h.getLevel(), actor.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(kiln.getBlockPos()), Direction.UP, kiln.getBlockPos(), false));
                h.assertTrue(actor.player.getMainHandItem().getCount() == 3 && kiln.remaining() == 0, "insufficient fuel does not consume or start");
                actor.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 4));
                kiln.getBlockState().use(h.getLevel(), actor.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(kiln.getBlockPos()), Direction.UP, kiln.getBlockPos(), false));
                h.assertTrue(actor.player.getMainHandItem().isEmpty() && kiln.remaining() == 400 && kiln.binding().orElseThrow().run().equals(run), "four-stick real interaction binds current run");
            }
            h.runAfterDelay(180, () -> {
                try {
                    var old = (TrialKilnBlockEntity) h.getBlockEntity(positions.get(2)); int remaining = old.remaining();
                    CompoundTag saved = old.saveWithFullMetadata(); BlockState state = old.getBlockState();
                    h.getLevel().removeBlockEntity(old.getBlockPos());
                    var restored = (TrialKilnBlockEntity) BlockEntity.loadStatic(h.absolutePos(positions.get(2)), state, saved);
                    h.getLevel().setBlockEntity(restored);
                    h.assertTrue(remaining > 0 && remaining < 400 && restored.remaining() == remaining && restored.binding().equals(old.binding()), "actual BE NBT reconstruction retains remaining ticks and binding without catch-up");
                } catch (Throwable failure) { actor.close(); KnowledgeDefinitions.install(previous); throw failure; }
            });
            h.runAfterDelay(398, () -> h.assertTrue(h.getBlockState(positions.get(0)).getValue(TrialKilnBlock.PHASE) == 1, "not done before 400 loaded ticks"));
            h.runAfterDelay(405, () -> {
                try {
                    for (int i = 0; i < 3; i++) h.assertTrue(h.getBlockState(positions.get(i)).getValue(TrialKilnBlock.PHASE) == (i == 2 ? 2 : 3), "two bad mixtures and one intact mixture");
                    var good = (TrialKilnBlockEntity) h.getBlockEntity(positions.get(2));
                    actor.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    good.getBlockState().use(h.getLevel(), actor.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(good.getBlockPos()), Direction.UP, good.getBlockPos(), false));
                    h.assertTrue(h.getBlockState(positions.get(2)).isAir() && good.settle().isEmpty(), "collect removes kiln and cannot settle again");
                    h.getLevel().destroyBlock(h.absolutePos(positions.get(0)), true, actor.player); h.getLevel().destroyBlock(h.absolutePos(positions.get(1)), true, actor.player);
                    h.assertTrue(drops(h, positions.get(0), FRContents.Items.CRACKED_TRIAL_KILN.get()) == 1 && drops(h, positions.get(1), FRContents.Items.CRACKED_TRIAL_KILN.get()) == 1, "finished destruction drops exactly one bad sample per kiln");
                    ItemStack sample = actor.player.getInventory().items.stream().filter(s -> s.is(FRContents.Items.FIRED_TRIAL_KILN.get())).findFirst().orElseThrow();
                    h.assertTrue(service.submitSample(run, "submit_intact", sample).succeeded() && sample.isEmpty(), "actual fired sample is consumed by its run");
                    h.assertTrue(service.finishResearch(run).stream().allMatch(KnowledgeService.OperationResult::succeeded), "normal task completion grants the finding");
                    h.succeed();
                } finally { actor.close(); KnowledgeDefinitions.install(previous); }
            });
        } catch (Throwable failure) { actor.close(); KnowledgeDefinitions.install(previous); throw failure; }
    }

    @GameTest(batch = "m3_trial_kiln", template = "trial_kiln_empty", timeoutTicks = 40)
    public static void breakResetSettlementAndFullInventoryDelivery(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current(); install();
        try (var actor = new PlayerFixture(h)) {
            var service = service();
            var grid = (net.minecraft.world.inventory.CraftingContainer) actor.player.inventoryMenu.getSlot(1).container;
            for (int clay : List.of(3, 1, 2)) {
                for (int i = 0; i < 4; i++) grid.setItem(i, new ItemStack(i < clay ? Items.CLAY_BALL : Items.SAND));
                var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel()).orElseThrow();
                ItemStack output = recipe.assemble(grid, h.getLevel().registryAccess());
                Item expected = (clay == 3 ? FRContents.Blocks.TRIAL_KILN_CLAY_RICH : clay == 1 ? FRContents.Blocks.TRIAL_KILN_SAND_RICH : FRContents.Blocks.TRIAL_KILN_BALANCED).get().asItem();
                h.assertTrue(grid.getContainerSize() == 4 && output.is(expected) && output.getCount() == 1, "native shapeless ratio fits personal 2x2 crafting");
            }
            var pos = new BlockPos(1, 1, 1); var kiln = kiln(h, pos, FRContents.Blocks.TRIAL_KILN_BALANCED.get());
            ItemStack fuel = new ItemStack(Items.STICK, 4); kiln.start(service, fuel); ticks(kiln, 100);
            h.getLevel().destroyBlock(h.absolutePos(pos), true, actor.player);
            h.assertTrue(fuel.isEmpty() && drops(h, pos, FRContents.Blocks.TRIAL_KILN_BALANCED.get().asItem()) == 1 && drops(h, pos, Items.STICK) == 0, "mid-firing destruction returns raw kiln once without fuel");
            h.assertTrue(kiln.settle().isEmpty(), "removal has settled the old device");
            var fresh = kiln(h, new BlockPos(4, 1, 1), FRContents.Blocks.TRIAL_KILN_BALANCED.get());
            h.assertTrue(fresh.remaining() == 0 && fresh.getBlockState().getValue(TrialKilnBlock.PHASE) == 0, "newly placed raw kiln has no retained progress");
            fresh.start(service, new ItemStack(Items.STICK, 4)); ticks(fresh, 400);
            for (int i = 0; i < actor.player.getInventory().getContainerSize(); i++) actor.player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            fresh.collect(actor.player);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, actor.player.getBoundingBox().inflate(2)).stream().filter(e -> e.getItem().is(FRContents.Items.FIRED_TRIAL_KILN.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1 && fresh.settle().isEmpty(), "shared collection delivery falls back to one drop when inventory is full");
            var raw = kiln(h, new BlockPos(7, 1, 1), FRContents.Blocks.TRIAL_KILN_CLAY_RICH.get());
            h.getLevel().destroyBlock(raw.getBlockPos(), true, actor.player);
            h.assertTrue(drops(h, new BlockPos(7, 1, 1), FRContents.Blocks.TRIAL_KILN_CLAY_RICH.get().asItem()) == 1, "unfired destruction returns its raw ratio");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }

    @GameTest(batch = "m3_trial_kiln", template = "trial_kiln_empty", timeoutTicks = 40)
    public static void realPlayerInventorySamplesAndDeskPriorityConsumeOnlyOnce(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current(); install();
        try (var actor = new PlayerFixture(h)) {
            for (int slot : new int[]{0, 9, 35}) {
                var service = service(); var run = begin(service, actor.player);
                var binding = service.sampleTargets(TrialKilnBlockEntity.EXPERIMENT).get(0);
                var good = FRContents.Items.FIRED_TRIAL_KILN.get().getDefaultInstance(); binding.write(good);
                var wrong = good.copy(); new ResearchSampleBinding(binding.team(), UUID.randomUUID(), binding.requirement(), binding.experiment()).write(wrong);
                var badDesk = FRContents.Items.CRACKED_TRIAL_KILN.get().getDefaultInstance(); binding.write(badDesk);
                actor.player.getInventory().clearContent();
                actor.player.getInventory().setItem(1, wrong);
                actor.player.getInventory().setItem(slot, good);
                var extra = good.copy(); actor.player.getInventory().setItem(slot == 35 ? 40 : 20, extra);
                h.assertTrue(service.submitSample(run, "submit_intact", badDesk, actor.player.getInventory().items).succeeded(), "actual hotbar/main inventory stack submits from slot " + slot);
                h.assertTrue(good.isEmpty() && extra.getCount() == 1 && wrong.getCount() == 1 && badDesk.getCount() == 1,
                        "only the first matching current-run sample is consumed; wrong desk and wrong-run items remain");
                h.assertTrue(service.submitSample(run, "submit_intact", badDesk, actor.player.getInventory().items).status() == KnowledgeService.Status.ALREADY_OWNED
                        && extra.getCount() == 1, "duplicate submission does not fall through and consume another sample");
            }
            var service = service(); var run = begin(service, actor.player); var binding = service.sampleTargets(TrialKilnBlockEntity.EXPERIMENT).get(0);
            var deskSample = FRContents.Items.FIRED_TRIAL_KILN.get().getDefaultInstance(); binding.write(deskSample);
            var carriedSample = deskSample.copy(); actor.player.getInventory().clearContent(); actor.player.getInventory().setItem(9, carriedSample);
            h.assertTrue(service.submitSample(run, "submit_intact", deskSample, actor.player.getInventory().items).succeeded()
                    && deskSample.isEmpty() && carriedSample.getCount() == 1, "a valid desk sample retains priority over the player's inventory");
            var missingService = service(); var missingRun = begin(missingService, actor.player);
            h.assertTrue(!missingService.submitSample(missingRun, "submit_intact", ItemStack.EMPTY, actor.player.getInventory().items).succeeded()
                    && carriedSample.getCount() == 1, "a leftover sample from another run is not consumed when no matching item exists");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }

    @GameTest(batch = "m3_trial_kiln", template = "trial_kiln_empty", timeoutTicks = 40)
    public static void sampleSubmissionBindsRunTeamAndRequirementAndRoundTrips(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current(); install();
        try (var actor = new PlayerFixture(h)) {
            var service = service(); UUID run = begin(service, actor.player);
            var binding = service.sampleTargets(TrialKilnBlockEntity.EXPERIMENT).get(0);
            ItemStack good = FRContents.Items.FIRED_TRIAL_KILN.get().getDefaultInstance(); binding.write(good);
            ItemStack bad = FRContents.Items.CRACKED_TRIAL_KILN.get().getDefaultInstance(); binding.write(bad);
            h.assertTrue(!service.submitSample(run, "submit_intact", bad).succeeded() && bad.getCount() == 1, "wrong physical sample is not consumed");
            ItemStack unbound = FRContents.Items.FIRED_TRIAL_KILN.get().getDefaultInstance();
            h.assertTrue(!service.submitSample(run, "submit_intact", unbound).succeeded() && unbound.getCount() == 1, "pre-study unbound sample does not qualify");
            for (var wrong : List.of(new ResearchSampleBinding(UUID.randomUUID(), run, binding.requirement(), binding.experiment()),
                    new ResearchSampleBinding(binding.team(), UUID.randomUUID(), binding.requirement(), binding.experiment()),
                    new ResearchSampleBinding(binding.team(), run, "other_task", binding.experiment()),
                    new ResearchSampleBinding(binding.team(), run, binding.requirement(), id("other_experiment")))) {
                var stack = good.copy(); wrong.write(stack);
                h.assertTrue(!service.submitSample(run, "submit_intact", stack).succeeded() && stack.getCount() == 1, "wrong binding never consumes");
            }
            h.setBlock(new BlockPos(1, 1, 1), FRContents.Blocks.DRAWING_DESK.get());
            var desk = (DrawingDeskTileEntity) h.getBlockEntity(new BlockPos(1, 1, 1));
            desk.getInventory().setStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT, good.copy());
            h.assertTrue(service.submitSample(run, "submit_intact", desk.getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT)).succeeded()
                && desk.getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT).isEmpty(), "submission consumes the desk's actual examination-slot stack");
            var repeat = good.copy(); h.assertTrue(service.submitSample(run, "submit_intact", repeat).status() == KnowledgeService.Status.ALREADY_OWNED && repeat.getCount() == 1, "repeat click does not consume again");
            var restored = KnowledgeState.read(service.state().write());
            h.assertTrue(restored.research().get(run).equals(service.state().research().get(run)) && KnowledgeState.SCHEMA_VERSION == 5, "schema5 round trip preserves schema4 sample receipt and prior progress");
            CompoundTag old = service.state().write(); old.putInt("schema_version", 3); old.getList("research", Tag.TAG_COMPOUND).getCompound(0).remove("samples");
            var legacy = KnowledgeState.read(old).research().get(run);
            h.assertTrue(legacy.samples().isEmpty() && legacy.origins().equals(service.state().research().get(run).origins()), "v3 missing sample progress defaults empty without dropping the run");
            service.abandonResearch(run); UUID next = service.startResearch(id("trial_kiln")).orElseThrow();
            h.assertTrue(!service.submitSample(next, "submit_intact", repeat).succeeded() && repeat.getCount() == 1, "old run sample cannot serve a restarted run");
            var definitions = KnowledgeDefinitions.current(); var projects = new HashMap<>(definitions.projects()); projects.put(id("second_trial"), projects.get(id("trial_kiln")));
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0, definitions.ideas(), definitions.results(), definitions.links(), definitions.itemRetrievalRules(), projects, List.of()));
            service.startResearch(id("second_trial")).orElseThrow();
            var device = kiln(h, new BlockPos(4, 1, 1), FRContents.Blocks.TRIAL_KILN_BALANCED.get()); ItemStack fuel = new ItemStack(Items.STICK, 4);
            h.assertTrue(device.start(service, fuel) == TrialKilnBlockEntity.Start.CHOOSE_TARGET && fuel.getCount() == 4, "ambiguous active targets do not consume fuel or choose silently");
            var chosen = device.cycleTarget(service).orElseThrow();
            h.assertTrue(device.start(service, fuel) == TrialKilnBlockEntity.Start.STARTED && device.binding().orElseThrow().equals(chosen), "empty-hand target choice binds the selected run");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
}
