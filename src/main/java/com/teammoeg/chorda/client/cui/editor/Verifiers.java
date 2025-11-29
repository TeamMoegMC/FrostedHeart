package com.teammoeg.chorda.client.cui.editor;

public class Verifiers {
	public static final Verifier<String> COMMAND=Verifier.successOrTranslatable(s->s.startsWith("/"), "gui.chorda.editor.command_must_starts_with_slash");
	public static final Verifier<String> NUMBER_STR=Verifier.successOrTranslatable(s->{try{
		Double.parseDouble(s);return true;
	}catch(Throwable t) {return false;}}, "gui.chorda.editor.wrong_number");
	public static final Verifier<String> INT_STR=Verifier.successOrTranslatable(s->{try{
		Integer.parseInt(s);return true;
	}catch(Throwable t) {return false;}}, "gui.chorda.editor.wrong_number");
	public static final Verifier<String> LONG_STR=Verifier.successOrTranslatable(s->{try{
		Long.parseLong(s);return true;
	}catch(Throwable t) {return false;}}, "gui.chorda.editor.wrong_number");
	private Verifiers() {
	}

}
