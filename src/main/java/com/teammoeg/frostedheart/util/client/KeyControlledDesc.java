/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.client;

import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.ChatFormatting.*;
import static net.minecraft.ChatFormatting.DARK_GRAY;

public class KeyControlledDesc {
	List<Component> lines;
	Supplier<List<Component>> linesOnKey;
	int key1;
	MutableComponent keyTooltip;
	String[] holdDesc;

	// initialize with empty lines
	public KeyControlledDesc(Supplier<List<Component>> LinesOnKey, int key1, String key1Desc, String key1Translation) {
		this.lines = new ArrayList<>();
		this.key1 = key1;

		holdDesc = Lang.translateTooltip(key1Translation, "$").getString().split("\\$");
		keyTooltip = Lang.text(key1Desc).component();
		this.linesOnKey=LinesOnKey;
	}

	public void appendKeyDesc(List<Component> lines, boolean isPressed) {

		MutableComponent tabBuilder = Components.empty();
		tabBuilder.append(Components.literal(holdDesc[0]).withStyle(DARK_GRAY));
		tabBuilder.append(keyTooltip.plainCopy().withStyle(isPressed ? GRAY:WHITE));
		tabBuilder.append(Components.literal(holdDesc[1]).withStyle(DARK_GRAY));
		lines.add(0, tabBuilder);
		if(isPressed)
			lines.add(1, Components.immutableEmpty());
	}

	public List<Component> getCurrentLines() {
		if (com.teammoeg.chorda.util.CUtils.isDown(key1)) {
			List<Component> res=linesOnKey.get();
			if(res!=null) {
				appendKeyDesc(res,true);
				return res;
			}else {
				return List.of();
			}
		}
		appendKeyDesc(lines, false);
		return lines;
	}

}
