/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Observes actual production builders without keeping them alive. */
public final class PreparationLifetimeProbe implements AutoCloseable {
    private record Observed(long tick, WeakReference<RegionalAirRuntime.RebuildPreparation> reference) {}
    private final List<Observed> observed = new ArrayList<>();
    private final ThermalDiagnostics.Observer previous;
    private final ThermalDiagnostics.Observer installed;

    public PreparationLifetimeProbe() {
        previous = ThermalDiagnostics.observer;
        installed = (ThermalDiagnostics.Observer) Proxy.newProxyInstance(ThermalDiagnostics.Observer.class.getClassLoader(),
                new Class<?>[] {ThermalDiagnostics.Observer.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("preparationCreated"))
                        preparationCreated((RegionalAirRuntime.RebuildPreparation) arguments[0]);
                    try {
                        if (previous != null) return method.invoke(previous, arguments);
                        if (method.getDeclaringClass() == Object.class) return method.invoke(this, arguments);
                        return InvocationHandler.invokeDefault(proxy, method, arguments == null ? new Object[0] : arguments);
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        ThermalDiagnostics.observer = installed;
    }

    private synchronized void preparationCreated(RegionalAirRuntime.RebuildPreparation preparation) {
        if (observed.size() < 512) observed.add(new Observed(preparation.targetTick, new WeakReference<>(preparation)));
    }

    public synchronized List<WeakReference<RegionalAirRuntime.RebuildPreparation>> completedBefore(long tick) {
        var result = new ArrayList<WeakReference<RegionalAirRuntime.RebuildPreparation>>();
        for (var entry : observed) {
            var preparation = entry.reference.get();
            if (entry.tick <= tick && (preparation == null || preparation.closed)) result.add(entry.reference);
        }
        return result;
    }

    @Override public void close() {
        if (ThermalDiagnostics.observer == installed) ThermalDiagnostics.observer = previous;
    }
}
