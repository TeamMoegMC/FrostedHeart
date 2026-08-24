/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

/**
 * GameTest-only owner handle mixed into a {@code LevelChunkSection}.
 */
public interface Phase0aSectionAttachment {
    Phase0aMutationProbe.LoadedSectionOwner frostedheart$getPhase0aOwner();

    void frostedheart$setPhase0aOwner(Phase0aMutationProbe.LoadedSectionOwner owner);

    long frostedheart$incrementPhase0aUnmappedWrites();

    long frostedheart$getPhase0aUnmappedWrites();
}
