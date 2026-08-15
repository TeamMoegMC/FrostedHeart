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

package com.teammoeg.frostedheart.content.town.citizen;

import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;

/**
 * 居民确定性生成器：由稳定 id 播种的 xorshift 派生姓名与闲聊台词序号。
 * 同一 id 在任何端、任何时刻生成结果一致（双端无需同步姓名），
 * 与架构文档"随机性用 id 播种保证确定性"的约定一致。
 * <p>
 * Deterministic citizen generator: an id-seeded xorshift derives names and
 * chat line indices. The same id yields the same result on any side at any
 * time (no name sync needed between server and client), matching the
 * architecture doc's determinism-by-id-seeding convention.
 */
public final class CitizenNames {

	/** 闲聊台词条数，语言键为 message.frostedheart.citizen.chat.0..N-1 / number of chat lines, lang keys chat.0..N-1 */
	public static final int CHAT_LINES = 8;

	private CitizenNames() {
	}

	/**
	 * xorshift64* 混合：把任意 int id 打散为伪随机长整型。
	 * <p>
	 * xorshift64* mixing: hashes any int id into a pseudo-random long.
	 *
	 * @param seed 种子（通常含稳定 id） / seed (usually contains the stable id)
	 * @return 混合结果 / mixed result
	 */
	public static long mix(long seed) {
		long x = seed + 0x9E3779B97F4A7C15L;
		x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
		x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
		return x ^ (x >>> 31);
	}

	/**
	 * 居民的名字（复用流浪难民的姓名池）。
	 * <p>
	 * The citizen's first name (reuses the wandering refugee name pools).
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 名字 / first name
	 */
	public static String firstName(int citizenId) {
		String[] pool = WanderingRefugee.FIRST_NAMES;
		return pool[(int) (mix(citizenId) % pool.length + pool.length) % pool.length];
	}

	/**
	 * 居民的姓氏。
	 * <p>
	 * The citizen's last name.
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 姓氏 / last name
	 */
	public static String lastName(int citizenId) {
		String[] pool = WanderingRefugee.LAST_NAMES;
		return pool[(int) (mix(citizenId ^ 0x5DEECE66DL) % pool.length + pool.length) % pool.length];
	}

	/**
	 * 居民全名（"名字 姓氏"）。
	 * <p>
	 * The citizen's full name ("first last").
	 *
	 * @param citizenId 稳定 id / stable id
	 * @return 全名 / full name
	 */
	public static String fullName(int citizenId) {
		return firstName(citizenId) + " " + lastName(citizenId);
	}

	/**
	 * 闲聊台词序号：同一居民同一天恒定，逐日轮换。
	 * <p>
	 * Chat line index: constant for a citizen on the same day, rotates daily.
	 *
	 * @param citizenId 稳定 id / stable id
	 * @param worldDay 世界日 / world day
	 * @return 0..{@link #CHAT_LINES}-1 / line index
	 */
	public static int chatLine(int citizenId, long worldDay) {
		return (int) ((mix(citizenId * 31L + worldDay) % CHAT_LINES + CHAT_LINES) % CHAT_LINES);
	}
}
