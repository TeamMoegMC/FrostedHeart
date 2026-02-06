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

package com.teammoeg.frostedheart.content.scenario.runner.target;

import java.util.List;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

public record ExecuteStackElement(String name,int nodeNum) implements ScenarioTarget{
	public static final Codec<ExecuteStackElement> RAW_CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.STRING.fieldOf("storage").forGetter(o->o.name()),
		Codec.INT.fieldOf("node").forGetter(o->o.nodeNum())
		).apply(t,ExecuteStackElement::new));
	public static final Codec<Either<ExecuteStackElement, ExecuteTarget>> CODEC=Codec.either(RAW_CODEC, ExecuteTarget.CODEC);
	public static final Codec<List<ScenarioTarget>> LIST_CODEC=Codec.list(CODEC.flatXmap(ExecuteStackElement::map, ExecuteStackElement::map));
	@Override
	public PreparedScenarioTarget prepare(ScenarioContext t, Scenario current) {

		return new PreparedScenarioTarget(t.loadScenario(name),nodeNum);
	}
	
	private static DataResult<ScenarioTarget> map(Either<ExecuteStackElement, ExecuteTarget> either) {
		return either.left().map(t->(ScenarioTarget)t).or(()->either.right()).map(DataResult::success).orElseGet(()->DataResult.error(()->"invalid data"));
		
	}
	
	private static DataResult<Either<ExecuteStackElement, ExecuteTarget>> map(ScenarioTarget either) {
		if(either instanceof ExecuteStackElement ex)
			return DataResult.success(Either.left(ex));
		if(either instanceof ExecuteTarget ex)
			return DataResult.success(Either.right(ex));
		return DataResult.error(()->"invalid stacktrace element");
		
	}
}