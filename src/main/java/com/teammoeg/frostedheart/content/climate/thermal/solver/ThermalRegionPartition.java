/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;

/** Optional structural-region summary of a compiled operator. */
final class ThermalRegionPartition {
    static String describe(AirFieldLayout layout, AirOperatorFragment[] volumes,
            CoupledThermalOperator.Contacts contacts, ThermalFragment[] fragments, int[] materialSlots, Int2IntOpenHashMap unknownBySlot, ThermalCellArena arena) {
        int airCount = layout.coefficientCount(), size = airCount + materialSlots.length;
        int[] parents = new int[size], ranks = new int[size];
        for (int row = 0; row < size; row++) parents[row] = row;
        for (AirOperatorFragment volume : volumes) {
            long cells = volume.cells();
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells); cells &= cells - 1;
                for (int index = 1; index < 8; index++) union(parents, ranks, volume.index(cell, 0), volume.index(cell, index));
            }
        }
        for (int contact = 0; contact < contacts.surfaceNodes.size(); contact++) {
            int row = contacts.surfaceNodes.getInt(contact);
            if (row >= 0 && contacts.surfaceConductance.getDouble(contact >>> 2) != 0)
                union(parents, ranks, row, airCount + contacts.surfaceBodies.getInt(contact >>> 2));
        }
        for (int contact = 0; contact < contacts.conductances.size(); contact++) {
            if (contacts.conductances.getDouble(contact) == 0) continue;
            int start = contacts.offsets.getInt(contact), end = contacts.offsets.getInt(contact + 1);
            if (start == end) continue;
            int first = contacts.indices.getInt(start);
            for (int entry = start + 1; entry < end; entry++) union(parents, ranks, first, contacts.indices.getInt(entry));
            if (!contacts.localBodies.isEmpty() && contacts.localBodies.getInt(contact) >= 0)
                union(parents, ranks, first, airCount + contacts.localBodies.getInt(contact));
        }
        int beforeInterior = airRootCount(parents, airCount);
        // Waiting and current zero temperature difference never disconnect future conduction.
        for (ThermalFragment fragment : fragments) {
            var material = fragment.materialContacts();
            for (int edge = 0; edge < material.size(); edge++) {
                if (material.conductance(edge) == 0) continue;
                int first = lookup(unknownBySlot, material.first(edge)), second = lookup(unknownBySlot, material.second(edge));
                if (first >= 0 && second >= 0) union(parents, ranks, first, second);
            }
        }
        int[] airSizes = new int[size], materialSizes = new int[size];
        int[] minimumX = new int[size], maximumX = new int[size];
        Arrays.fill(minimumX, Integer.MAX_VALUE); Arrays.fill(maximumX, Integer.MIN_VALUE);
        for (int row = 0; row < airCount; row++) airSizes[find(parents, row)]++;
        for (int body = 0; body < materialSlots.length; body++) {
            int root = find(parents, airCount + body);
            materialSizes[root]++;
            int x = arena.minimum(materialSlots[body], 0);
            minimumX[root] = Math.min(minimumX[root], x); maximumX[root] = Math.max(maximumX[root], x + 1);
        }
        for (AirOperatorFragment volume : volumes) {
            long cells = volume.cells();
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells); cells &= cells - 1;
                int root = find(parents, volume.index(cell, 0));
                minimumX[root] = Math.min(minimumX[root], volume.component.minX);
                maximumX[root] = Math.max(maximumX[root], volume.component.minX + 4);
            }
        }
        ArrayList<Integer> roots = new ArrayList<>();
        for (int row = 0; row < size; row++) if (airSizes[row] != 0 || materialSizes[row] != 0) roots.add(row);
        roots.sort(Comparator.<Integer>comparingInt(row -> minimumX[row]).thenComparingInt(row -> row));
        int airRegions = 0, materialOnly = 0, largestAir = 0, largestMaterial = 0;
        StringBuilder sizes = new StringBuilder();
        for (int region = 0; region < roots.size(); region++) {
            int root = roots.get(region);
            largestAir = Math.max(largestAir, airSizes[root]); largestMaterial = Math.max(largestMaterial, materialSizes[root]);
            if (airSizes[root] == 0) { materialOnly++; continue; }
            airRegions++;
            if (sizes.length() > 0) sizes.append(';');
            sizes.append(airSizes[root]).append('/').append(materialSizes[root]).append('@').append(minimumX[root]).append(':').append(maximumX[root]);
        }
        return "structuralRegions=" + roots.size()
                + ",airRegions=" + airRegions + ",airRegionsBeforeMaterialConduction=" + beforeInterior
                + ",materialOnlyRegions=" + materialOnly + ",largestAirRegion=" + largestAir
                + ",largestMaterialRegion=" + largestMaterial + ",regionAirMaterialSizesAndX=" + sizes;
    }

    private static int lookup(Int2IntOpenHashMap rows, int slot) { return rows.get(slot); }
    private static int airRootCount(int[] parents, int airCount) {
        boolean[] seen = new boolean[parents.length];
        int count = 0;
        for (int row = 0; row < airCount; row++) {
            int root = find(parents, row);
            if (!seen[root]) { seen[root] = true; count++; }
        }
        return count;
    }
    private static int find(int[] parents, int row) {
        int root = row;
        while (parents[root] != root) root = parents[root];
        while (parents[row] != row) { int next = parents[row]; parents[row] = root; row = next; }
        return root;
    }
    private static void union(int[] parents, int[] ranks, int first, int second) {
        int a = find(parents, first), b = find(parents, second);
        if (a == b) return;
        if (ranks[a] < ranks[b]) parents[a] = b;
        else { parents[b] = a; if (ranks[a] == ranks[b]) ranks[a]++; }
    }
}
