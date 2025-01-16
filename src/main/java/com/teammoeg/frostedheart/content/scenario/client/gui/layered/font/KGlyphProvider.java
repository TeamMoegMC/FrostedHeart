package com.teammoeg.frostedheart.content.scenario.client.gui.layered.font;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.UnihexParser.OverrideRange;
import com.teammoeg.chorda.util.client.ClientUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

public class KGlyphProvider extends SimplePreparableReloadListener<Object> {
	public static KGlyphProvider INSTANCE = new KGlyphProvider();
	private Int2ObjectMap<GlyphData> data = new Int2ObjectOpenHashMap<>();
	private Int2ObjectMap<GlyphData> unicodeData = new Int2ObjectOpenHashMap<>();

	ResourceManager rm;

	private KGlyphProvider() {
	}

	public GlyphData getGlyph(int code) {
		if (ClientUtils.mc().options.forceUnicodeFont().get()) {
			return unicodeData.get(code);
		}
		return data.get(code);
	}

	public void readFont(JsonObject jo) {
		FHMain.LOGGER.info("loading fonts...");
		JsonArray ja = jo.get("providers").getAsJsonArray();
		for (int i = 0; i < ja.size(); i++) {

			JsonObject cr = ja.get(i).getAsJsonObject();
			FHMain.LOGGER.info("loading provider " + cr.get("type").getAsString());
			switch (cr.get("type").getAsString()) {
			case "bitmap":
				readBitmap(cr);
				break;
			case "legacy_unicode":
				readUnicode(cr);
				break;
			case "reference":
				loadFont(new ResourceLocation(cr.get("id").getAsString() + ".json"));
				break;
			case "unihex":
				readUnihex(cr);
				break;
			case "space":
				readSpace(cr);
				break;
			}
		}
		// System.out.println("loaded "+data.size());
	}
	public void readSpace(JsonObject unicode) {
		for(Entry<String, JsonElement> ent:unicode.get("advances").getAsJsonObject().entrySet()) {
			GlyphData gd=new GlyphData();
			gd.height=16;
			gd.advance=ent.getValue().getAsInt()*2;
			data.putIfAbsent(ent.getKey().codePointAt(0), gd);
		}
	}
	public void readBitmap(JsonObject unicode) {
		int height = 9;
		if (unicode.has("height"))
			height = unicode.get("height").getAsInt();
		int ascent = unicode.get("ascent").getAsInt();
		ResourceLocation file = new ResourceLocation(unicode.get("file").getAsString());

		Optional<Resource> r = rm.getResource(new ResourceLocation(file.getNamespace(), "textures/" + file.getPath()));
		if (r.isPresent())
			try {
				InputStream stream = r.get().open();
				BufferedImage image = ImageIO.read(stream);

				JsonArray ja = unicode.get("chars").getAsJsonArray();
				if (image != null) {
					int i = image.getWidth();
					int j = image.getHeight();
					int k = i / ja.get(0).getAsString().length();
					int l = j / ja.size();
					float f = height * 1f / l;
					for (int i1 = 0; i1 < ja.size(); i1++) {
						String codepoints = ja.get(i1).getAsString();
						for (int k1 = 0; k1 < codepoints.codePointCount(0, codepoints.length()); k1++) {
							int n = codepoints.codePointAt(k1);
							if (n == 0) continue;
							int i2 = getCharacterWidth(image, k, l, k1, i1);
							GlyphData gd = new GlyphData(k1 * k, i1 * l, k, l, (int) (0.5D + i2 * f) + 1, ascent, f);
							gd.image = image;
							data.putIfAbsent(n, gd);
						}
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

	private int getCharacterWidth(BufferedImage nativeImageIn, int charWidthIn, int charHeightInsp, int columnIn, int rowIn) {
		int i;

		for (i = charWidthIn - 1; i >= 0; --i) {

			for (int k = rowIn * charHeightInsp; k < rowIn * charHeightInsp + charHeightInsp; ++k) {
				if ((nativeImageIn.getRGB(i + columnIn * charWidthIn, k) & 0xFF000000) != 0) {
					return i + 1;
				}
			}
		}

		return i + 1;
	}

	public void readUnicode(JsonObject unicode) {
		String sizes = unicode.get("sizes").getAsString();
		String template = unicode.get("template").getAsString();

		byte[] sizesb = new byte[65536];
		try {
			Optional<Resource> r = rm.getResource(new ResourceLocation(sizes));
			if (r.isPresent())
				try (InputStream stream = r.get().open()) {
					stream.read(sizesb);
					for (int i = 0; i <= 0xFF; i++) {
						String hex = String.format("%02x", i);
						ResourceLocation rrl = new ResourceLocation(String.format(template, hex));
						ResourceLocation imgloc = new ResourceLocation(rrl.getNamespace(), "textures/" + rrl.getPath());
						Optional<Resource> resource = rm.getResource(imgloc);
						if (resource.isPresent())
							try (InputStream streamImg = resource.get().open()) {
								BufferedImage image = ImageIO.read(streamImg);
								if (image != null) {
									for (int j = 0; j <= 0Xff; j++) {
										GlyphData gd = new GlyphData((j & 0xF) * 16, (j & 0xF0));
										int n = (i * 0x100) + j;
										gd.image = image;
										gd.parseSize(sizesb[n]);
										unicodeData.put(n, gd);
										gd.isUnicode = true;
										data.putIfAbsent(n, gd);
									}
								} else {
									FHMain.LOGGER.info("Error loading " + rrl);
								}
							}
					}
				}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void readUnihex(JsonObject unicode) {
		String file = unicode.get("hex_file").getAsString();
		List<OverrideRange> ranges = new ArrayList<>();
		if (unicode.has("size_overrides")) {
			JsonArray over = unicode.get("size_overrides").getAsJsonArray();
			for (JsonElement avo : over) {
				ranges.add(new OverrideRange(avo.getAsJsonObject()));
			}
		}
		try {
				try (InputStream stream = rm.open(new ResourceLocation(file))) {
					try (ZipInputStream zipinputstream = new ZipInputStream(stream)) {
						ZipEntry zipentry;
						while ((zipentry = zipinputstream.getNextEntry()) != null) {
							String s = zipentry.getName();
							FHMain.LOGGER.info("Got " + s+" from zipped file");
							if (s.endsWith(".hex")) {
								UnihexParser.readFromStream(new FastBufferedInputStream(zipinputstream), (k, v) -> {
									unicodeData.put(k, v);
									data.putIfAbsent(k, v);
								}, ranges);
							}
						}
					}
				}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onResourceManagerReload(ResourceManager resourceManager) {
		rm = resourceManager;
		loadFont(new ResourceLocation("default.json"));
	}

	public void loadFont(ResourceLocation location) {
		try {
			List<Resource> cls = rm.getResourceStack(new ResourceLocation(location.getNamespace(), "font/" + location.getPath()));
			for (Resource rl : cls) {
				FHMain.LOGGER.info("Reloading Font from " + rl.sourcePackId());
				try (BufferedReader stream = rl.openAsReader()) {
					readFont(JsonParser.parseReader(stream).getAsJsonObject());
				}
			}
		} catch (JsonSyntaxException | IOException e) {
			FHMain.LOGGER.error("Error loading font", e);
		}
	}

	@Override
	protected Object prepare(ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
		onResourceManagerReload(resourceManagerIn);
		/*
		 * for(int i='A';i<'z';i++) {
		 * System.out.println(Character.toString((char)i)+unicodeData.get(i)); }
		 */
		return new Object();
	}

	@Override
	protected void apply(Object objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {

	}

	public static void addListener() {
		if (Minecraft.getInstance() != null && Minecraft.getInstance().getResourceManager() != null)
			((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(INSTANCE);
	}
}
