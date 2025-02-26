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

package com.teammoeg.frostedheart.content.research.research.clues;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.editor.*;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicative0;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicative6;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicative7;
import com.teammoeg.frostedheart.content.research.research.Research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClueEditors {

    public static final Editor<ItemClue> ITEM =EditorDialogBuilder.create(b->applyBase(b)
    	.add(Editors.SIZED_INGREDIENT.withName("Select Ingredient").forGetter(e->e.stack))
    	.add(Editors.BOOLEAN.withName("Consumes item").forGetter(e->e.consume))
    	.apply(ItemClue::new)
    	);
    
    public static final Editor<CustomClue> CUSTOM = EditorDialogBuilder.create(b->applyBase(b).apply(CustomClue::new));
    
    public static final Editor<AdvancementClue> ADVA = EditorDialogBuilder.create(b->applyListener(b)
 
    	.add(Editors.ADVANCEMENT_CITERION.withName(""/*Unused*/).forGetter(e->{
    	
    		if(e.advancement!=null)
    			return Pair.of(ClientUtils.getPlayer().connection.getAdvancements().getAdvancements().get(e.advancement), e.criterion);
    		return null;
    	}))
    	.apply(AdvancementClue::new));
    
    public static final Editor<KillClue> KILL =  EditorDialogBuilder.create(b->applyListener(b)
    	.add(Editors.openDialogLabeled(Editors.EDITOR_ENTITY,t->CIcons.nop(),t->t.getDescription()).withName("Select Entity").forGetter(e->e.type))
    	.apply(KillClue::new));
    
    
    public static final Editor<MinigameClue> GAME = EditorDialogBuilder.create(b->applyBase(b)
    	.add(Editors.selectBox(Arrays.asList(0, 1, 2, 3)).withName("Level").forGetter(e->e.getLevel()))
    	.apply(MinigameClue::new));
    public static final Editor<List<Clue>> RESEARCH_GAME = (p, l, clues, c) -> {
    	if(clues==null) {
    		clues=new ArrayList<>();
    	}
    	
        MinigameClue ex = null;
        int index=0;
        for (Clue cl : clues) {
            if (cl instanceof MinigameClue) {
                ex = (MinigameClue) cl;
                break;
            }
            index++;
        }
        final List<Clue> fclues=clues;
        final int findex=ex==null?-1:index;
        GAME.open(p, l, ex, o->{if(findex>=0)fclues.set(findex,o);else fclues.add(o);c.accept(fclues);});
    };
    public static final Editor<Clue> EDITOR = new EditorSelector.EditorSelectorBuilder<Clue>()
        .addEditor("Submit Item", ITEM,v->v instanceof ItemClue)
        .addEditor("Triger in program", CUSTOM,v->v instanceof CustomClue)
        .addEditor("Advancement", ADVA,v->v instanceof AdvancementClue)
        .addEditor("Kill Entity", KILL,v->v instanceof KillClue)
        .addEditor("Complete minigame", GAME,v->v instanceof MinigameClue)
        .build();
    public static <T extends Clue> Applicative6<SetterAndGetter<T, ?>, String, String, String, String, Float, Boolean> applyBase(Applicative0<SetterAndGetter<T, ?>> app) {
    	return app.add(Editors.STRING_ID.withName("nonce").forGetter(Clue::getNonce))
        	.add(Editors.STRING.withName("Name").forGetter(e->e.name))
        	.add(Editors.STRING.withName("Description").forGetter(e->e.desc))
        	.add(Editors.STRING.withName("Hint").forGetter(e->e.hint))
        	.add(Editors.FLOAT.withName("Contribution(%)").forGetter(e->e.contribution * 100))
        	.add(Editors.BOOLEAN.withName("Required").forGetter(e->e.required));
    }
    public static <T extends ListenerClue> Applicative7<SetterAndGetter<T, ?>, String, String, String, String, Float, Boolean, Boolean> applyListener(Applicative0<SetterAndGetter<T, ?>> app) {
    	return applyBase(app)
    		.add(Editors.BOOLEAN.withName("Listen even not active").forGetter(e->e.alwaysOn));
    }
    public static final Editor<Collection<Clue>> EDITOR_LIST = (p, l, v, c) -> new EditListDialog<Clue>(p, l, v, EDITOR, e -> Components.str(e.getBrief() + e.getBriefDesc()), c).open();
}
