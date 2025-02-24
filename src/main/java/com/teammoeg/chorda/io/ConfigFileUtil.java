package com.teammoeg.chorda.io;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.config.ConfigFileType;
import com.teammoeg.frostedheart.FHMain;

public class ConfigFileUtil {
	private static Gson gs = new GsonBuilder().setPrettyPrinting().create();

	public ConfigFileUtil() {
	}

	@Nullable
	public static <T> T load(ConfigFileType<T> c, String name) {
		File f = new File(c.folder(), name + ".json");
		if (f.exists())
			try {
				JsonElement je = JsonParser.parseString(FileUtil.readString(f));
				return c.codec().parse(JsonOps.INSTANCE, je).resultOrPartial(FHMain.LOGGER::error).orElse(null);
			} catch (IOException e) {
				FHMain.LOGGER.error("Cannot load data " + c + ":" + f.getName() + ": " + e.getMessage());
			}
		return null;
	}
	@Nullable
	public static <T> void delete(ConfigFileType<T> c, String name) {
		File f = new File(c.folder(), name + ".json");
		if (f.exists())
			if(!f.delete())
				f.deleteOnExit();
	}
	public static <T> Map<String, T> loadAll(ConfigFileType<T> c) {
		FHMain.LOGGER.info("loading " + c + " data from files...");
		Map<String, T> list = new LinkedHashMap<>();
		MutableObject<String> sk = new MutableObject<>();
		Consumer<T> addToList = t -> list.put(sk.getValue(), t);
		if (c.folder().exists())
			for (File f : c.folder().listFiles((dir, name) -> name.endsWith(".json"))) {
				try {
					JsonElement je = JsonParser.parseString(FileUtil.readString(f));
					String id = f.getName();
					id = id.substring(0, id.length() - 5);
					sk.setValue(id);
					c.codec().parse(JsonOps.INSTANCE, je).resultOrPartial(FHMain.LOGGER::error).ifPresent(addToList);
				} catch (Exception e) {
					e.printStackTrace();
					FHMain.LOGGER.warn("Cannot load data " + f.getName() + ": " + e.getMessage());
				}

			}
		return list;
	}

	public static <T> void save(ConfigFileType<T> c, String name, T data) {
		c.folder().mkdirs();

		File out = new File(c.folder(), name + ".json");
		try {
			FileUtil.transfer(gs.toJson(CodecUtil.encodeOrThrow(c.codec().encodeStart(JsonOps.INSTANCE, data))),
				out);
		} catch (IOException e) {

			throw new RuntimeException("Cannot save data " + c + ":" + name + ": " + e.getMessage());
		}
	}

	public static <T> void saveAll(ConfigFileType<T> c, Map<String, T> data) {
		c.folder().mkdirs();

		for (Entry<String, T> r : data.entrySet()) {
			File out = new File(c.folder(), r.getKey() + ".json");
			try {
				FileUtil.transfer(
					gs.toJson(CodecUtil.encodeOrThrow(c.codec().encodeStart(JsonOps.INSTANCE, r.getValue()))), out);
			} catch (IOException e) {
				throw new RuntimeException("Cannot save data " + c + ":" + r.getKey() + ": " + e.getMessage());
			}

		}
	}
}
