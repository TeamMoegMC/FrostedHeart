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

package com.teammoeg.chorda.util.parsereader;
/**
 * A unified reader framework for dsl parsing, contains most used reader functions
 * 
 * */
public class ParseReader {
	public static record ParserState(String name,int lineNum,int position){
	    public ParseReaderException generateException(Throwable nested) {
	    	return new ParseReaderException("At File: "+name+" Line:"+lineNum+":"+position+" "+nested.getMessage(),nested);
	    }
	    public ParseReaderException generateException(String message) {
	    	return new ParseReaderException("At File: "+name+" Line:"+lineNum+":"+position+" "+message);
	    }
	}
    public final CodeLineSource strs;
    private String str;
    private int idx = 0;
    private int srecord = 0;
    private int lineNo=0;
    private ParserState cache;
    public ParseReader(CodeLineSource str) {
        super();
        this.strs = str;
    }
    /**
     * read and go to next line, dispose current line, return true if a new line is populated to the cache.
     * */
    public boolean nextLine(){
    	cache=null;
    	str=null;
    	idx=0;
		srecord=0;
    	if(strs.hasNext()) {
    		lineNo++;
    		str=strs.readLine();
    		return true;
    	}
    	return false;
    }
    /**
     * Return a string with characters from saved index(included) to current position(excluded)
     * */
    public String fromStart() {
        return str.substring(srecord, idx);
    }
    /**
     * Return true if there's any character in the current line
     * */
    public boolean has() {
        return str!=null&&idx < str.length();
    }
    /**
     * Return current character
     * */
    public char read() {
        return str.charAt(idx);
    }
    /**
     * Return current character
     * */
    public String read(int num) {
        String text=str.substring(idx,idx+num);
        idx+=num;
        return text;
    }
    /**
     * return and skip current character
     * */
    public char eat() {
    	cache=null;
        return str.charAt(idx++);
    }
    /**
     * skip current character if the character matches given and return true if skipped
     * 
     * */
    public boolean eat(char ch) {
        if (read() == ch) {
            eat();
            return true;
        }
        return false;
    }

    /**
     * record current index for later use.
     * */
    public void saveIndex() {
        srecord = idx;
    }
    /**
     * go back to saved index
     * */
    public void restoreIndex() {
        idx=srecord;
    }
    /**
     * Skip character until current character is not space.
     * 
     * */
    public void skipWhitespace() {
    	cache=null;
        while (has()&&Character.isWhitespace(read())) {
            idx++;
        }
    }
    /**
     * get current position for debug, including file name, line number, position.
     * */
    public ParserState getCurrentState() {
    	if(cache==null)
    		cache=new ParserState(strs.getName(),lineNo,idx);
    	return cache;
    }

}
