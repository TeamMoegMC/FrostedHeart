/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalMaterialEdge;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalMaterialExecution;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Arrays;

/** Sparse deterministic material aggregation for one topology plan. */
final class MaterialEdgeCompiler {
    private final ThermalCellArena arena;
    private final ThermalSolver solver;
    private final LongOpenHashSet affectedKeySet = new LongOpenHashSet();
    private final IntOpenHashSet changedFragmentSet = new IntOpenHashSet();
    private final IntOpenHashSet ownerSet = new IntOpenHashSet();

    private long[] newKey = new long[16];
    private int[] newFragment = new int[16];
    private int[] newOperation = new int[16];
    private long[] newRank = new long[16];
    private double[] newConductance = new double[16];
    private int newCount;

    MaterialEdgeCompiler(ThermalCellArena arena, ThermalSolver solver) {
        this.arena = arena;
        this.solver = solver;
    }

    Result compile(int[] fragmentIndexes, ThermalFragment[] fragments) {
        if (fragmentIndexes.length == 0) {
            return Result.unchanged(solver.materialEdgeCount());
        }
        affectedKeySet.clear();
        changedFragmentSet.clear();
        ownerSet.clear();
        newCount = 0;
        for (int index = 0; index < fragmentIndexes.length; index++) {
            int fragmentIndex = fragmentIndexes[index];
            changedFragmentSet.add(fragmentIndex);
            collectAffected(solver.fragment(fragmentIndex));
            collectNew(fragmentIndex, fragments[index]);
        }
        sortNewContributions(0, newCount - 1);
        long[] keys = affectedKeySet.toLongArray();
        Arrays.sort(keys);
        ThermalMaterialEdge[] edges = new ThermalMaterialEdge[keys.length];
        int expectedSize = solver.materialEdgeCount();
        int possibleInsertions = 0;
        int newCursor = 0;
        for (int keyIndex = 0; keyIndex < keys.length; keyIndex++) {
            long key = keys[keyIndex];
            while (newCursor < newCount && newKey[newCursor] < key) {
                newCursor++;
            }
            int newStart = newCursor;
            while (newCursor < newCount && newKey[newCursor] == key) {
                newCursor++;
            }
            ThermalMaterialEdge old = solver.materialEdge(key);
            ThermalMaterialEdge next = mergeEdge(
                    key, old, newStart, newCursor);
            edges[keyIndex] = next;
            if (old == null && next != null) {
                expectedSize++;
                possibleInsertions++;
            } else if (old != null && next == null) {
                expectedSize--;
            }
            if (old != null) {
                ownerSet.add(old.ownerFragment());
            }
            if (next != null) {
                ownerSet.add(next.ownerFragment());
            }
        }
        int[] owners = ownerSet.toIntArray();
        Arrays.sort(owners);
        int[] additionStarts = new int[owners.length + 1];
        for (ThermalMaterialEdge edge : edges) {
            if (edge != null) {
                int owner = Arrays.binarySearch(
                        owners, edge.ownerFragment());
                additionStarts[owner + 1]++;
            }
        }
        for (int index = 1; index < additionStarts.length; index++) {
            additionStarts[index] += additionStarts[index - 1];
        }
        int[] additionOrder = new int[
                additionStarts[additionStarts.length - 1]];
        int[] additionCursors = Arrays.copyOf(
                additionStarts, owners.length);
        for (int edgeIndex = 0; edgeIndex < edges.length; edgeIndex++) {
            ThermalMaterialEdge edge = edges[edgeIndex];
            if (edge != null) {
                int owner = Arrays.binarySearch(
                        owners, edge.ownerFragment());
                additionOrder[additionCursors[owner]++] = edgeIndex;
            }
        }
        ThermalMaterialExecution[] executions =
                new ThermalMaterialExecution[owners.length];
        for (int index = 0; index < owners.length; index++) {
            executions[index] = rebuildExecution(
                    owners[index], keys, edges,
                    additionOrder,
                    additionStarts[index], additionStarts[index + 1]);
        }
        return new Result(
                keys,
                edges,
                owners,
                executions,
                expectedSize,
                possibleInsertions);
    }

    private void collectAffected(ThermalFragment fragment) {
        ThermalFragment.MaterialContributions material =
                fragment.materialContributions();
        for (int operation = 0; operation < material.size(); operation++) {
            affectedKeySet.add(material.edgeKey(operation));
        }
    }

    private void collectNew(int fragmentIndex, ThermalFragment fragment) {
        ThermalFragment.MaterialContributions material =
                fragment.materialContributions();
        ensureNewCapacity(newCount + material.size());
        for (int operation = 0; operation < material.size(); operation++) {
            long key = material.edgeKey(operation);
            affectedKeySet.add(key);
            newKey[newCount] = key;
            newFragment[newCount] = fragmentIndex;
            newOperation[newCount] = operation;
            newRank[newCount] = fragment.spatialRank();
            newConductance[newCount] = material.conductance(operation);
            newCount++;
        }
    }

