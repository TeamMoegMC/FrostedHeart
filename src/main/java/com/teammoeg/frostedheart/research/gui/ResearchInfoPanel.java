package com.teammoeg.frostedheart.research.gui;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.clues.AbstractClue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.EffectBuilding;
import com.teammoeg.frostedheart.research.effects.EffectItemReward;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
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
        // clues

        TextField clues = new TextField(this);
        clues.setText(GuiUtils.translateGui("research.clues")).setMaxWidth(width).setPosAndSize(0, offset, width, PADDING);
        add(clues);
        offset += clues.height;

        for (AbstractClue clue : detailPanel.research.getClues()) {
            TextField clueName = new TextField(this);
            clueName.setText(clue.getName()).setMaxWidth(width).setPos(0, offset);
            add(clueName);
            offset+=clueName.height;

            TextField clueDesc = new TextField(this);
            clueDesc.setText(clue.getDescription()).setMaxWidth(width).setPos(0, offset);
            add(clueDesc);
            offset+=clueDesc.height;

            TextField clueHint = new TextField(this);
            clueHint.setText(GuiUtils.translateGui("research.hint").appendSibling(clue.getHint())).setMaxWidth(width).setPos(0, offset);
            add(clueHint);
            offset+=clueHint.height;
        }


        // exp materials
        TextField req = new TextField(this);
        req.setText(GuiUtils.translateGui("research.requirements")).setMaxWidth(width).setPos(0, offset);
        add(req);
        offset += req.height;

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
        offset+=17;

        // effects
        TextField effects = new TextField(this);
        effects.setText(GuiUtils.translateGui("research.effects")).setMaxWidth(width).setPos(0, offset);
        add(effects);
        offset+=effects.height;

        for (Effect effect : detailPanel.research.getEffects()) {

            // effect name
            TextField effectName = new TextField(this);
            effectName.setText(effect.getName().mergeStyle(TextFormatting.BOLD)).setMaxWidth(width).setPos(0, offset);
            add(effectName);
            offset += effectName.height;

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

            // use

            // stats

        }

    }

    @Override
    public void alignWidgets() {

    }
}
