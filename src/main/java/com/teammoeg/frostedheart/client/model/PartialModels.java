package com.teammoeg.frostedheart.client.model;

import java.util.HashMap;
import java.util.Map;

import com.jozufozu.flywheel.core.PartialModel;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

public class PartialModels {
	public static final Map<Boolean, Map<Direction, PartialModel>> PIPE_ATTACHMENTS = new HashMap<>();
	public static final PartialModel FLUID_PIPE_CASING = get("fluid_pipe/casing");
	static {
		populateMaps();
	}

	static void populateMaps() {
		Map<Direction, PartialModel> map = new HashMap<>();
		for (Direction d : Direction.values()) {
			map.put(d, get("fluid_pipe/rim/" + d.getName2().toLowerCase()));
		}
		PIPE_ATTACHMENTS.put(true, map);
		Map<Direction, PartialModel> map2 = new HashMap<>();
		for (Direction d : Direction.values()) {
			map2.put(d, get("fluid_pipe/" + d.getName2().toLowerCase()));
		}
		PIPE_ATTACHMENTS.put(false, map2);
	}
	private static PartialModel get(String path) {
		return new PartialModel(new ResourceLocation(FHMain.MODID, "block/" + path));
	}
}
