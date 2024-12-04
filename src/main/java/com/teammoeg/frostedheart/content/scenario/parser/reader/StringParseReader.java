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

package com.teammoeg.frostedheart.content.scenario.parser.reader;

import com.teammoeg.frostedheart.content.scenario.parser.ScenarioParseException;

public class StringParseReader {
	public static record ParserState(String name,int lineNum,int position){
	    public ScenarioParseException generateException(Throwable nested) {
	    	return new ScenarioParseException("At File: "+name+" Line:"+lineNum+":"+position+" "+nested.getMessage(),nested);
	    }
	    public ScenarioParseException generateException(String message) {
	    	return new ScenarioParseException("At File: "+name+" Line:"+lineNum+":"+position+" "+message);
	    }
	}
    public final CodeLineSource strs;
    private String str;
    private int idx = 0;
    private int srecord = 0;
    private int lineNo=0;
    private ParserState cache;
    public StringParseReader(CodeLineSource str) {
        super();
        this.strs = str;
    }
    public boolean nextLine(){
    	cache=null;
    	str=null;
    	idx=0;
		srecord=0;
    	if(strs.hasNext()) {
    		lineNo++;
    		str=strs.read();
    		return true;
    	}
    	return false;
    }
    public String fromStart() {
        return str.substring(srecord, idx);
    }
    public boolean has() {
        return str!=null&&idx < str.length();
    }

    public char read() {
        return str.charAt(idx);
    }

    public char eat() {
    	cache=null;
        return str.charAt(idx++);
    }

    public boolean eat(char ch) {
        if (read() == ch) {
            eat();
            return true;
        }
        return false;
    }


    public void saveIndex() {
        srecord = idx;
    }

    public void skipWhitespace() {
    	cache=null;
        while (has()&&Character.isWhitespace(read())) {
            idx++;
        }
    }
    public ParserState getCurrentState() {
    	if(cache==null)
    		cache=new ParserState(strs.getName(),lineNo,idx);
    	return cache;
    }

}
