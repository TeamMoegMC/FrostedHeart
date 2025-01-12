package com.teammoeg.frostedheart.util.mixin;

import java.util.function.Function;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;

public record ChangeAdvanceGlyph(int advance,GlyphInfo nested) implements GlyphInfo {


	@Override
	public float getAdvance() {
		return advance;
	}

	@Override
	public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_231088_) {
		return nested.bake(p_231088_);
	}

}
