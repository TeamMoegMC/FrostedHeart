package com.teammoeg.frostedheart.research.gui;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.clues.AbstractClue;
import com.teammoeg.frostedheart.research.effects.*;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

import static com.teammoeg.frostedheart.research.gui.ResearchDetailPanel.PADDING;

public class ResearchInfoPanel extends Panel {

    ResearchDetailPanel detailPanel;

    public ResearchInfoPanel(ResearchDetailPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        detailPanel = panel;
    }

    @Override
    public void addWidgets() {
        int offset = 0;

        // exp materials
        TextField req = new TextField(this);
        req.setMaxWidth(width).setText(GuiUtils.translateGui("research.requirements").mergeStyle(TextFormatting.UNDERLINE)).setPos(0, offset);
        add(req);
        offset += req.height+1;

        int xoffset = 0;
        for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
            if (ingredient.getMatchingStacks().length != 0) {
                ItemStack toDisplay = ingredient.getMatchingStacks()[0];
                Icon icon = ItemIcon.getItemIcon(toDisplay);
                Button button = new Button(this) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {

                    }
                };

                button.setPosAndSize(xoffset, offset, 16, 16);
                button.setIcon(icon);
                button.setTitle(new TranslationTextComponent(toDisplay.getTranslationKey()).appendString(" x " + toDisplay.getCount()));
                add(button);

                xoffset += 17;
            }
        }
        offset += 17;

        // commit items button
        Button commitItems = new SimpleTextButton(this, GuiUtils.translateGui("research.commit_material_and_start"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {

                // check materials
                boolean hasAllMaterials = true;
                for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
                    if (!hasAllMaterials)
                        break;
                    // each ingredient
                    ItemStack[] matchingStacks = ingredient.getMatchingStacks();
                    boolean alreadyFound = false;
                    for (ItemStack requiredStack : matchingStacks) {
                        if (alreadyFound) break;
                        for(ItemStack invStack : detailPanel.researchScreen.player.inventory.mainInventory) {
                            if (!invStack.isEmpty() && invStack.isItemEqual(requiredStack) && invStack.getCount() >= requiredStack.getCount()) {
                                alreadyFound = true;
                                break;
                            }
                        }
                    }
                    if (!alreadyFound)
                        hasAllMaterials = false;
                }

                // commit materials
                if (hasAllMaterials) {
                    for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
                        // each ingredient
                        ItemStack[] matchingStacks = ingredient.getMatchingStacks();
                        boolean alreadyFound = false;
                        for (ItemStack requiredStack : matchingStacks) {
                            if (alreadyFound) break;
                            for(ItemStack invStack : detailPanel.researchScreen.player.inventory.mainInventory) {
                                if (!invStack.isEmpty() && invStack.isItemEqual(requiredStack) && invStack.getCount() >= requiredStack.getCount()) {
                                    invStack.shrink(requiredStack.getCount());
                                    alreadyFound = true;
                                    break;
                                }
                            }
                        }
                    }

                    // TODO: mark research as in progress
                }
            }
        };

        commitItems.setPos(0, offset);
        add(commitItems);
        offset += commitItems.height+1;

        offset += 5;

        // effects
        TextField effects = new TextField(this);
        effects.setMaxWidth(width).setText(GuiUtils.translateGui("research.effects").mergeStyle(TextFormatting.UNDERLINE)).setPos(0, offset);
        add(effects);
        offset+=effects.height+1;

        for (Effect effect : detailPanel.research.getEffects()) {

            // effect name
            TextField effectName = new TextField(this);
            effectName.setMaxWidth(width).setText(effect.getName().mergeStyle(TextFormatting.BOLD)).setPos(0, offset);
            add(effectName);
            offset += effectName.height+1;

            // item reward
            if (effect instanceof EffectItemReward) {
                List<ItemStack> rewards = ((EffectItemReward) effect).getRewards();
                for (int i = 0; i < rewards.size(); i++) {
                    Icon icon = ItemIcon.getItemIcon(rewards.get(i));
                    Button button = new Button(this) {
                        @Override
                        public void onClicked(MouseButton mouseButton) {

                        }
                    };
                    button.setPosAndSize(i*17, offset, 16, 16);
                    button.setIcon(icon);
                    button.setTitle(new TranslationTextComponent(rewards.get(i).getTranslationKey()).appendString(" x " + rewards.get(i).getCount()));
                    add(button);
                }
                // additional offset caused by item icons
                offset += 17;
            }

            // building
            if (effect instanceof EffectBuilding) {
                Block block = ((EffectBuilding) effect).getBlock();
                Icon icon = ItemIcon.getItemIcon(block.asItem());
                Button button = new Button(this) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {

                    }
                };
                button.setPosAndSize(0, offset, 32, 32);
                button.setIcon(icon);
                button.setTitle(block.getTranslatedName());
                add(button);
                offset += 33;
            }

            // crafting
            if (effect instanceof EffectCrafting) {
                List<Item> itemsToCraft = ((EffectCrafting) effect).getItemsToCraft();
                for (int i = 0; i < itemsToCraft.size(); i++) {
                    Icon icon = ItemIcon.getItemIcon(itemsToCraft.get(i));
                    Button button = new Button(this) {
                        @Override
                        public void onClicked(MouseButton mouseButton) {

                        }
                    };
                    button.setPosAndSize(i*17, offset, 16, 16);
                    button.setIcon(icon);
                    button.setTitle(new TranslationTextComponent(itemsToCraft.get(i).getTranslationKey()));
                    add(button);
                }
                // additional offset caused by item icons
                offset += 17;
            }

            // use
            if (effect instanceof EffectUse) {
                List<Block> blocksToUse = ((EffectUse) effect).getBlocksToUse();
                for (int i = 0; i < blocksToUse.size(); i++) {
                    Icon icon = ItemIcon.getItemIcon(blocksToUse.get(i).asItem());
                    Button button = new Button(this) {
                        @Override
                        public void onClicked(MouseButton mouseButton) {

                        }
                    };
                    button.setPosAndSize(i*17, offset, 16, 16);
                    button.setIcon(icon);
                    button.setTitle(new TranslationTextComponent(blocksToUse.get(i).getTranslationKey()));
                    add(button);
                }
                // additional offset caused by item icons
                offset += 17;
            }

            // stats
            if (effect instanceof EffectStats) {
                String info = ((EffectStats) effect).getUpgradeInfo();
                TextField stats = new TextField(this);
                stats.setMaxWidth(width).setText(new StringTextComponent(info)).setPos(0, offset);
                add(stats);
                offset += stats.height;
            }

        }
        offset += 5;

        // clues
        TextField clues = new TextField(this);
        clues.setMaxWidth(width).setText(GuiUtils.translateGui("research.clues").mergeStyle(TextFormatting.UNDERLINE)).setPosAndSize(0, offset, width, PADDING);
        add(clues);
        offset += clues.height+1;

        for (AbstractClue clue : detailPanel.research.getClues()) {
            TextField clueName = new TextField(this);
            clueName.setMaxWidth(width).setText(clue.getName()).setPos(0, offset);
            add(clueName);
            offset+=clueName.height+1;

            TextField clueDesc = new TextField(this);
            clueDesc.setMaxWidth(width).setText(clue.getDescription()).setPos(0, offset);
            add(clueDesc);
            offset+=clueDesc.height+1;

            TextField clueHint = new TextField(this);
            clueHint.setMaxWidth(width).setText(GuiUtils.translateGui("research.hint").appendSibling(clue.getHint())).setPos(0, offset);
            add(clueHint);
            offset+=clueHint.height+1;
        }

        detailPanel.scrollInfo.setMaxValue(offset);

    }

    @Override
    public void alignWidgets() {

    }
}
