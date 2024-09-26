/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.mixin;

import java.util.Set;
import java.util.concurrent.Executor;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;

public class LazyDataFixerBuilder extends DataFixerBuilder {
  /*  @Override
	public DataFixer buildUnoptimized() {
		return super.buildUnoptimized();
	}

	@Override
	public DataFixer buildOptimized(Set<TypeReference> requiredTypes, Executor executor) {
		return super.buildOptimized(requiredTypes, NO_OP_EXECUTOR);
	}

	private static final Executor NO_OP_EXECUTOR = command -> {
    };
*/
    public LazyDataFixerBuilder(int dataVersion) {
        super(dataVersion);
    }

}
