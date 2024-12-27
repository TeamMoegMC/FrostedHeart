package com.teammoeg.frostedheart.mixin.client;

import java.io.InputStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.DataResult;
import com.teammoeg.frostedheart.util.mixin.ChangeAdvanceGlyph;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.client.gui.font.providers.BitmapProvider.Glyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

@Mixin(net.minecraft.client.gui.font.providers.BitmapProvider.Definition.class)
public class MixinBitmapProviderDefinition {
	int advance=-256;
	@Shadow
	@Mutable
	int ascent;
	public MixinBitmapProviderDefinition() {

	}
	@Inject(at=@At("HEAD"),method="validate")
	private static void fh$validate(BitmapProvider.Definition p_286662_,CallbackInfoReturnable<DataResult<BitmapProvider.Definition>> res){
		if(p_286662_.ascent()>0xFF&&p_286662_.ascent()>p_286662_.height()) {
			((MixinBitmapProviderDefinition)(Object)p_286662_).advance=((p_286662_.ascent()>>8)&0xff)-1;
			if(((MixinBitmapProviderDefinition)(Object)p_286662_).advance>=128)
				((MixinBitmapProviderDefinition)(Object)p_286662_).advance=((MixinBitmapProviderDefinition)(Object)p_286662_).advance-256;
			//System.out.println("assum advance "+((MixinBitmapProviderDefinition)(Object)p_286662_).advance);
			((MixinBitmapProviderDefinition)(Object)p_286662_).ascent&=0Xff;
		}
		
	}
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/font/providers/BitmapProvider;<init>(Lcom/mojang/blaze3d/platform/NativeImage;Lnet/minecraft/client/gui/font/CodepointMap;)V"),method="load",locals=LocalCapture.CAPTURE_FAILSOFT)
	private void fh$load(ResourceManager p_286694_, CallbackInfoReturnable<GlyphProvider> cir, ResourceLocation resourcelocation, InputStream inputstream, NativeImage nativeimage, int i, int j, int k, int l, float f, CodepointMap codepointmap) {
		if(advance>-256) {
			IntSet keys=new IntAVLTreeSet(codepointmap.keySet());
			//System.out.println("modifying fonts");
			keys.forEach(t->{
				BitmapProvider.Glyph gi=(Glyph) codepointmap.get(t);
				//System.out.println(t+":"+gi);
				if(gi!=null)
					codepointmap.put(t, new BitmapProvider.Glyph(gi.scale(),gi.image(), gi.offsetX(), gi.offsetY(), gi.width(), gi.height(), advance, gi.ascent()) );
			});
		}
	}

}
