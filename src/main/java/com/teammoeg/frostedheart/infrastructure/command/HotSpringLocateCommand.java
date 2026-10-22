package com.teammoeg.frostedheart.infrastructure.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bounded, opt-in debugging search for existing hot-spring fluid, including old or hand-placed pools. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HotSpringLocateCommand {
    static final int DEFAULT_RADIUS = 16;
    static final int MAX_RADIUS = 32;
    static final int CHUNKS_PER_TICK = 16;
    static final int SECTIONS_PER_TICK = 64;
    private static final int CHUNK_TIMEOUT_TICKS = 600;
    private static final int SEARCH_TIMEOUT_TICKS = 12_000;
    private static final TicketType<ChunkPos> SEARCH_TICKET = TicketType.create(
            "fh_hot_spring_locate", Comparator.comparingLong(ChunkPos::toLong));
    // One server-wide job bounds generation and scanning even if several operators use the command.
    private static Search active;

    private HotSpringLocateCommand() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        // Do not use registerCommand: its unprefixed alias would merge into vanilla /locate.
        for (String alias : CommandHelper.COMMANDS) {
            event.getDispatcher().register(Commands.literal(alias).then(command()));
        }
    }

    static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("locate").requires(source -> source.hasPermission(2))
                .then(Commands.literal("hot_spring")
                        .executes(context -> start(context.getSource(), DEFAULT_RADIUS, false))
                        .then(Commands.argument("radius_chunks", IntegerArgumentType.integer(0, MAX_RADIUS))
                                .executes(context -> start(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius_chunks"), false)))
                        .then(Commands.literal("generate")
                                .executes(context -> start(context.getSource(), DEFAULT_RADIUS, true))
                                .then(Commands.argument("radius_chunks", IntegerArgumentType.integer(0, MAX_RADIUS))
                                        .executes(context -> start(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius_chunks"), true))))
                        .then(Commands.literal("cancel").executes(context -> cancel(context.getSource())))
                        .then(Commands.literal("status").executes(context -> status(context.getSource()))));
    }

    private static int start(CommandSourceStack source, int radius, boolean generate) {
        if (active != null) {
            source.sendFailure(Component.literal("A hot-spring search is already running. Use /fh locate hot_spring status or cancel."));
            return 0;
        }
        active = new Search(source, radius, generate);
        source.sendSuccess(() -> Component.literal("Searching for actual hot-spring fluid in "
                + active.level.dimension().location() + ", centered on chunk " + active.center
                + ", radius " + radius + " chunks (" + active.chunks.size() + " target chunks, full build height). "
                + (generate ? "GENERATE mode loads/generates terrain and its vanilla dependencies, one target at a time."
                        : "Loaded chunks only; unloaded chunks are skipped. Use the explicit generate subcommand to search new terrain.")
                + " Stops at the first result; cancel with /fh locate hot_spring cancel."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(active == null ? "No hot-spring search is running."
                : active.progress()), false);
        return active == null ? 0 : 1;
    }

    private static int cancel(CommandSourceStack source) {
        if (active == null) return status(source);
        Search search = active;
        active = null;
        search.releaseTicket();
        source.sendSuccess(() -> Component.literal("Hot-spring search cancelled. " + search.progress()
                + " Already generated terrain is retained."), false);
        return 1;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) return;
        Search search = active;
        if (search.source.getServer() != event.getServer()) return;
        if (search.source.getEntity() instanceof ServerPlayer player
                && (player.hasDisconnected() || player.serverLevel() != search.level)) {
            search.releaseTicket();
            active = null;
            if (!player.hasDisconnected()) search.source.sendFailure(Component.literal("Hot-spring search cancelled after changing dimension."));
        } else if (search.tick()) {
            search.releaseTicket();
            active = null;
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        if (active != null) active.releaseTicket();
        active = null;
    }

    static List<ChunkPos> chunksInRange(ChunkPos center, int radius) {
        List<ChunkPos> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        chunks.add(center);
        for (int ring = 1; ring <= radius; ring++) {
            for (int x = -ring; x <= ring; x++) {
                chunks.add(new ChunkPos(center.x + x, center.z - ring));
                chunks.add(new ChunkPos(center.x + x, center.z + ring));
            }
            for (int z = -ring + 1; z < ring; z++) {
                chunks.add(new ChunkPos(center.x - ring, center.z + z));
                chunks.add(new ChunkPos(center.x + ring, center.z + z));
            }
        }
        return chunks;
    }

    static BlockPos findInSection(LevelChunkSection section, ChunkPos chunk, int sectionY) {
        if (!section.maybeHas(state -> state.getFluidState().getType().isSame(FHFluids.HOT_SPRING.get()))) return null;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (section.getBlockState(x, y, z).getFluidState().getType().isSame(FHFluids.HOT_SPRING.get())) {
                        return new BlockPos(chunk.getMinBlockX() + x, (sectionY << 4) + y, chunk.getMinBlockZ() + z);
                    }
                }
            }
        }
        return null;
    }

    static final class Search {
        final CommandSourceStack source;
        final ServerLevel level;
        final ChunkPos center;
        final int radius;
        final boolean generate;
        final List<ChunkPos> chunks;
        int nextChunk;
        int sectionIndex;
        int scanned;
        int skipped;
        int failed;
        int elapsed;
        int waiting;
        private ChunkPos ticket;

        Search(CommandSourceStack source, int radius, boolean generate) {
            this.source = source;
            level = source.getLevel();
            center = new ChunkPos(BlockPos.containing(source.getPosition()));
            this.radius = radius;
            this.generate = generate;
            chunks = chunksInRange(center, radius);
        }

        String progress() {
            return "Hot-spring search: " + level.dimension().location() + ", center " + center + ", radius " + radius
                    + " chunks, mode=" + (generate ? "generate" : "loaded") + ", scanned=" + scanned
                    + ", skipped unloaded/outside border=" + skipped + ", failed/timed out=" + failed + ", targets=" + chunks.size() + ".";
        }

        boolean tick() {
            if (++elapsed > SEARCH_TIMEOUT_TICKS) return finish("Search reached its 10-minute tick limit.");
            if (elapsed % 200 == 0) source.sendSuccess(() -> Component.literal(progress()), false);
            int sectionBudget = SECTIONS_PER_TICK;
            int chunkBudget = CHUNKS_PER_TICK;
            while (nextChunk < chunks.size() && chunkBudget-- > 0 && sectionBudget > 0) {
                ChunkPos pos = chunks.get(nextChunk);
                if (!level.getWorldBorder().isWithinBounds(pos)) {
                    skipped++;
                    advance();
                    continue;
                }
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk == null) {
                    if (!generate) {
                        skipped++;
                        advance();
                        continue;
                    }
                    if (ticket == null) {
                        ticket = pos;
                        // radius=0 requests FULL but not block/entity ticking. Vanilla advances it on later ticks.
                        level.getChunkSource().addRegionTicket(SEARCH_TICKET, pos, 0, pos);
                    }
                    if (++waiting > CHUNK_TIMEOUT_TICKS) {
                        failed++;
                        advance();
                    }
                    return false;
                }
                LevelChunkSection[] sections = chunk.getSections();
                while (sectionIndex < sections.length && sectionBudget-- > 0) {
                    int index = sectionIndex++;
                    BlockPos found = findInSection(sections[index], pos, level.getSectionYFromSectionIndex(index));
                    if (found != null) {
                        report(found);
                        return true;
                    }
                }
                if (sectionIndex == sections.length) {
                    scanned++;
                    advance();
                }
            }
            if (nextChunk == chunks.size()) return finish("No hot-spring fluid found in the scanned chunks.");
            return false;
        }

        private void advance() {
            releaseTicket();
            nextChunk++;
            sectionIndex = 0;
            waiting = 0;
        }

        void releaseTicket() {
            if (ticket != null) {
                level.getChunkSource().removeRegionTicket(SEARCH_TICKET, ticket, 0, ticket);
                ticket = null;
            }
        }

        private boolean finish(String reason) {
            source.sendSuccess(() -> Component.literal(reason + " " + progress()
                    + " Skipped/failed chunks are unsearched; this does not prove absence elsewhere."), false);
            return true;
        }

        private void report(BlockPos found) {
            String dimension = level.dimension().location().toString();
            String teleport = "/execute in " + dimension + " run tp @s " + (found.getX() + 0.5)
                    + " " + (found.getY() + 1) + " " + (found.getZ() + 0.5);
            Component coordinates = Component.literal("[" + found.getX() + ", " + found.getY() + ", " + found.getZ() + "]")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, teleport))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Suggest teleport one block above the fluid: " + teleport))));
            source.sendSuccess(() -> Component.literal("Hot-spring fluid found in " + dimension + " at ")
                    .append(coordinates).append(". Click to fill a teleport command. First match in chunk-ring order, not guaranteed nearest. ")
                    .append(progress()), false);
        }
    }
}
