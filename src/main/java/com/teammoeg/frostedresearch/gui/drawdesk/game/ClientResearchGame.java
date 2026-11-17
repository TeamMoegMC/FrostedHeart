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

package com.teammoeg.frostedresearch.gui.drawdesk.game;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacket;

import net.minecraft.core.BlockPos;

public class ClientResearchGame implements Consumer<ResearchGame> {
	ResearchGame rg;
	CardPos lastSelect = null;// transient
	Map<Integer, CardStat> stats = new LinkedHashMap<>();// transient
	BlockPos bp;
	private boolean ideaLink, pending;

	public ClientResearchGame(ResearchGame rg, BlockPos bp) {
		super();
		this.rg = rg;
		this.bp = bp;
		rg.listener = this;
		this.calculateCardNum();
	}

	@Override
	public void accept(ResearchGame t) {
		if (ideaLink) { pending = false; lastSelect = null; }
		this.calculateCardNum();
		ResearchUtils.refreshResearchGui();
	}

	public void attach() {
		rg.listener = this;
	}

	/** One shared desk view can show either the persisted research board or a transient idea-link board. */
	public void bind(ResearchGame game, boolean linking) {
		if (rg == game && ideaLink == linking) { attach(); return; }
		deinit();
		rg = game;
		ideaLink = linking;
		pending = false;
		lastSelect = null;
		attach();
		calculateCardNum();
	}

	public void calculateCardNum() {
		stats.clear();
		Map<Integer, CardStat> stat = new HashMap<>();
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 9; j++) {
				Card c = get(i, j);
				CardStat cs = stat.computeIfAbsent(c.pack(), k -> new CardStat(c.ct, c.card));
				if (c.show) {
					cs.num++;
				}
				cs.tot++;
			}
		stat.values().stream().filter(e -> e.tot > 0).filter(c -> (c.type == CardType.SIMPLE) || (c.type == CardType.PAIR && c.card % 2 == 0) || (c.type == CardType.ADDING && c.card != 0))
			.sorted(Comparator.comparingInt(CardStat::pack)).forEach(e -> stats.put(e.pack(), e));

	}

	public void deinit() {
		rg.listener = null;
	}

	public Card get(CardPos card) {
		return rg.get(card);
	}

	public Card get(int x, int y) {
		return rg.get(x, y);
	}

	public ResearchGame getGame() {
		return rg;
	}

	public CardPos getLastSelect() {
		return lastSelect;
	}

	public int getLevel() {
		return rg.lvl;
	}

	public Map<Integer, CardStat> getStats() {
		return stats;
	}

	public void init() {
		if (ideaLink && pending) return;
		FRNetwork.INSTANCE.sendToServer(ideaLink ? FHDrawingDeskOperationPacket.linkReset(bp)
			: new FHDrawingDeskOperationPacket(bp));
		if (ideaLink) pending = true;
	}

	private void sendMove(CardPos first, CardPos second) {
		FRNetwork.INSTANCE.sendToServer(ideaLink ? FHDrawingDeskOperationPacket.linkMove(bp, first, second)
			: second == null ? new FHDrawingDeskOperationPacket(bp, first)
			: new FHDrawingDeskOperationPacket(bp, first, second));
		if (ideaLink) pending = true;
	}

	public boolean isTouchable(CardPos card) {
		return rg.isTouchable(card);
	}

	public void reset() {
		lastSelect = null;
	}

	/**
	 * Testing shortcut for players allowed to cheat: asks the server to finish the running game.
	 *
	 * @return whether the click was the final card of a running game
	 */
	public boolean finishForTesting(CardPos pos) {
		if (ideaLink && pending) return false;
		if (!ResearchGame.FINAL_CARD.equals(pos) || rg.isEmpty())
			return false;
		lastSelect = null;
		sendMove(pos, null);
		return true;
	}

	public void select(CardPos pos) {
		if (ideaLink && pending) return;
		if (!rg.isTouchable(pos)) return;
		if (!pos.equals(lastSelect) && tryCombine(pos, lastSelect)) {
			lastSelect = null;
			return;
		}
		if (lastSelect == null) {
			lastSelect = pos;
		} else
			lastSelect = null;

	}

	public boolean tryCombine(CardPos c1, CardPos c2) {
		if (ideaLink && pending) return false;
		if (c2 == null) {
			if (rg.addcur == rg.addmax && isTouchable(c1)) {
				Card c = get(c1);
				if (c.ct == CardType.ADDING && c.card == 8) {
					sendMove(c1, null);
					return true;
				}
			}
		} else {
			if (rg.canCombine(c1, c2)) {
				sendMove(c1, c2);
				return true;
			}
		}
		return false;
	}
}
