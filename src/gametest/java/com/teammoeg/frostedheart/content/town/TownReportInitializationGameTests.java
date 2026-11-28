/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town;

import com.mojang.serialization.Codec;
import java.io.IOException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Fresh class definitions prevent earlier tests or mod startup from masking initialization cycles. */
@GameTestHolder("frostedheart_report_initialization")
@PrefixGameTestTemplate(false)
public final class TownReportInitializationGameTests {
    private static final String ROOT = "com.teammoeg.frostedheart.content.town.buildings.";

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void mineReportFirst(GameTestHelper helper) throws Exception {
        check(helper, ROOT + "mine.MineBaseBuilding", "MiningDailyReport", true);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void mineBuildingFirst(GameTestHelper helper) throws Exception {
        check(helper, ROOT + "mine.MineBaseBuilding", "MiningDailyReport", false);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void transportReportFirst(GameTestHelper helper) throws Exception {
        check(helper, ROOT + "logistics.TransportStationBuilding", "TransportStationDailyReport", true);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void transportBuildingFirst(GameTestHelper helper) throws Exception {
        check(helper, ROOT + "logistics.TransportStationBuilding", "TransportStationDailyReport", false);
    }

    private static void check(GameTestHelper helper, String building, String report, boolean reportFirst)
            throws Exception {
        ClassLoader loader = new FreshBuildingLoader(building);
        String reportName = building + "$" + report;
        Class.forName(reportFirst ? reportName : building, true, loader);
        Class<?> reportClass = Class.forName(reportName, true, loader);
        Codec<Object> reportCodec = codec(reportClass);
        Object empty = reportClass.getField("EMPTY").get(null);
        helper.assertTrue(empty.equals(parse(reportCodec, new CompoundTag())), "missing report fields use EMPTY defaults");
        CompoundTag data = new CompoundTag();
        data.putBoolean("hasData", true);
        if (report.equals("MiningDailyReport")) {
            data.putDouble("requested", 8.0);
            data.putDouble("extracted", 3.0);
        } else {
            data.putInt("workerCount", 2);
            data.putDouble("totalProductivity", 1.5);
            data.putDouble("plannedCapacity", 8.0);
            data.putDouble("addedCapacity", 3.0);
        }
        Object expected = parse(reportCodec, data);
        helper.assertTrue(expected.equals(parse(reportCodec, encode(reportCodec, expected))), "nonempty report round trip");
        Codec<Object> buildingCodec = codec(Class.forName(building, true, loader));
        for (boolean present : new boolean[]{false, true}) {
            CompoundTag body = new CompoundTag();
            if (present) body.put("dailyReport", data.copy());
            Object decoded = parse(buildingCodec, body);
            Object actual = decoded.getClass().getMethod("getDailyReport").invoke(decoded);
            helper.assertTrue((present ? expected : empty).equals(actual), "building preserves report or uses EMPTY");
            Object reopened = parse(buildingCodec, encode(buildingCodec, decoded));
            helper.assertTrue(actual.equals(reopened.getClass().getMethod("getDailyReport").invoke(reopened)),
                    "building save/reload preserves report");
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static Codec<Object> codec(Class<?> type) throws ReflectiveOperationException {
        return (Codec<Object>) type.getField("CODEC").get(null);
    }

    private static Object parse(Codec<Object> codec, Tag tag) {
        return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, message -> {});
    }

    private static Tag encode(Codec<Object> codec, Object value) {
        return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, message -> {});
    }

    /** Reload only the building and its nested records; use the real Forge dependencies for everything else. */
    private static final class FreshBuildingLoader extends ClassLoader {
        private final String building;

        FreshBuildingLoader(String building) {
            super(TownReportInitializationGameTests.class.getClassLoader());
            this.building = building;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(building) && !name.startsWith(building + "$")) return super.loadClass(name, resolve);
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try (var stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (stream == null) throw new ClassNotFoundException(name);
                    byte[] bytes = stream.readAllBytes();
                    loaded = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }
}
