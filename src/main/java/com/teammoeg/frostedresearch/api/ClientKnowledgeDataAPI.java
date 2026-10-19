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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.frostedresearch.knowledge.KnowledgeProjection;
import com.teammoeg.frostedresearch.knowledge.TechnologyAccessProjection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client projections installed atomically from the knowledge snapshot; the team state itself stays on the server. */
@OnlyIn(Dist.CLIENT)
public final class ClientKnowledgeDataAPI {
    private static volatile long catalogRevision;
    private static volatile KnowledgeProjection knowledgeProjection = KnowledgeProjection.EMPTY;
    private static volatile TechnologyAccessProjection technologyProjection = TechnologyAccessProjection.EMPTY;

    private ClientKnowledgeDataAPI() {
    }

    public static void install(long revision, KnowledgeProjection knowledge,
            TechnologyAccessProjection technology) {
        catalogRevision = revision;
        knowledgeProjection = knowledge;
        technologyProjection = technology;
    }

    public static long catalogRevision() {
        return catalogRevision;
    }

    public static KnowledgeProjection knowledgeProjection() {
        return knowledgeProjection;
    }

    public static TechnologyAccessProjection technologyProjection() {
        return technologyProjection;
    }
}
