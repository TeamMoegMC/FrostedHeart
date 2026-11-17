/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_source_admission")
@PrefixGameTestTemplate(false)
public final class ThermalSourceAdmissionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void fullServerRefusesNewProducersButKeepsAcceptedEnergy(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1024 * 1024);
        try (var first = new SpatialSourceTimeline(budget.createCategory(), 16);
                var second = new SpatialSourceTimeline(budget.createCategory(), 16)) {
            helper.assertTrue(first.register(descriptor(1), 20, true, 0), "Admit the original producer");
            var pressure = budget.tryReserve(budget.limitBytes() - budget.usedBytes());
            helper.assertTrue(pressure != null, "Fill the shared server budget with another owner's live data");
            try (pressure) {
                long full = budget.usedBytes();
                helper.assertTrue(!second.register(descriptor(2), 20, true, 0),
                        "A different dimension cannot admit a new producer beyond the shared target");
                helper.assertTrue(budget.usedBytes() == full && budget.peakUsedBytes() == budget.limitBytes(),
                        "A refused registration must not grow either indexes or producer backing");
                first.power(1, 40, 20);
                var windows = first.seal(40);
                helper.assertTrue(windows.length == 1 && Math.abs(energy(windows[0]) - 60) < 1e-9,
                        "Accepted power intervals continue: 20W for 1s plus 40W for 1s");
                try (var window = windows[0]) {
                    helper.assertTrue(window.acceptPort(0, 60) == 60, "The worker records the accepted energy exactly once");
                    window.acceptedThrough(40);
                }
                first.unload(1, 1, 60);
                var terminal = first.seal(60);
                helper.assertTrue(terminal.length == 1 && terminal[0].unload && Math.abs(energy(terminal[0]) - 40) < 1e-9,
                        "Retirement still records the last accepted second under pressure");
                terminal[0].close();
                helper.assertTrue(first.seal(60).length == 0, "The settled generation leaves no repeated terminal input");
            }
            helper.assertTrue(second.register(descriptor(2), 20, true, 60),
                    "A source can be admitted once real shared capacity is released");
        }
        helper.assertTrue(budget.usedBytes() == 0, "All input and index owners release their budget");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void prepaidRegistrationCommitsAtFullBudgetAndCancellationHasNoInput(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1024 * 1024);
        try (var timeline = new SpatialSourceTimeline(budget, 16)) {
            var prepaid = timeline.prepareRegistration(1);
            var cancelled = timeline.prepareRegistration(1);
            helper.assertTrue(prepaid != null && cancelled != null, "Reserve lifetime storage before creating source targets");
            helper.assertTrue(timeline.seal(0).length == 0, "A reservation alone produces no control or energy input");
            try (prepaid; cancelled; var pressure = budget.tryReserve(budget.limitBytes() - budget.usedBytes())) {
                helper.assertTrue(pressure != null && timeline.prepareRegistration(1) == null,
                        "No additional lifetime can be allocated at the target");
                long full = budget.usedBytes();
                helper.assertTrue(timeline.register(descriptor(1), 20, true, 0, prepaid),
                        "The already prepaid producer can finish registration without an optional allocation");
                prepaid.close();
                helper.assertTrue(budget.usedBytes() == full, "Closing the transferred token cannot release live producer storage");
                cancelled.close();
                helper.assertTrue(budget.usedBytes() < full, "Removing a source before its first cut returns its unused credit");
                var windows = timeline.seal(20);
                helper.assertTrue(windows.length == 1 && Math.abs(energy(windows[0]) - 20) < 1e-9,
                        "Only the committed producer appears in transport");
                windows[0].close();
            }
        }
        helper.assertTrue(budget.usedBytes() == 0, "Prepaid, cancelled and committed lifetimes all release once");
        helper.succeed();
    }

    private static SpatialSourceTimeline.Descriptor descriptor(long id) {
        return new SpatialSourceTimeline.Descriptor(id, 1, ThermalSourceMode.POWER_SOURCE,
                0, 64, 0, 1, new EmissionPort[] {EmissionPort.of(0, 1, SourceBinding.declaredLoss(id))});
    }

    private static double energy(SpatialSourceTimeline.Window window) {
        double joules = 0;
        for (int index = 0; index < window.size; index++) joules += window.span(index).joules();
        return joules;
    }
}
