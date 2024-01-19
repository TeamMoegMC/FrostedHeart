package com.teammoeg.frostedheart.scenario.client.gui.layered.text;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.frostedheart.util.FileUtil;

import blusunrize.immersiveengineering.client.ClientUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

public class KGlyphProvider extends ReloadListener<Object>{
	public static KGlyphProvider INSTANCE=new KGlyphProvider();
	private Int2ObjectMap<GlyphData> data=new Int2ObjectOpenHashMap<>();
	private Int2ObjectMap<GlyphData> unicodeData=new Int2ObjectOpenHashMap<>();
	IResourceManager rm;
	private KGlyphProvider() {
	
	}
	public GlyphData getGlyph(int code) {
		if(ClientUtils.mc().gameSettings.forceUnicodeFont) {
			return unicodeData.get(code);
		}
		return data.get(code);
	}
	public void readFont(JsonObject jo) {
		System.out.println("loading fonts...");
		JsonArray ja=jo.get("providers").getAsJsonArray();
		for(int i=0;i<ja.size();i++) {
			JsonObject cr=ja.get(i).getAsJsonObject();
			switch(cr.get("type").getAsString()) {
			case "bitmap":readBitmap(cr);break;
			case "legacy_unicode":readUnicode(cr);break;
			}
		}
		System.out.println("loaded "+data.size());
	}
	public void readBitmap(JsonObject unicode) {
		int height=9;
		if(unicode.has("height"))
			height=unicode.get("height").getAsInt();
		int width=unicode.get("ascent").getAsInt();
		ResourceLocation file=new ResourceLocation(unicode.get("file").getAsString());

		
		try {
			BufferedImage image=ImageIO.read(rm.getResource(new ResourceLocation(file.getNamespace(),"textures/"+file.getPath())).getInputStream());
			JsonArray ja=unicode.get("chars").getAsJsonArray();
			if(image!=null) {
			for(int i=0;i<ja.size();i++) {
				String codepoints=ja.get(i).getAsString();
					for(int j=0;j<codepoints.length();j++) {
						int n=codepoints.codePointAt(j);
						if(n==0)continue;
						GlyphData gd=new GlyphData(j*width,i*height,width,height);
						gd.image=image;
						data.putIfAbsent(n, gd);
					}
				}
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	public void readUnicode(JsonObject unicode) {
		System.out.println(unicode);
		String sizes=unicode.get("sizes").getAsString();
		String template=unicode.get("template").getAsString();
		ResourceLocation rrl=new ResourceLocation(template);
		byte[] sizesb=new byte[65536];
		try {
			rm.getResource(new ResourceLocation(sizes)).getInputStream().read(sizesb);
			for(int i=0;i<=0xFF;i++) {
				BufferedImage image=ImageIO.read(rm.getResource(new ResourceLocation(rrl.getNamespace(),"textures/"+String.format(rrl.getPath(),String.format("%02x",i)))).getInputStream());
				if(image!=null) {
					for(int j=0;j<=0Xff;j++) {
						GlyphData gd=new GlyphData((j&0xF)<<4,(j&0xF0));
						int n=i<<8+j;
						gd.image=image;
						gd.parseSize(sizesb[n]);
						unicodeData.put(n, gd);
						data.putIfAbsent(n, gd);
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	public void onResourceManagerReload(IResourceManager resourceManager) {
		System.out.println("123456");
		rm=resourceManager;
		JsonParser jp=new JsonParser();
		try {
			Collection<ResourceLocation> cls=rm.getAllResourceLocations("font", (p_215274_0_) -> p_215274_0_.endsWith(".json"));
			for(ResourceLocation rl:cls) {
				System.out.println(rl);
				if(rl.getPath().contains("default"))
				readFont(jp.parse(FileUtil.readString(
					rm.getResource(rl).getInputStream())).getAsJsonObject());
			}
		} catch (JsonSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	protected Object prepare(IResourceManager resourceManagerIn, IProfiler profilerIn) {
		onResourceManagerReload(resourceManagerIn);
		return new Object();
	}
	@Override
	protected void apply(Object objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
		// TODO Auto-generated method stub
		
	}
}
