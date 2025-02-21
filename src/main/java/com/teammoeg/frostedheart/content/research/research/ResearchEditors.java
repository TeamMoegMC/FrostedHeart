package com.teammoeg.frostedheart.content.research.research;

import java.util.Collection;

import com.teammoeg.chorda.client.cui.editor.EditListDialog;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder;
import com.teammoeg.chorda.client.cui.editor.Editors;
import com.teammoeg.chorda.client.cui.editor.IngredientEditor;
import com.teammoeg.chorda.client.cui.editor.SelectDialog;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.IconEditor;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.research.clues.ClueEditor;
import com.teammoeg.frostedheart.content.research.research.effects.EffectEditor;

public class ResearchEditors {

	public ResearchEditors() {
	}
	public static final Editor<Collection<Research>> RESEARCH_LIST=(p,l,v,c)->new EditListDialog<>(p, l, v,null,SelectDialog.EDITOR_RESEARCH, Research::getName, Research::getIcon, c);
	public static final Editor<Research> RESEARCH_EDITOR=EditorDialogBuilder.builder()
		.add(Editors.STRING_ID.withName("id"), Research::getId)
		.add(Editors.STRING.withName("name"), t->t.name)
		.add(Editors.INT.withName("insight"), r->r.insight)
		.add(Editors.INT.withName("points"), r->r.points)
		.add(Editors.openDialog(IconEditor.EDITOR,t->t,t->Components.empty()).withName("icon"), r->r.icon)
		.add(Editors.enumBox(ResearchCategory.class).withName("category"), r->r.getCategory())
		.add(Editors.openDialog(EditListDialog.STRING_LIST).withName("Edit Description"), r->r.desc)
		.add(Editors.openDialog(EditListDialog.STRING_LIST).withName("Edit Alternative Description"), r->r.fdesc)
		.add(Editors.openDialog(RESEARCH_LIST).withName("Edit Parents"), r->r.getParents())
		.add(Editors.openDialog(RESEARCH_LIST).withName("Edit Children"), r->r.getChildren())
		.add(Editors.openDialog(IngredientEditor.LIST_EDITOR).withName("Edit Ingredients"),r->r.getRequiredItems())
		.add(Editors.openDialog(EffectEditor.EFFECT_LIST).withName("Edit Effects"),r->r.getEffects())
		.add(Editors.openDialog(ClueEditor.EDITOR_LIST).withName("Edit Clues"), r->r.getClues())
		.addAction(Editors.<Research>createAction(CIcons.nop(), (r,t)->{r.setNoSave();t.delete();r.close();}).withName("Remove"))
		.add(Editors.BOOLEAN.withName("Always show"), r->r.alwaysShow)
		.add(Editors.BOOLEAN.withName("Hide effects"), r->r.hideEffects)
		.add(Editors.BOOLEAN.withName("Show Alt description before complete"), r->r.showfdesc)
		.add(Editors.BOOLEAN.withName("Infinite"), r->r.isInfinite())
		.add(Editors.BOOLEAN.withName("Hide this research"), r->r.isHidden)
		.add(Editors.BOOLEAN.withName("Lock this research"), r->r.isInCompletable())
		.apply(Research::new);
		

}
