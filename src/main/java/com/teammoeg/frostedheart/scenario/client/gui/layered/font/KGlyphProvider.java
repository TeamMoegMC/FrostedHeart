package com.teammoeg.frostedheart.scenario.client.gui.layered.font;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.frostedheart.util.io.FileUtil;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
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
		//System.out.println("loaded "+data.size());
	}
	public void readBitmap(JsonObject unicode) {
		int height=9;
		if(unicode.has("height"))
			height=unicode.get("height").getAsInt();
		int ascent=unicode.get("ascent").getAsInt();
		ResourceLocation file=new ResourceLocation(unicode.get("file").getAsString());

		
		try {
			BufferedImage image=ImageIO.read(rm.getResource(new ResourceLocation(file.getNamespace(),"textures/"+file.getPath())).getInputStream());
			
			JsonArray ja=unicode.get("chars").getAsJsonArray();
			if(image!=null) {
				int i=image.getWidth();
				int j=image.getHeight();
				int k=i/ja.get(0).getAsString().length();
				int l=j/ja.size();
				float f=height*1f/l;
			for(int i1=0;i1<ja.size();i1++) {
				String codepoints=ja.get(i1).getAsString();
					for(int k1=0;k1<codepoints.codePointCount(0, codepoints.length());k1++) {
						int n=codepoints.codePointAt(k1);
						if(n==0)continue;
						int i2=getCharacterWidth(image,k,l,k1,i1);
						GlyphData gd=new GlyphData(k1*k,i1*l,k,l,(int)(0.5D + i2 * f) + 1, ascent,f);
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
	
    private int getCharacterWidth(BufferedImage nativeImageIn, int charWidthIn, int charHeightInsp, int columnIn, int rowIn) {
        int i;
        
        for(i = charWidthIn - 1; i >= 0; --i) {
        	

           for(int k = rowIn * charHeightInsp ; k < rowIn * charHeightInsp +charHeightInsp; ++k) {
              if ((nativeImageIn.getRGB(i+columnIn * charWidthIn, k)&0xFF000000) != 0) {
                 return i + 1;
              }
           }
        }

        return i + 1;
     }
	public void readUnicode(JsonObject unicode) {
		String sizes=unicode.get("sizes").getAsString();
		String template=unicode.get("template").getAsString();
		
		byte[] sizesb=new byte[65536];
		try {
			rm.getResource(new ResourceLocation(sizes)).getInputStream().read(sizesb);
			for(int i=0;i<=0xFF;i++) {
				String hex=String.format("%02x",i);
				ResourceLocation rrl=new ResourceLocation(String.format(template,hex));
				ResourceLocation imgloc=new ResourceLocation(rrl.getNamespace(),"textures/"+rrl.getPath());
				if(!rm.hasResource(imgloc))continue;
				BufferedImage image=ImageIO.read(rm.getResource(imgloc).getInputStream());
				if(image!=null) {
					for(int j=0;j<=0Xff;j++) {
						GlyphData gd=new GlyphData((j&0xF)*16,(j&0xF0));
						int n=(i*0x100)+j;
						gd.image=image;
						gd.parseSize(sizesb[n]);
						unicodeData.put(n, gd);
						gd.isUnicode=true;
						data.putIfAbsent(n, gd);
					}
				}else {
					System.out.println("error loading "+rrl);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	public void onResourceManagerReload(IResourceManager resourceManager) {
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
		/*for(int i='A';i<'z';i++) {
			System.out.println(Character.toString((char)i)+unicodeData.get(i));
		}*/
		return new Object();
	}
	@Override
	protected void apply(Object objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
		// TODO Auto-generated method stub
		
	}
}
