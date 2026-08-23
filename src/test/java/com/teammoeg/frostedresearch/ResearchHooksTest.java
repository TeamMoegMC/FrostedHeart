package com.teammoeg.frostedresearch;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchHooksTest {

    @Test
    void recipeOwnerContextIsNestedAndExceptionSafe() {
        UUID outer = UUID.randomUUID();
        UUID inner = UUID.randomUUID();

        assertNull(ResearchHooks.currentRecipeOwner());
        assertThrows(IllegalStateException.class, () -> ResearchHooks.withRecipeOwner(outer, () -> {
            assertEquals(outer, ResearchHooks.currentRecipeOwner());
            ResearchHooks.withRecipeOwner(inner, () -> {
                assertEquals(inner, ResearchHooks.currentRecipeOwner());
                return null;
            });
            assertEquals(outer, ResearchHooks.currentRecipeOwner());
            throw new IllegalStateException("expected");
        }));
        assertNull(ResearchHooks.currentRecipeOwner());
    }

    @Test
    void recipeOwnerContextIsIsolatedBetweenThreads() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread firstThread = ownerThread(first, entered, release, failure);
        Thread secondThread = ownerThread(second, entered, release, failure);
        firstThread.start();
        secondThread.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertNull(ResearchHooks.currentRecipeOwner());
        release.countDown();
        firstThread.join(5_000);
        secondThread.join(5_000);

        assertFalse(firstThread.isAlive());
        assertFalse(secondThread.isAlive());
        if (failure.get() != null)
            throw new AssertionError("recipe owner context leaked between threads", failure.get());
    }

    private static Thread ownerThread(UUID owner, CountDownLatch entered, CountDownLatch release,
            AtomicReference<Throwable> failure) {
        return new Thread(() -> {
            try {
                ResearchHooks.withRecipeOwner(owner, () -> {
                    assertEquals(owner, ResearchHooks.currentRecipeOwner());
                    entered.countDown();
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    assertEquals(owner, ResearchHooks.currentRecipeOwner());
                    return null;
                });
                assertNull(ResearchHooks.currentRecipeOwner());
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });
    }

    @Test
    void incompleteMatchingKillClueCompletes() {
        assertTrue(ResearchHooks.shouldCompleteKillClue(false, true));
    }

    @Test
    void completedOrMismatchedKillClueDoesNotCompleteAgain() {
        assertFalse(ResearchHooks.shouldCompleteKillClue(true, true));
        assertFalse(ResearchHooks.shouldCompleteKillClue(false, false));
        assertFalse(ResearchHooks.shouldCompleteKillClue(true, false));
    }
}
