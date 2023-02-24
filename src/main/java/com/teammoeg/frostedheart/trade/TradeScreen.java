/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.trade;

import java.util.Iterator;
import java.util.Map.Entry;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.gui.FakeSlot;
import com.teammoeg.frostedheart.research.gui.RTextField;
import com.teammoeg.frostedheart.research.gui.SwitchButton;
import com.teammoeg.frostedheart.research.gui.ToolTipWidget;
import com.teammoeg.frostedheart.research.gui.TristateButton;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TradeScreen extends BaseScreen {
	TradeContainer cx;
	SwitchButton tab;
	FakeSlot[] slots = new FakeSlot[27];
	SellData[] sds = new SellData[27];
	FakeSlot[] orders = new FakeSlot[12];
	TristateButton trade;
	TristateButton bargain;
	public TradeScreen(TradeContainer cx) {
		this.cx = cx;
		tab = new SwitchButton(this, TradeIcons.PTABSELL, TradeIcons.PTABBUY, false) {

			@Override
			public void onSwitched() {
				super.onSwitched();
				updateOffers();
			}
			
		};
		tab.setPosAndSize(182, 157, 54, 31);
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++) {
				final int no = i + j * 3;
				FakeSlot fs = new FakeSlot(this) {
					@Override
					public void onClick(MouseButton btn) {
						super.onClick(btn);
						onSlotClick(no, btn);
					}
				};
				slots[no] = fs;
				fs.setPos(i * 16 + 189, j * 16 + 8);
			}
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 3; j++) {
				final int no = i + j * 4;
				FakeSlot fs = new FakeSlot(this) {
					@Override
					public void onClick(MouseButton btn) {
						super.onClick(btn);
						onOrderSlotClick(no, btn);
					}
				};
				orders[no] = fs;
				fs.setPos(i * 16 + 62, j * 16 + 76);
			}
		updateOffers();
	}

	@Override
	public boolean onInit() {
		int sw = 244;
		int sh = 227;
		this.setSize(sw, sh);
		return super.onInit();
	}

	public void onOrderSlotClick(int sno, MouseButton btn) {
		boolean shift = Widget.isShiftKeyDown();
		String sd = cx.order.keySet().stream().skip(sno).findFirst().orElse(null);
		if (sd == null)
			return;
		if (shift)
			cx.order.remove(sd);
		else
			cx.order.compute(sd, (k, v) -> {
				int cnt = v == null ? 0 : v;
				if (btn == MouseButton.LEFT)
					cnt--;
				else if (btn == MouseButton.RIGHT)
					cnt = cnt / 2;
				return cnt > 0 ? cnt : null;
			});
		updateOrders();
	}

	public void onSlotClick(int sno, MouseButton btn) {
		if (!tab.getState()) {
			SellData sd = sds[sno];
			boolean shift = Widget.isShiftKeyDown();
			if (sd == null)
				return;
			if (shift) {
				cx.order.put(sd.id, sd.getStore());
				//slots[sno].setCount(0);
			}else
				cx.order.compute(sd.id, (k, v) -> {
					int cnt = v == null ? 0 : v;
					if (btn == MouseButton.LEFT)
						cnt++;
					else if (btn == MouseButton.RIGHT) {
						int remain = sd.getStore() - cnt;
						if (remain > 0) {
							cnt += MathHelper.ceil(remain / 2f);
						}
					}
					cnt=Math.min(sd.getStore(), cnt);
					//slots[sno].setCount(sd.getStore()-cnt);
					return cnt > 0 ? cnt : null;
				});
			updateOrders();
			
		}
	}
	public void updateOrders() {
		for (FakeSlot fs : orders)
			fs.clear();
		Iterator<Entry<String, Integer>> it=cx.order.entrySet().iterator();
		int j=0;
		while(it.hasNext()) {
			Entry<String, Integer> cur=it.next();
			FakeSlot curs=orders[j++];
			curs.setSlot(cx.policy.sells.get(cur.getKey()).getItem());
			curs.setCount(cur.getValue());
		}
		cx.recalc();
		if(cx.balance>=0&&cx.poffer>0) {
			trade.setEnabled(true);
			trade.resetTooltips();
			if(cx.originalVOffer>cx.poffer) {
				trade.setNormal(TradeIcons.DEALGRN);
				trade.setTooltips(c->c.add(GuiUtils.translateGui("trade.discounted_offering")));
			}else if(cx.balance>1) {
				trade.setNormal(TradeIcons.DEALYEL);
				trade.setTooltips(c->c.add(GuiUtils.translateGui("trade.too_much_offering")));
			}
		}else {
			trade.setTooltips(c->c.add(GuiUtils.translateGui("trade.not_enough_offering")));
		}
		if(cx.relations.sum()>30&&cx.voffer>0) {
			bargain.setEnabled(true);
		}else bargain.setEnabled(false);
	}
	public void updateOffers() {
		for (FakeSlot fs : slots)
			fs.clear();
		System.out.println(cx.policy);
		for (int i = 0; i < sds.length; i++)
			sds[i] = null;
		if (tab.getState()) {
			int start = 0;
			int end = Math.min(cx.policy.buys.size(), start + 27);
			int j = 0;
			if (start < end)
				for (int i = start; i < end; i++) {
					BuyData cur = cx.policy.buys.get(i);
					FakeSlot slot = slots[j++];
					slot.setSlot(cur.getItem());
					slot.setCount(cur.getStore());
				}
		} else {
			int start = 0;
			int end = Math.min(cx.policy.sells.size(), start + 27);
			int j = 0;
			if (start < end) {
				end -= start;
				Iterator<SellData> it = cx.policy.sells.values().stream().skip(start).limit(27).iterator();
				while (it.hasNext()) {
					SellData sd = it.next();
					sds[j] = sd;
					FakeSlot slot = slots[j++];
					slot.setSlot(sd.getItem());
					slot.setCount(sd.getStore());
				}
			}
		}

	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		TradeIcons.MAIN.draw(matrixStack, x, y, w, h);
		TradeIcons.REL.draw(matrixStack, x + 133, y + 18, 54, 5);
		int repos = cx.relations.sum();
		repos = Math.min(Math.max(repos, -50), 50);
		int reposx = (repos + 50) * 54 / 100;
		TradeIcons.PTR.draw(matrixStack, x + reposx + 133, y + 17, 3, 7);

		TradeIcons.icons[cx.balance + 3].draw(matrixStack, x, y, 56, 45);// TODO: move balance
		if (tab.getState())
			TradeIcons.OVLBUY.draw(matrixStack, x + 189, y + 56, 48, 48);
		else
			TradeIcons.OVLSELL.draw(matrixStack, x + 189, y + 56, 48, 48);
		InventoryScreen.drawEntityOnScreen(x + 28, y + 115, 30, (float) (x + 8) - this.getMouseX(),
				(float) (y + 75 - 50) - this.getMouseY(), ClientUtils.getPlayer());
		InventoryScreen.drawEntityOnScreen(x + 160, y + 115, 30, (float) (x + 140) - this.getMouseX(),
				(float) (y + 75 - 50) - this.getMouseY(), cx.data.parent);
	}

	@Override
	public void addWidgets() {
		

		RTextField ptf = new RTextField(this).addFlags(Theme.CENTERED | Theme.SHADOW).setMaxWidth(56).setMinWidth(56)
				.setMaxLine(1).setText(GuiUtils.translateGui("trade.me"));
		ptf.setPos(0, 119);
		super.add(ptf);
		RTextField vptf = new RTextField(this).addFlags(Theme.CENTERED | Theme.SHADOW).setMaxWidth(56).setMinWidth(56)
				.setMaxLine(1).setText(GuiUtils.translateGui("trade.profession." + cx.data.policytype));
		vptf.setPos(132, 119);
		super.add(vptf);

		RTextField vltf = new RTextField(this).addFlags(Theme.CENTERED | Theme.SHADOW).setMaxWidth(56).setMinWidth(56)
				.setMaxLine(1).setText(new TranslationTextComponent("merchant.level." + (cx.data.getTradeLevel() + 1)));
		vltf.setPos(132, 34);
		super.add(vltf);

		super.add(trade = new TristateButton(this, TradeIcons.DEALN, TradeIcons.DEALO, TradeIcons.DEALD) {

			@Override
			public void onClicked(MouseButton arg0) {
			}

		});
		trade.setPosAndSize(139, 2, 20, 14);
		super.add(bargain = new TristateButton(this, TradeIcons.BARGAINN, TradeIcons.BARGAINO, TradeIcons.BARGAIND) {

			@Override
			public void onClicked(MouseButton arg0) {
			}

		});
		bargain.setPosAndSize(161, 2, 20, 14);
		super.add(tab);
		ToolTipWidget ttw = new ToolTipWidget(this, list -> {
			int tot = cx.relations.sum();
			list.add(GuiUtils.translateGui("trade.relation").appendSibling(GuiUtils.str(tot > 0 ? " +" + tot : "" + tot)
					.mergeStyle(tot > 0 ? TextFormatting.GREEN : TextFormatting.RED)));

			for (RelationModifier m : RelationModifier.values()) {
				int rel = cx.relations.get(m);
				if (rel == 0)
					continue;
				IFormattableTextComponent tx = new StringTextComponent(rel > 0 ? " +" + rel : " " + rel)
						.mergeStyle(rel > 0 ? TextFormatting.GREEN : TextFormatting.RED);
				list.add(m.getDesc().appendSibling(tx));

			}
		});
		ttw.setPosAndSize(133, 18, 54, 5);
		super.add(ttw);
		for (FakeSlot fs : slots)
			super.add(fs);
		for(FakeSlot fs:orders)
			super.add(fs);
	}

}