    private ThermalMaterialEdge mergeEdge(
            long key,
            ThermalMaterialEdge old,
            int newStart,
            int newEnd
    ) {
        int oldKept = 0;
        if (old != null) {
            for (int index = 0; index < old.contributionCount(); index++) {
                if (!changedFragmentSet.contains(
                        old.contributorFragment(index))) {
                    oldKept++;
                }
            }
        }
        int count = oldKept + newEnd - newStart;
        if (count == 0) {
            return null;
        }
        int[] fragments = new int[count];
        int[] operations = new int[count];
        long[] ranks = new long[count];
        double[] conductances = new double[count];
        int oldCursor = 0;
        int newCursor = newStart;
        int write = 0;
        while (write < count) {
            while (old != null
                    && oldCursor < old.contributionCount()
                    && changedFragmentSet.contains(
                            old.contributorFragment(oldCursor))) {
                oldCursor++;
            }
            boolean hasOld = old != null
                    && oldCursor < old.contributionCount();
            boolean hasNew = newCursor < newEnd;
            boolean takeOld = hasOld && (!hasNew
                    || compare(
                            old.contributorRank(oldCursor),
                            old.contributorOperation(oldCursor),
                            newRank[newCursor],
                            newOperation[newCursor]) <= 0);
            if (takeOld) {
                fragments[write] = old.contributorFragment(oldCursor);
                operations[write] = old.contributorOperation(oldCursor);
                ranks[write] = old.contributorRank(oldCursor);
                conductances[write] = old.contributorConductance(oldCursor);
                oldCursor++;
            } else {
                fragments[write] = newFragment[newCursor];
                operations[write] = newOperation[newCursor];
                ranks[write] = newRank[newCursor];
                conductances[write] = newConductance[newCursor];
                newCursor++;
            }
            write++;
        }
        int first = (int) (key >>> 32);
        int second = (int) key;
        double conductance = 0.0D;
        for (double contribution : conductances) {
            conductance += contribution;
        }
        if (!Double.isFinite(conductance) || conductance < 0.0D) {
            throw new IllegalStateException(
                    "material aggregate conductance is invalid");
        }
        double coefficient =
                ThermalExchangeKernel.compilePairCoefficientJPerK(
                        arena.capacityJPerK(first),
                        arena.capacityJPerK(second),
                        conductance,
                        1.0D);
        return new ThermalMaterialEdge(
                first,
                second,
                fragments,
                operations,
                ranks,
                conductances,
                conductance,
                coefficient,
                fragments[0],
                operations[0]);
    }

    private ThermalMaterialExecution rebuildExecution(
            int owner,
            long[] affectedKeys,
            ThermalMaterialEdge[] replacements,
            int[] additionOrder,
            int additionStart,
            int additionEnd
    ) {
        ThermalMaterialExecution old = solver.materialExecution(owner);
        int retained = 0;
        for (int index = 0; index < old.size(); index++) {
            if (Arrays.binarySearch(affectedKeys, old.key(index)) < 0) {
                retained++;
            }
        }
        int additions = additionEnd - additionStart;
        int count = retained + additions;
        if (count == 0) {
            return ThermalMaterialExecution.EMPTY;
        }
        long[] keys = new long[count];
        int[] first = new int[count];
        int[] second = new int[count];
        double[] conductance = new double[count];
        double[] coefficient = new double[count];
        int[] ownerOperation = new int[count];
        int write = 0;
        for (int index = 0; index < old.size(); index++) {
            if (Arrays.binarySearch(affectedKeys, old.key(index)) >= 0) {
                continue;
            }
            keys[write] = old.key(index);
            first[write] = old.first(index);
            second[write] = old.second(index);
            conductance[write] = old.conductance(index);
            coefficient[write] = old.coefficient(index);
            ThermalMaterialEdge edge = solver.materialEdge(old.key(index));
            ownerOperation[write] = edge.ownerOperation();
            write++;
        }
        for (int cursor = additionStart; cursor < additionEnd; cursor++) {
            int edgeIndex = additionOrder[cursor];
            ThermalMaterialEdge edge = replacements[edgeIndex];
            keys[write] = affectedKeys[edgeIndex];
            first[write] = edge.first();
            second[write] = edge.second();
            conductance[write] = edge.conductance();
            coefficient[write] = edge.coefficient();
            ownerOperation[write] = edge.ownerOperation();
            write++;
        }
        sortExecution(
                ownerOperation,
                keys,
                first,
                second,
                conductance,
                coefficient,
                0,
                count - 1);
        return new ThermalMaterialExecution(
                keys, first, second, conductance, coefficient);
    }

    private void ensureNewCapacity(int required) {
        if (required <= newKey.length) {
            return;
        }
        int capacity = grow(newKey.length, required);
        newKey = Arrays.copyOf(newKey, capacity);
        newFragment = Arrays.copyOf(newFragment, capacity);
        newOperation = Arrays.copyOf(newOperation, capacity);
        newRank = Arrays.copyOf(newRank, capacity);
        newConductance = Arrays.copyOf(newConductance, capacity);
    }

