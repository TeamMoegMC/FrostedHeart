package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.resident.Resident;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TownWithResident{
    /**
     * get all residents in the town.
     * @return collection of all residents
     */
    Collection<Resident> getAllResidents();

    /**
     * get a resident by uuid
     * @param id the uuid of the resident
     * @return the resident that catches the uuid. might be null if the resident doesn't exist in the town.
     */
    Optional<Resident> getResident(UUID id);

}
