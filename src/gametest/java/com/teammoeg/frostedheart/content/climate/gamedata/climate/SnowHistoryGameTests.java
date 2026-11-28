/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.UUID;

/**
 * Checks the snow history that WorldClimate searches around the current hour and the packet that carries it.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class SnowHistoryGameTests {
    private static final long TEST_DAY = 100L;
    private static final ChunkPos TEST_CHUNK = new ChunkPos(0, 0);

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void snowHistorySearchesAroundTheCurrentHour(GameTestHelper helper) {
        WorldClimate quiet = buildClimate(TEST_DAY, 2);
        assertHour(helper, "last snow end with no snow anywhere", quiet.getLastSnowEndHour(TEST_CHUNK), -1L);
        assertHour(helper, "next snow start with no snow anywhere", quiet.getNextSnowStartHour(TEST_CHUNK), -1L);

        WorldClimate behind = buildClimate(TEST_DAY, 2);
        setHourType(behind, TEST_DAY - 1, 20, ClimateType.SNOW);
        setHourType(behind, TEST_DAY - 1, 21, ClimateType.SNOW);
        setHourType(behind, TEST_DAY - 1, 22, ClimateType.SNOW);
        assertHour(helper, "last snow end with snow yesterday from hour 20 to 22",
                behind.getLastSnowEndHour(TEST_CHUNK), (TEST_DAY - 1) * 24 + 23);
        assertHour(helper, "next snow start with snow yesterday from hour 20 to 22",
                behind.getNextSnowStartHour(TEST_CHUNK), -1L);

        WorldClimate current = buildClimate(TEST_DAY, 5);
        setHourType(current, TEST_DAY, 4, ClimateType.SNOW);
        setHourType(current, TEST_DAY, 5, ClimateType.SNOW);
        assertHour(helper, "last snow end with snow in the previous and the current hour",
                current.getLastSnowEndHour(TEST_CHUNK), TEST_DAY * 24 + 5);

        WorldClimate ahead = buildClimate(TEST_DAY, 22);
        setHourType(ahead, TEST_DAY + 1, 1, ClimateType.SNOW);
        assertHour(helper, "next snow start with snow tomorrow at hour 1",
                ahead.getNextSnowStartHour(TEST_CHUNK), (TEST_DAY + 1) * 24 + 1);
        assertHour(helper, "last snow end with snow tomorrow at hour 1",
                ahead.getLastSnowEndHour(TEST_CHUNK), -1L);

        WorldClimate farAhead = buildClimate(TEST_DAY, 0);
        setHourType(farAhead, TEST_DAY + 1, 0, ClimateType.SNOW);
        assertHour(helper, "next snow start exactly " + WorldClimate.SNOW_SEARCH_HOURS + " hours ahead",
                farAhead.getNextSnowStartHour(TEST_CHUNK), (TEST_DAY + 1) * 24);

        WorldClimate tooFarAhead = buildClimate(TEST_DAY, 0);
        setHourType(tooFarAhead, TEST_DAY + 1, 1, ClimateType.SNOW);
        assertHour(helper, "next snow start one hour beyond " + WorldClimate.SNOW_SEARCH_HOURS + " hours ahead",
                tooFarAhead.getNextSnowStartHour(TEST_CHUNK), -1L);

        WorldClimate farBehind = buildClimate(TEST_DAY, 23);
        setHourType(farBehind, TEST_DAY - 1, 23, ClimateType.SNOW);
        assertHour(helper, "last snow end exactly " + WorldClimate.SNOW_SEARCH_HOURS + " hours behind",
                farBehind.getLastSnowEndHour(TEST_CHUNK), TEST_DAY * 24);

        WorldClimate tooFarBehind = buildClimate(TEST_DAY, 23);
        setHourType(tooFarBehind, TEST_DAY - 1, 22, ClimateType.SNOW);
        assertHour(helper, "last snow end one hour beyond " + WorldClimate.SNOW_SEARCH_HOURS + " hours behind",
                tooFarBehind.getLastSnowEndHour(TEST_CHUNK), -1L);

        WorldClimate storm = buildClimate(TEST_DAY, 10);
        setHourType(storm, TEST_DAY, 9, ClimateType.BLIZZARD);
        setHourType(storm, TEST_DAY, 11, ClimateType.SNOW_BLIZZARD);
        assertHour(helper, "last snow end with a blizzard in the previous hour",
                storm.getLastSnowEndHour(TEST_CHUNK), TEST_DAY * 24 + 10);
        assertHour(helper, "next snow start with a snow blizzard in the next hour",
                storm.getNextSnowStartHour(TEST_CHUNK), TEST_DAY * 24 + 11);

        WorldClimate calm = buildClimate(TEST_DAY, 10);
        setHourType(calm, TEST_DAY, 9, ClimateType.CLOUDY);
        setHourType(calm, TEST_DAY, 11, ClimateType.SUN);
        assertHour(helper, "last snow end with a cloudy previous hour", calm.getLastSnowEndHour(TEST_CHUNK), -1L);
        assertHour(helper, "next snow start with a sunny next hour", calm.getNextSnowStartHour(TEST_CHUNK), -1L);

        WorldClimate snowNow = buildClimate(TEST_DAY, 7);
        setHourType(snowNow, TEST_DAY, 7, ClimateType.SNOW);
        assertHour(helper, "last snow end with snow only in the current hour",
                snowNow.getLastSnowEndHour(TEST_CHUNK), -1L);
        assertHour(helper, "next snow start with snow only in the current hour",
                snowNow.getNextSnowStartHour(TEST_CHUNK), -1L);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void climatePacketCarriesSnowHistory(GameTestHelper helper) throws Exception {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-mock-player"));
        WorldClimate climate = buildClimate(TEST_DAY, 12);
        setHourType(climate, TEST_DAY, 10, ClimateType.SNOW);
        setHourType(climate, TEST_DAY, 14, ClimateType.SNOW);
        FHClimatePacket packet = new FHClimatePacket(climate, player);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        byte[] encoded = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), encoded);
        SerializeUtil.readShortArray(buffer);
        buffer.readVarLong();
        buffer.readVarLong();
        buffer.readByte();
        buffer.readByte();
        buffer.readVarInt();
        buffer.readFloat();
        buffer.readShort();
        long lastSnowEndHour = buffer.readLong();
        long nextSnowStartHour = buffer.readLong();
        helper.assertTrue(buffer.readableBytes() == 0,
                "The packet left " + buffer.readableBytes() + " bytes unread after the snow history");
        assertHour(helper, "last snow end in the packet", lastSnowEndHour, TEST_DAY * 24 + 11);
        assertHour(helper, "next snow start in the packet", nextSnowStartHour, TEST_DAY * 24 + 14);
        Constructor<FHClimatePacket> constructor = FHClimatePacket.class.getDeclaredConstructor(FriendlyByteBuf.class);
        constructor.setAccessible(true);
        FHClimatePacket decoded = constructor.newInstance(new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded)));
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        decoded.encode(reencoded);
        byte[] second = new byte[reencoded.readableBytes()];
        reencoded.getBytes(reencoded.readerIndex(), second);
        helper.assertTrue(Arrays.equals(encoded, second),
                "Re-encoding the decoded packet gives " + second.length + " bytes instead of the " + encoded.length
                        + " bytes encoded before");
        helper.succeed();
    }

    /**
     * A climate whose days are all sunny, with the clock inside the given hour of the given day.
     */
    private static WorldClimate buildClimate(long day, int hourInDay) {
        WorldClimate climate = new WorldClimate(0);
        climate.dailyTempData.clear();
        for (int index = 0; index <= WorldClimate.DAY_CACHE_LENGTH; index++) {
            DayClimateData data = new DayClimateData(day - 1 + index);
            for (int hour = 0; hour < 24; hour++) {
                data.setType(hour, ClimateType.SUN);
            }
            climate.dailyTempData.add(data);
        }
        climate.clockSource.secs = (day * 24 + hourInDay) * 50 + 25;
        return climate;
    }

    private static void setHourType(WorldClimate climate, long day, int hourInDay, ClimateType type) {
        int index = (int) (day - climate.clockSource.getDate() + 1);
        climate.dailyTempData.get(index).setType(hourInDay, type);
    }

    private static void assertHour(GameTestHelper helper, String what, long actual, long expected) {
        helper.assertTrue(actual == expected, what + " is " + actual + " instead of " + expected);
    }
}
