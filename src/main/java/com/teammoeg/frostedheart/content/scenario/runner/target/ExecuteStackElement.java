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

package com.teammoeg.frostedheart.content.scenario.runner.target;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

public record ExecuteStackElement(String name,int nodeNum) implements ScenarioTarget{
	public static final Codec<ExecuteStackElement> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.STRING.fieldOf("storage").forGetter(o->o.name()),
		Codec.INT.fieldOf("node").forGetter(o->o.nodeNum())
		).apply(t,ExecuteStackElement::new));
	public static final Codec<List<ExecuteStackElement>> LIST_CODEC=Codec.list(CODEC);
	@Override
	public PreparedScenarioTarget prepare(ScenarioContext t, Scenario current) {

		return new PreparedScenarioTarget(t.loadScenario(name),nodeNum);
	}

}