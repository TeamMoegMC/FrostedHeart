/*
 * Copyright (c) 2026 TeamMoeg
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
