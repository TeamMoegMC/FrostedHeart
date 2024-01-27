package com.teammoeg.frostedheart.scenario.client.text;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.util.ReferenceValue;

import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;

public class TextInfo {
	public static class SizedReorderingProcessor implements IReorderingProcessor {
		IReorderingProcessor origin;
		int limit = 0;
		boolean isFinished = false;

		public SizedReorderingProcessor(IReorderingProcessor origin) {
			super();
			this.origin = origin;
		}

		public boolean hasText() {
			return limit > 0;
		}

		public IReorderingProcessor asFinished() {
			if (isFinished) return origin;
			return this;
		}
		public int nextSpace() {
			ReferenceValue<Integer> renderTracker = new ReferenceValue<>(0);
			ReferenceValue<Integer> retTracker = new ReferenceValue<>();
			origin.accept((i, s, c) -> {
				if (c != 65533) {
					renderTracker.setVal(renderTracker.getVal() + 1);
				}
				if (renderTracker.getVal() < limit) return true;
				if(Character.isWhitespace(c)) {
					retTracker.setVal(renderTracker.getVal());
				}
				return false;
			});
			retTracker.setIfAbsent(renderTracker::getVal);
			return retTracker.getVal();
		}
		@Override
		public boolean accept(ICharacterConsumer p_accept_1_) {
			ReferenceValue<Integer> renderTracker = new ReferenceValue<>(0);
			return origin.accept((i, s, c) -> {
				isFinished = true;
				if (renderTracker.getVal() < limit) {
					p_accept_1_.accept(i, s, c);
				} else {
					isFinished = false;
					return false;
				}
				if (c != 65533) {
					renderTracker.setVal(renderTracker.getVal() + 1);
				}
				return true;
			});
		}

		public void checkIsFinished() {
			origin.accept((i, s, c) -> {
				isFinished = true;
				if (i >= limit) {
					isFinished = false;
				}
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
	public ITextComponent parent;
	public int line;
	public IReorderingProcessor text;
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

	public TextInfo(ITextComponent parent, int line, IReorderingProcessor text) {
		super();
		this.parent = parent;
		this.line = line;
		this.text = text;
		
	}

	public int getMaxLen() {
		return ClientUtils.mc().fontRenderer.getStringWidth(ClientScene.toString(getFinished()))+30;
	}
	public int getCurLen() {
		return ClientUtils.mc().fontRenderer.getStringWidth(ClientScene.toString(text))+30;
	}
	public IReorderingProcessor asFinished() {
		return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).asFinished() : text;

	}
	public boolean isFinished() {
		return !(text instanceof SizedReorderingProcessor) || ((SizedReorderingProcessor) text).isFinished;
	}

	public boolean hasText() {
		return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).hasText() : true;
	}

	public IReorderingProcessor getFinished() {
		// TODO Auto-generated method stub
		return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).origin : text;
	}
}