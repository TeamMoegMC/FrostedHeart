package com.teammoeg.chorda.scheduler;

import com.teammoeg.chorda.ChordaConfig;
import com.teammoeg.chorda.Chorda;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main-thread round robin, owned by actual world sessions rather than dimension names. */
public final class SchedulerQueue {
    private static final Map<ServerLevel, SchedulerQueue> QUEUES = new IdentityHashMap<>();
    private final LinkedHashMap<BlockPos, Registration> tasks = new LinkedHashMap<>();
    private double fractionalBudget;
    private boolean closed;

    public static final class Registration implements AutoCloseable {
        private final SchedulerQueue queue;
        private final BlockEntity blockEntity;
        private boolean cancelled;

        private Registration(SchedulerQueue queue, BlockEntity blockEntity) {
            this.queue = queue;
            this.blockEntity = blockEntity;
        }

        @Override public void close() {
            cancelled = true;
            queue.tasks.remove(blockEntity.getBlockPos(), this);
        }
    }

    public static Registration register(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        SchedulerQueue queue = QUEUES.computeIfAbsent(level, ignored -> new SchedulerQueue());
        Registration previous = queue.tasks.get(blockEntity.getBlockPos());
        if (previous != null && previous.blockEntity == blockEntity) {
            return previous;
        }
        if (previous != null) {
            previous.close();
        }
        Registration registration = new Registration(queue, blockEntity);
        queue.tasks.put(blockEntity.getBlockPos().immutable(), registration);
        return registration;
    }

    public static void add(BlockEntity blockEntity) {
        register(blockEntity);
    }

    public static void tickAll(ServerLevel level) {
        SchedulerQueue queue = QUEUES.get(level);
        if (queue != null) {
            queue.tick();
        }
    }

    private void tick() {
        fractionalBudget += ChordaConfig.SERVER.taskPerTick.get();
        int taskBudget = Math.min(tasks.size(), (int) fractionalBudget);
        fractionalBudget -= (int) fractionalBudget;
        for (int count = 0; count < taskBudget && !tasks.isEmpty(); count++) {
            var iterator = tasks.entrySet().iterator();
            Registration registration = iterator.next().getValue();
            iterator.remove();
            BlockEntity blockEntity = registration.blockEntity;
            if (!registration.cancelled && blockEntity instanceof ScheduledTaskTileEntity task && task.isStillValid()) {
                try {
                    task.executeTask();
                } catch (RuntimeException failure) {
                    Chorda.LOGGER.error("Scheduled block task failed at {}", blockEntity.getBlockPos(), failure);
                } finally {
                    if (!closed && !registration.cancelled && task.isStillValid()) {
                        tasks.putIfAbsent(blockEntity.getBlockPos(), registration);
                    }
                }
            }
        }
    }

    public static void closeWorld(ServerLevel level) {
        SchedulerQueue queue = QUEUES.remove(level);
        if (queue != null) {
            queue.closed = true;
            for (Registration registration : new ArrayList<>(queue.tasks.values())) {
                registration.close();
            }
        }
    }

    public static void reset() {
        for (ServerLevel level : new ArrayList<>(QUEUES.keySet())) {
            closeWorld(level);
        }
    }
}
