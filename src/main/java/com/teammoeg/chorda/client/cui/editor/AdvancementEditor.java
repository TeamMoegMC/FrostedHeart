package com.teammoeg.chorda.client.cui.editor;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.lang.Components;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;

public class AdvancementEditor extends Layer {
	Advancement advancement;
	String criterion;
    public AdvancementEditor(UIWidget panel, Component lbl, Pair<Advancement,String> e) {
        super(panel);
        if(e!=null) {
        	advancement=e.getFirst();
        	criterion=e.getSecond();
        }
    }
    public Pair<Advancement,String> getValue(){
    	if(advancement==null)
    		return null;
    	return Pair.of(advancement, criterion);
    }
    @Override
    public void addUIElements() {
    	Component text=Components.empty();
    	CIcon icon=CIcons.nop();
    	if(advancement!=null) {
    		DisplayInfo dp=advancement.getDisplay();
    		if(dp!=null) {
    			text=dp.getTitle();
    			icon=CIcons.getIcon(dp.getIcon());
    		}else
        		text=Components.str(advancement.getId().toString());
    	}
    	
    	
        add(new LabeledOpenEditorButton<>(this,text, Components.str("Select Advancement"), Editors.EDITOR_ADVANCEMENT, advancement,icon, c ->{
        	advancement=c;
        	refresh();
        }));
        add(LabeledSelection.createCriterion(this, Components.str("Select Criterion"), advancement, criterion, c -> criterion = c));

    }
    public void setValue(Pair<Advancement,String> e) {
    	 if(e!=null) {
         	advancement=e.getFirst();
         	criterion=e.getSecond();
         }
    }
	@Override
	public void alignWidgets() {
		this.setWidth(align(false));
	}
}