    private void sortNewContributions(int low, int high) {
        int left = low;
        int right = high;
        if (left >= right) {
            return;
        }
        int pivot = low + (high - low >>> 1);
        long pivotKey = newKey[pivot];
        long pivotRank = newRank[pivot];
        int pivotOperation = newOperation[pivot];
        while (left <= right) {
            while (compareNew(
                    left, pivotKey, pivotRank, pivotOperation) < 0) {
                left++;
            }
            while (compareNew(
                    right, pivotKey, pivotRank, pivotOperation) > 0) {
                right--;
            }
            if (left <= right) {
                swapNew(left++, right--);
            }
        }
        if (low < right) {
            sortNewContributions(low, right);
        }
        if (left < high) {
            sortNewContributions(left, high);
        }
    }

    private int compareNew(
            int index,
            long key,
            long rank,
            int operation
    ) {
        int compared = Long.compare(newKey[index], key);
        return compared != 0
                ? compared
                : compare(newRank[index], newOperation[index], rank, operation);
    }

    private void swapNew(int first, int second) {
        if (first == second) {
            return;
        }
        long longValue = newKey[first];
        newKey[first] = newKey[second];
        newKey[second] = longValue;
        int intValue = newFragment[first];
        newFragment[first] = newFragment[second];
        newFragment[second] = intValue;
        intValue = newOperation[first];
        newOperation[first] = newOperation[second];
        newOperation[second] = intValue;
        longValue = newRank[first];
        newRank[first] = newRank[second];
        newRank[second] = longValue;
        double doubleValue = newConductance[first];
        newConductance[first] = newConductance[second];
        newConductance[second] = doubleValue;
    }

    private static void sortExecution(
            int[] operation,
            long[] key,
            int[] first,
            int[] second,
            double[] conductance,
            double[] coefficient,
            int low,
            int high
    ) {
        int left = low;
        int right = high;
        if (left >= right) {
            return;
        }
        int pivot = low + (high - low >>> 1);
        int pivotOperation = operation[pivot];
        long pivotKey = key[pivot];
        while (left <= right) {
            while (compare(
                    operation[left], key[left],
                    pivotOperation, pivotKey) < 0) {
                left++;
            }
            while (compare(
                    operation[right], key[right],
                    pivotOperation, pivotKey) > 0) {
                right--;
            }
            if (left <= right) {
                swap(operation, left, right);
                swap(key, left, right);
                swap(first, left, right);
                swap(second, left, right);
                swap(conductance, left, right);
                swap(coefficient, left, right);
                left++;
                right--;
            }
        }
        if (low < right) {
            sortExecution(
                    operation, key, first, second,
                    conductance, coefficient, low, right);
        }
        if (left < high) {
            sortExecution(
                    operation, key, first, second,
                    conductance, coefficient, left, high);
        }
    }

    private static int compare(
            long firstRank,
            int firstOperation,
            long secondRank,
            int secondOperation
    ) {
        int compared = Long.compare(firstRank, secondRank);
        return compared != 0
                ? compared
                : Integer.compare(firstOperation, secondOperation);
    }

    private static int compare(
            int firstOperation,
            long firstKey,
            int secondOperation,
            long secondKey
    ) {
        int compared = Integer.compare(firstOperation, secondOperation);
        return compared != 0
                ? compared
                : Long.compare(firstKey, secondKey);
    }

    private static void swap(int[] values, int first, int second) {
        int value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private static void swap(long[] values, int first, int second) {
        long value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private static void swap(double[] values, int first, int second) {
        double value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private static int grow(int current, int required) {
        int capacity = Math.max(1, current);
        while (capacity < required) {
            capacity = Math.addExact(
                    capacity, Math.max(8, capacity >>> 1));
        }
        return capacity;
    }

    record Result(
            long[] keys,
            ThermalMaterialEdge[] edges,
            int[] executionFragments,
            ThermalMaterialExecution[] executions,
            int expectedFinalSize,
            int possibleInsertions
    ) {
        private static final Result EMPTY = new Result(
                PreparedTopologyChange.NO_LONGS,
                PreparedTopologyChange.NO_MATERIAL_EDGES,
                PreparedTopologyChange.NO_INTS,
                PreparedTopologyChange.NO_MATERIAL_EXECUTIONS,
                0,
                0);

        private static Result unchanged(int edgeCount) {
            return edgeCount == 0 ? EMPTY : new Result(
                    PreparedTopologyChange.NO_LONGS,
                    PreparedTopologyChange.NO_MATERIAL_EDGES,
                    PreparedTopologyChange.NO_INTS,
                    PreparedTopologyChange.NO_MATERIAL_EXECUTIONS,
                    edgeCount,
                    0);
        }
    }
}
