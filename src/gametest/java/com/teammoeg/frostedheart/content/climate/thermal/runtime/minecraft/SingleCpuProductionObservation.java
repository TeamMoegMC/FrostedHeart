package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import java.lang.management.ManagementFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development metadata for actual CPU-affinity runs, with no change to scheduling or heat. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class SingleCpuProductionObservation {
    private static final boolean ENABLED = "true".equals(System.getenv("FH_SINGLE_CPU_VALIDATION"));
    private static final boolean OBSERVE_SHUTDOWN = "true".equals(System.getenv("FH_CONTINUOUS_AIR_SHUTDOWN_OBSERVATION"));
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void started(ServerStartedEvent event) {
        if (!ENABLED) return;
        int processors = Runtime.getRuntime().availableProcessors();
        int workers = ThermalWorkerPool.shared().parallelism();
        FHMain.LOGGER.info("SINGLE_CPU_RUNTIME,pid={},processors={},thermalWorkers={},maxHeap={},gc={}",
                ProcessHandle.current().pid(), processors, workers, Runtime.getRuntime().maxMemory(),
                ManagementFactory.getGarbageCollectorMXBeans().stream().map(bean -> bean.getName()).toList());
        FHMain.LOGGER.info("SINGLE_CPU_BACKEND,{}", "regional shared-grid");
        if (processors != 1 || workers != 1)
            throw new IllegalStateException("Single-CPU test was not launched with one visible processor and one thermal worker");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopped(ServerStoppedEvent event) {
        if (ENABLED) FHMain.LOGGER.info("SINGLE_CPU_STOP,pid={},processors={},rootBytes={}",
                ProcessHandle.current().pid(), Runtime.getRuntime().availableProcessors(), MinecraftThermalInput.thermalBudgetUsedBytes());
        if (OBSERVE_SHUTDOWN) FHMain.LOGGER.info("THERMAL_SHUTDOWN_ROOT,pid={},rootBytes={}",
                ProcessHandle.current().pid(), MinecraftThermalInput.thermalBudgetUsedBytes());
    }
}
