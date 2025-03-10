package com.teammoeg.frostedresearch.research;

import java.util.Collection;

import com.teammoeg.chorda.client.cui.editor.EditListDialog;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder;
import com.teammoeg.chorda.client.cui.editor.Editors;
import com.teammoeg.chorda.client.cui.editor.IngredientEditor;
import com.teammoeg.chorda.client.cui.editor.SelectDialog;
import com.teammoeg.chorda.client.icon.CIconFTBWrapper;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.FTBIconCWrapper;
import com.teammoeg.chorda.client.icon.IconEditor;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskIcons;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardType;
import com.teammoeg.frostedresearch.research.clues.ClueEditors;
import com.teammoeg.frostedresearch.research.effects.EffectEditor;

public class ResearchEditors {

	public static final Editor<Research> EDITOR_RESEARCH = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, FHResearch::getAllResearch,
	Research::getName, e -> new String[] { e.getId(), e.getName().getString() },
	Research::getIcon).open();
	public ResearchEditors() {
	}
	public static final Editor<Collection<Research>> RESEARCH_LIST=(p,l,v,c)->new EditListDialog<>(p, l, v,null,ResearchEditors.EDITOR_RESEARCH, Research::getName, Research::getIcon, c);
	public static final Editor<Research> RESEARCH_EDITOR=EditorDialogBuilder.create(b->b
		.add(Editors.STRING_ID.withName("id").forGetter( Research::getId))
		.add(Editors.STRING.withName("name").forGetter( t->t.name))
		.add(Editors.INT.withName("insight").forGetter( r->r.insight))
		.add(Editors.INT.withName("points").forGetter( r->r.points))
		.add(Editors.openDialog(IconEditor.EDITOR,t->t,t->Components.str("icon")).withName("icon").forGetter( r->r.icon))
		.add(Editors.enumBox(ResearchCategory.class,ResearchCategory::getName,a->CIcons.getIcon(a.getIcon())).withName("category").forGetter( r->r.getCategory()))
		.decorator(Editors.createAction(CIcons.nop(), (d,v)->{
			ClueEditors.RESEARCH_GAME.open(d, Components.str("Edit Minigame"), d.getValue(12), o->{d.setValue(12,o);d.refresh();});
		}).withName("Edit minigame").decorator())
		.add(Editors.openDialog(EditListDialog.STRING_LIST).withName("Edit Description").forGetter( r->r.desc))
		.add(Editors.openDialog(EditListDialog.STRING_LIST).withName("Edit Alternative Description").forGetter( r->r.fdesc))
		.add(Editors.openDialog(RESEARCH_LIST).withName("Edit Parents").forGetter( r->r.getParents()))
		.add(Editors.openDialog(RESEARCH_LIST).withName("Edit Children").forGetter( r->r.getChildren()))
		.add(Editors.openDialog(IngredientEditor.LIST_EDITOR).withName("Edit Ingredients").forGetter(r->r.getRequiredItems()))
		.add(Editors.openDialog(EffectEditor.EFFECT_LIST).withName("Edit Effects").forGetter(r->r.getEffects()))
		.add(Editors.openDialog(ClueEditors.EDITOR_LIST).withName("Edit Clues").forGetter(r->r.getClues()))
		.decorator(Editors.<Research>createAction(CIcons.nop(), (r,t)->{r.setNoSave();t.delete();r.close();}).withName("Remove").decorator())
		.add(Editors.BOOLEAN.withName("Always show").forGetter( r->r.alwaysShow))
		.add(Editors.BOOLEAN.withName("Hide effects").forGetter( r->r.hideEffects))
		.add(Editors.BOOLEAN.withName("Show Alt description before complete").forGetter( r->r.showfdesc))
		.add(Editors.BOOLEAN.withName("Infinite").forGetter( r->r.isInfinite()))
		.add(Editors.BOOLEAN.withName("Hide this research").forGetter( r->r.isHidden))
		.add(Editors.BOOLEAN.withName("Lock this research").forGetter( r->r.isInCompletable()))
		.apply(Research::new));
		

}
