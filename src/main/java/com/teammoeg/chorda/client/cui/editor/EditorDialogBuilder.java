package com.teammoeg.chorda.client.cui.editor;

import java.util.function.Function;

import com.teammoeg.chorda.client.cui.editor.EditorDialog.EditorDialogPrototype;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicatable;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicative0;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuildResult;

public class EditorDialogBuilder {

	private EditorDialogBuilder() {

	}
	public static record SetterAndGetter<O, A>(EditorItemFactory<A> factory, Function<O, A> func) implements Applicatable<SetterAndGetter<O, ?>, A> {

	}

	public static <O> Editor<O> create(Function<Applicative0<SetterAndGetter<O, ?>>, BuildResult<SetterAndGetter<O, ?>, O>> builder) {
		EditorDialogPrototype<O> proto=new EditorDialogPrototype<>(CurryApplicativeTemplate.build(builder));
		return (p,l,v,c)->proto.create(p, l, v, c).open();
	}
	public static <O> Applicative0<SetterAndGetter<O, ?>> startBuilder(){
		return Applicative0.getInstance();
	}
}
