/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.trade.gui;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.OverlayItemSlot;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.CheckBox;
import com.teammoeg.chorda.client.cui.ToolTipElement;
import com.teammoeg.chorda.client.cui.TristateCheckBox;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.trade.RelationModifier;
import com.teammoeg.frostedheart.content.trade.TradeConstants;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer.DetectionSlot;
import com.teammoeg.frostedheart.content.trade.network.BargainRequestPacket;
import com.teammoeg.frostedheart.content.trade.network.TradeCommitPacket;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.BuyData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.SellData;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

public class TradeScreen extends PrimaryLayer {

	TradeContainer cx;
	CheckBox tab;
	OverlayItemSlot[] slots = new OverlayItemSlot[27];
	SellData[] sds = new SellData[27];
	OverlayItemSlot[] orders = new OverlayItemSlot[11];
	TristateCheckBox trade;
	TristateCheckBox bargain;
	RelationSlot rels;

	public TradeScreen(TradeContainer cx) {
		super();
		this.cx = cx;
		tab = new CheckBox(this, TradeIcons.PTABSELL, TradeIcons.PTABBUY, false) {

			@Override
			public void onSwitched() {
				super.onSwitched();
				updateOffers();
			}

			@Override
			public void getTooltip(Consumer<Component> tooltip) {
				if (isChecked()) {
					tooltip.accept(Lang.gui("trade.buying").component());
				} else {
					tooltip.accept(Lang.gui("trade.selling").component());
				}
			}

		};
		tab.setPosAndSize(182, 157, 54, 31);
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++) {
				final int no = i + j * 3;

				OverlayItemSlot fs = new OverlayItemSlot(this) {
					@Override
					public boolean onClicked(MouseButton btn) {
						// super.onClicked(btn);
						onSlotClick(no, btn);
						return true;
					}

					@Override
					public void getTooltip(Consumer<Component> tooltip) {
						if (tab.isChecked() || cx.relations.sum() > TradeConstants.RELATION_TO_TRADE) {
							super.getTooltip(tooltip);
						} else tooltip.accept(Component.translatable("gui.frostedheart.trade.no_trade_will"));
					}
				};
				slots[no] = fs;
				fs.setPos(i * 16 + 189, j * 16 + 8);
			}

		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 3; j++) {
				final int no = i + j * 4;
				if (no == 11)
					continue;
				OverlayItemSlot fs = new OverlayItemSlot(this) {
					@Override
					public boolean onClicked(MouseButton btn) {
						// super.onClicked(btn);
						onOrderSlotClick(no, btn);
						return true;
					}

					@Override
					public void getTooltip(Consumer<Component> tooltip) {
						if (cx.relations.sum() > TradeConstants.RELATION_TO_TRADE) {
							super.getTooltip(tooltip);
						} else tooltip.accept(Component.translatable("gui.frostedheart.trade.no_trade_will"));
					}

				};
				orders[no] = fs;
				fs.setPos(i * 16 + 62, j * 16 + 76);
			}
		rels = new RelationSlot(this, () -> cx.relationMinus - cx.discountAmount);
		rels.setTooltip(c -> {
			int relation = cx.relations.sum();
			if (relation < TradeConstants.RELATION_TO_TRADE && !cx.order.isEmpty()) {
				c.accept(Lang.translateGui("trade.unwilling").withStyle(ChatFormatting.DARK_RED));
				return;
			}
			if (cx.discountAmount != 0)
				c.accept(Lang.translateGui("trade.discount").append(Components
					.str(" -" + (int) Math.ceil(cx.discountAmount / 10f)).withStyle(ChatFormatting.GREEN)));
			if (cx.relationMinus > 0)
				c.accept(Lang.translateGui("trade.bad_relation").append(
					Components.str(" " + (int) Math.ceil(cx.relationMinus / 10f)).withStyle(ChatFormatting.RED)));

		});
		rels.setPos(110, 108);
		bargain = new TristateCheckBox(this, TradeIcons.BARGAINN, TradeIcons.BARGAINO, TradeIcons.BARGAIND) {

			@Override
			public boolean onClicked(MouseButton arg0) {
				if (bargain.isEnabled()) {
					FHNetwork.INSTANCE.sendToServer(new BargainRequestPacket(cx.order));
					return true;
				}
				return true;
			}

		};
		trade = new TristateCheckBox(this, TradeIcons.DEALN, TradeIcons.DEALO, TradeIcons.DEALD) {

			@Override
			public boolean onClicked(MouseButton arg0) {
				if (trade.isEnabled()) {
					FHNetwork.INSTANCE.sendToServer(new TradeCommitPacket(cx.order));
					return true;
				}
				return true;
			}

		};
		trade.setPosAndSize(73, 57, 20, 14);
		bargain.setPosAndSize(95, 57, 20, 14);
		updateOffers();
	}

	@Override
	public void getTooltip(Consumer<Component> list) {
		@Nullable
		Slot slotUnderMouse = ((AbstractContainerScreen) this.getScreen().getScreen()).getSlotUnderMouse();
		if (this.cx.getCarried().isEmpty() && slotUnderMouse != null && slotUnderMouse.hasItem()) {
			AbstractContainerScreen.getTooltipFromItem(ClientUtils.getMc(), slotUnderMouse.getItem()).forEach(list::accept);
		}
		super.getTooltip(list);
	}

	@Override
	public void addUIElements() {
		super.addUIElements();
		TextField ptf = new TextField(this).centerV().centerH().shadow().setMaxWidth(56).setMinWidth(56)
			.setMaxLines(1).setText(Lang.translateGui("trade.me"));
		ptf.setPos(0, 119);
		super.add(ptf);
		TextField vptf = new TextField(this).centerV().centerH().shadow().setMaxWidth(56).setMinWidth(56)
			.setMaxLines(1).setText(Lang.translateGui("trade.profession." + cx.data.policytype));
		vptf.setPos(132, 119);
		super.add(vptf);

		TextField vltf = new TextField(this).centerV().centerH().shadow().setMaxWidth(56).setMinWidth(56)
			.setMaxLines(1).setText(Lang.translateKey("merchant.level." + (cx.data.getTradeLevel() + 1)));
		vltf.setPos(132, 34);
		super.add(vltf);

		super.add(trade);

		super.add(bargain);

		super.add(tab);
		ToolTipElement ttw = new ToolTipElement(this, list -> {
			int tot = cx.relations.sum();
			list.accept(Lang.translateGui("trade.relation").append(Components.str(tot > 0 ? " +" + tot : "" + tot)
				.withStyle(tot > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
			if (tot <= TradeConstants.RELATION_TO_TRADE) {
				list.accept(Component.translatable("gui.frostedheart.trade.no_trade_will"));
			}
			for (RelationModifier m : RelationModifier.values()) {
				int rel = cx.relations.get(m);
				if (rel == 0)
					continue;
				MutableComponent tx = Components.str(rel > 0 ? " +" + rel : " " + rel)
					.withStyle(rel > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
				list.accept(m.getDesc().append(tx));

			}
		});
		ttw.setPosAndSize(133, 18, 54, 5);
		super.add(ttw);
		for (OverlayItemSlot fs : slots)
			super.add(fs);
		for (OverlayItemSlot fs : orders)
			super.add(fs);
		super.add(rels);
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		TradeIcons.MAIN.draw(matrixStack, x, y, w, h);
		TradeIcons.REL.draw(matrixStack, x + 133, y + 18, 54, 5);
		int repos = cx.relations.sum();
		repos = Math.min(Math.max(repos, -50), 50);
		int reposx = (repos + 50) * 50 / 100;
		TradeIcons.PTR.draw(matrixStack, x + reposx + 133, y + 17, 3, 7);

		TradeIcons.icons[cx.balance + 3].draw(matrixStack, x, y, 56, 45);
		if (cx.poffer != 0)
			TradeIcons.POFFER_EMP.draw(matrixStack, x + 54, y, 76, 60);
		if (!cx.order.isEmpty())
			TradeIcons.VOFFER_EMP.draw(matrixStack, x + 57, y + 73, 74, 60);
		if (tab.isChecked())
			TradeIcons.OVLBUY.draw(matrixStack, x + 189, y + 56, 48, 48);
		else
			TradeIcons.OVLSELL.draw(matrixStack, x + 189, y + 56, 48, 48);
		int max = cx.policy.maxExp;
		if (max > 0) {
			float progress = ((float) (cx.data.totaltraded)) / max;
			progress = Mth.clamp(progress, 0, 1);
			TradeIcons.EXP.draw(matrixStack, 133 + x, 25 + y, (int) (54 * progress), 5);
		} else {
			TradeIcons.EXP.draw(matrixStack, 133 + x, 25 + y, 54, 5);
		}
		InventoryScreen.renderEntityInInventoryFollowsMouse(matrixStack, x + 28, y + 115, 30, (float) ((x + 8) - this.getMouseX()),
			(float) ((y + 75 - 50) - this.getMouseY()), ClientUtils.getPlayer());
		InventoryScreen.renderEntityInInventoryFollowsMouse(matrixStack, x + 160, y + 115, 30, (float) ((x + 140) - this.getMouseX()),
			(float) ((y + 75 - 50) - this.getMouseY()), cx.data.parent);
	}

	@Override
	public void drawForeground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		super.drawForeground(matrixStack, x, y, w, h);
		for (DetectionSlot ds : cx.slots) {
			if (ds.isSaleable) {
				TradeIcons.SALEABLE.draw(matrixStack, ds.x + x, ds.y + y, 7, 6);
			}
		}
	}

	@Override
	public boolean onInit() {
		int sw = 244;
		int sh = 227;
		this.setSize(sw, sh);
		return super.onInit();
	}

	public void onOrderSlotClick(int sno, MouseButton btn) {

		boolean shift = CInputHelper.isShiftKeyDown();
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
		if (!tab.isChecked()) {
			if (cx.relations.sum() <= TradeConstants.RELATION_TO_TRADE) {
				return;
			}
			SellData sd = sds[sno];
			boolean shift = CInputHelper.isShiftKeyDown();
			if (cx.order.size() >= 11 && !cx.order.containsKey(sd.getId()))
				return;
			if (sd == null)
				return;
			if (shift) {
				cx.order.put(sd.getId(), sd.getStore());
				// slots[sno].setCount(0);
			} else
				cx.order.compute(sd.getId(), (k, v) -> {
					int cnt = v == null ? 0 : v;
					if (btn == MouseButton.LEFT)
						cnt++;
					else if (btn == MouseButton.RIGHT) {
						int remain = sd.getStore() - cnt;
						if (remain > 0) {
							cnt += Mth.ceil(remain / 2f);
						}
					}
					cnt = Math.min(sd.getStore(), cnt);
					// slots[sno].setCount(sd.getStore()-cnt);
					return cnt > 0 ? cnt : null;
				});
			updateOrders();

		}
	}

	public void updateOffers() {
		for (OverlayItemSlot fs : slots)
			fs.clear();
		Arrays.fill(sds, null);
		if (tab.isChecked()) {
			int start = 0;
			int end = Math.min(cx.policy.getBuys().size(), start + 27);
			int j = 0;
			if (start < end)
				for (int i = start; i < end; i++) {
					BuyData cur = cx.policy.getBuys().get(i);
					OverlayItemSlot slot = slots[j++];
					if (cur.getStore() == 0) {
						slot.setOverlay(TradeIcons.NOBUY, 7, 6);
						slot.setTooltips(c -> c.accept(Lang.translateGui("trade.not_needed_now").withStyle(ChatFormatting.RED)));
					} else {
						slot.resetOverlay();
						slot.setTooltips(null);
					}
					slot.setItem(cur.getItem());
					slot.setCountOverride(cur.getStore());
				}
		} else {
			int start = 0;
			int end = Math.min(cx.policy.getSells().size(), start + 27);
			int j = 0;
			if (start < end) {
				end -= start;
				Iterator<SellData> it = cx.policy.getSells().values().stream().skip(start).limit(27).iterator();
				while (it.hasNext()) {
					SellData sd = it.next();
					sds[j] = sd;
					OverlayItemSlot slot = slots[j++];
					if (sd.getStore() == 0) {
						if (sd.canRestock(cx.data) > 0) {
							slot.setOverlay(TradeIcons.STOCKOUT, 7, 6);
							slot.setTooltips(c -> c.accept(Lang.translateGui("trade.no_stock").withStyle(ChatFormatting.RED)));
						} else {
							slot.setOverlay(TradeIcons.NORESTOCK, 7, 6);
							slot.setTooltips(c -> c.accept(Lang.translateGui("trade.not_restocking").withStyle(ChatFormatting.RED)));
						}
					} else {
						if (sd.isFullStock()) {
							slot.setOverlay(TradeIcons.FULL, 7, 6);
							slot.setTooltips(c -> c.accept(Lang.translateGui("trade.full_stock").withStyle(ChatFormatting.GREEN)));
						} else if (sd.canRestock(cx.data) > 0) {
							slot.setOverlay(TradeIcons.RESTOCKS, 7, 6);
							slot.setTooltips(c -> c.accept(Lang.translateGui("trade.restocking").withStyle(ChatFormatting.YELLOW)));
						} else {
							slot.resetOverlay();
							slot.setTooltips(c -> c.accept(Lang.translateGui("trade.not_restocking")));
						}
					}
					slot.setItem(sd.getItem());
					slot.setCountOverride(sd.getStore());
				}
			}
		}
		updateTrade();
	}

	public void updateOrders() {
		for (OverlayItemSlot fs : orders)
			fs.clear();
		Iterator<Entry<String, Integer>> it = cx.order.entrySet().iterator();
		int j = 0;
		while (it.hasNext()) {
			Entry<String, Integer> cur = it.next();
			OverlayItemSlot curs = orders[j++];
			curs.setItem(cx.policy.getSells().get(cur.getKey()).getItem());
			curs.setCountOverride(cur.getValue());
		}
		updateTrade();
	}

	public void updateTrade() {
		cx.recalc();
		if (cx.balance >= 0 && cx.poffer > 0) {

			trade.setEnabled(true);

			if (cx.originalVOffer > cx.poffer) {
				trade.setNormal(TradeIcons.DEALGRN);
				trade.setOver(TradeIcons.DEALGRNO);
				trade.setTooltips(c -> c.accept(Lang.translateGui("trade.discounted_offering")));
			} else if (cx.poffer - cx.originalVOffer > 10) {
				trade.setNormal(TradeIcons.DEALYEL);
				trade.setOver(TradeIcons.DEALYELO);
				trade.setTooltips(c -> c.accept(Lang.translateGui("trade.too_much_offering")));
			} else {
				trade.setNormal(TradeIcons.DEALN);
				trade.setOver(TradeIcons.DEALO);
				trade.setTooltips(c -> c.accept(Lang.translateGui("trade.deal")));
			}
		} else {
			trade.setTooltips(c -> c.accept(Lang.translateGui("trade.not_enough_offering")));
			trade.setEnabled(false);
		}
		if (cx.relations.sum() >= 40) {
			if (cx.order.isEmpty()) {
				bargain.setTooltips(c -> c.accept(Lang.translateGui("trade.trade_to_bargain")));
				bargain.setEnabled(false);
			} else {
				if (cx.discountRatio > 0.4) {
					bargain.setTooltips(c -> c.accept(Lang.translateGui("trade.maxed_bargain")));
					bargain.setEnabled(false);
				} else {
					bargain.setTooltips(c -> c.accept(Lang.translateGui("trade.bargain")));
					bargain.setEnabled(true);
				}
			}

		} else {
			bargain.setTooltips(c -> c.accept(Lang.translateGui("trade.no_bargain")));
			bargain.setEnabled(false);
		}
	}
	@Override
	public int getContentWidth() {
		return 244;
	}

	@Override
	public int getContentHeight() {
		return 227;
	}

}
