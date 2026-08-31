/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.async;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalRuntimeTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalDimensionMailboxTest {
    @BeforeEach
    void stopPreviousPool() {
        ThermalWorkerPool.closeShared();
    }

    @AfterEach
    void stopPool() {
        ThermalWorkerPool.closeShared();
    }

    @Test
    void oneBatchRunsAsynchronouslyAndRequiresAnExplicitAck() throws Exception {
        ThermalWorkerPool pool = ThermalWorkerPool.startShared();
        FakeProcessor processor = new FakeProcessor(false);
        ThermalDimensionMailbox mailbox = new ThermalDimensionMailbox(
                pool, processor);

        ThermalInputBatch batch = ThermalRuntimeTestFixtures.batch(
                1L, 0L,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ResolvedGeometryBatch.EMPTY);
        assertTrue(mailbox.submit(batch));
        assertTrue(processor.started.await(2L, TimeUnit.SECONDS));
        ThermalCompletion completion = awaitCompletion(mailbox);
        assertEquals(ThermalCompletion.Status.COMPLETED,
                completion.status());
        mailbox.acknowledgeCompletion(1L);
        assertTrue(mailbox.submit(ThermalRuntimeTestFixtures.batch(
                2L, 0L,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ResolvedGeometryBatch.EMPTY)));

        mailbox.close();
        assertTrue(processor.closed.await(2L, TimeUnit.SECONDS));
    }

    @Test
    void mailboxRejectsASecondBatchAndClosesAQueuedProcessorOnce() throws Exception {
        ThermalWorkerPool pool = ThermalWorkerPool.startShared();
        FakeProcessor processor = new FakeProcessor(true);
        ThermalDimensionMailbox mailbox = new ThermalDimensionMailbox(
                pool, processor);
        ThermalInputBatch batch = ThermalRuntimeTestFixtures.batch(
                1L, 0L,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ResolvedGeometryBatch.EMPTY);

        assertTrue(mailbox.submit(batch));
        assertTrue(processor.started.await(2L, TimeUnit.SECONDS));
        assertFalse(mailbox.submit(batch));
        mailbox.close();
        processor.release.countDown();

        assertTrue(processor.closed.await(2L, TimeUnit.SECONDS));
        assertEquals(1, processor.closeCalls);
    }

    @Test
    void processorFailureProducesOneTerminalCompletion() throws Exception {
        ThermalWorkerPool pool = ThermalWorkerPool.startShared();
        FakeProcessor processor = new FakeProcessor(false);
        processor.fail = true;
        ThermalDimensionMailbox mailbox = new ThermalDimensionMailbox(
                pool, processor);

        assertTrue(mailbox.submit(ThermalRuntimeTestFixtures.batch(
                1L, 0L,
                ThermalInputBatch.NO_ADMISSIONS,
                ThermalInputBatch.NO_RETIREMENTS,
                ResolvedGeometryBatch.EMPTY)));
        ThermalCompletion completion = awaitCompletion(mailbox);
        assertEquals(ThermalCompletion.Status.ENGINE_FAILED,
                completion.status());
        assertNotNull(completion.failure());
        mailbox.acknowledgeCompletion(1L);
        assertTrue(processor.closed.await(2L, TimeUnit.SECONDS));
    }

    private static ThermalCompletion awaitCompletion(
            ThermalDimensionMailbox mailbox
    ) throws InterruptedException {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(2L);
        ThermalCompletion completion;
        do {
            completion = mailbox.peekCompletion();
            if (completion != null) return completion;
            Thread.onSpinWait();
        } while (System.nanoTime() < deadline);
        throw new AssertionError("worker completion was not acknowledged");
    }

    private static final class FakeProcessor
            implements ThermalDimensionProcessor {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final boolean block;
        private boolean fail;
        private int closeCalls;

        private FakeProcessor(boolean block) {
            this.block = block;
        }

        @Override
        public ThermalCompletion process(ThermalInputBatch batch) {
            started.countDown();
            if (block) {
                try {
                    release.await(2L, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            if (fail) throw new IllegalStateException("test failure");
            return new ThermalCompletion(
                    1L,
                    batch.sequence(),
                    ThermalCompletion.Status.COMPLETED,
                    null,
                    ThermalCompletion.NO_PHASE_REQUESTS,
                    ThermalCompletion.NO_RESYNC_TOKENS,
                    ThermalCompletion.NO_RESIDENCY_UPDATES);
        }

        @Override
        public void close() {
            closeCalls++;
            closed.countDown();
        }
    }
}
