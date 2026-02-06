/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.scenario.client.dialog;

import com.teammoeg.frostedheart.content.scenario.client.ClientScene;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import com.teammoeg.chorda.client.ClientUtils;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

public class TextInfo {
	public static class SizedReorderingProcessor implements FormattedCharSequence {
		FormattedCharSequence origin;
		int limit = 0;
		boolean isFinished = false;

		public SizedReorderingProcessor(FormattedCharSequence origin) {
			super();
			this.origin = origin;
		}

		public boolean hasText() {
			return limit > 0;
		}

		public FormattedCharSequence asFinished() {
			if (isFinished) return origin;
			return this;
		}
		public int nextSpace() {
			MutableInt renderTracker = new MutableInt(0);
			MutableObject<Integer> retTracker = new MutableObject<>(-1);
			origin.accept((i, s, c) -> {
				if (c != 65533) {
					renderTracker.increment();
				}
				if (renderTracker.getValue() < limit) return true;
				if(Character.isWhitespace(c)) {
					retTracker.setValue(renderTracker.getValue());
				}
				return false;
			});
			if(retTracker.getValue()==null)
				return renderTracker.getValue();
			return retTracker.getValue();
		}
		@Override
		public boolean accept(FormattedCharSink p_accept_1_) {
			MutableInt renderTracker = new MutableInt(0);
			return origin.accept((i, s, c) -> {
				isFinished = true;
				if (renderTracker.getValue() < limit) {
					p_accept_1_.accept(i, s, c);
				} else {
					isFinished = false;
					return false;
				}
				if (c != 65533) {
					renderTracker.increment();
				}
				return true;
			});
		}

		public void checkIsFinished() {
			origin.accept((i, s, c) -> {
                isFinished = i < limit;
				return true;
			});
		}

		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}

	}
	public Component parent;
	public int line;
	public FormattedCharSequence text;
	public boolean addLimit(int amount,boolean toSpace) {
		if (text instanceof SizedReorderingProcessor) {
			SizedReorderingProcessor t = (SizedReorderingProcessor) text;
			if (!t.isFinished) {
				if(toSpace)
					t.limit=t.nextSpace();
				else
					t.limit+=amount;
				return true;
			}
		}
		return false;
	}

	public TextInfo(Component parent, int line, FormattedCharSequence text) {
		super();
		this.parent = parent;
		this.line = line;
		this.text = text;
		
	}

	public int getMaxLen() {
		return ClientUtils.getMc().font.width(ClientScene.toString(getFinished()))+30;
	}
	public int getCurLen() {
		return ClientUtils.getMc().font.width(ClientScene.toString(text))+30;
	}
	public FormattedCharSequence asFinished() {
		return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).asFinished() : text;

	}
	public boolean isFinished() {
		return !(text instanceof SizedReorderingProcessor) || ((SizedReorderingProcessor) text).isFinished;
	}

	public boolean hasText() {
		return !(text instanceof SizedReorderingProcessor) || ((SizedReorderingProcessor) text).hasText();
	}

	public FormattedCharSequence getFinished() {
		return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).origin : text;
	}
}