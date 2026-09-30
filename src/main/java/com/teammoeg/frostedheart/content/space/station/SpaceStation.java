package com.teammoeg.frostedheart.content.space.station;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.core.Vec3i;

public class SpaceStation {
	public Reference2LongOpenHashMap<SpaceResource> resources=new Reference2LongOpenHashMap<SpaceResource>(SpaceResource.values().length);
	public List<ConstructionSite> constructions=new ArrayList<>();
	public Map<Vec3i,Section> sections;
	public int radius;
	public int height;
	public int depth;
}
