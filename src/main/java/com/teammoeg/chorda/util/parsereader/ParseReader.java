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
 * 统一的DSL解析读取器框架，包含最常用的读取函数。
 * 提供逐字符读取、跳过空白、保存/恢复位置等解析原语。
 * <p>
 * A unified reader framework for DSL parsing, containing the most commonly used
 * reader functions. Provides character-by-character reading, whitespace skipping,
 * save/restore position and other parsing primitives.
 */
public class ParseReader {
	/**
	 * 解析器状态记录，包含文件名、行号和位置，用于生成调试异常。
	 * <p>
	 * Parser state record containing filename, line number and position, used for generating debug exceptions.
	 */
	public static record ParserState(String name,int lineNum,int position){
	    /**
	     * 从嵌套异常生成带位置信息的解析异常。
	     * <p>
	     * Generate a parse exception with position info from a nested exception.
	     *
	     * @param nested 嵌套异常 / the nested exception
	     * @return 解析异常 / the parse reader exception
	     */
	    public ParseReaderException generateException(Throwable nested) {
	    	return new ParseReaderException("At File: "+name+" Line:"+lineNum+":"+position+" "+nested.getMessage(),nested);
	    }
	    /**
	     * 从消息字符串生成带位置信息的解析异常。
	     * <p>
	     * Generate a parse exception with position info from a message string.
	     *
	     * @param message 错误消息 / the error message
	     * @return 解析异常 / the parse reader exception
	     */
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
    /**
     * 构造解析读取器。
     * <p>
     * Construct a parse reader.
     *
     * @param str 代码行数据源 / the code line source
     */
    public ParseReader(CodeLineSource str) {
        super();
        this.strs = str;
    }
    /**
     * 读取并跳到下一行，丢弃当前行。如果有新行被填充到缓存则返回true。
     * <p>
     * Read and go to the next line, dispose of the current line.
     * Returns true if a new line is populated to the cache.
     *
     * @return 是否成功读取到新行 / whether a new line was successfully read
     */
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
     * 返回从保存索引（包含）到当前位置（不包含）之间的字符串。
     * <p>
     * Return a string with characters from saved index (included) to current position (excluded).
     *
     * @return 子字符串 / the substring
     */
    public String fromStart() {
        return str.substring(srecord, idx);
    }
    /**
     * 当前行是否还有字符可读。
     * <p>
     * Return true if there is any character remaining in the current line.
     *
     * @return 是否还有字符 / whether there are remaining characters
     */
    public boolean has() {
        return str!=null&&idx < str.length();
    }
    /**
     * 返回当前位置的字符（不移动位置）。
     * <p>
     * Return the character at the current position (without advancing).
     *
     * @return 当前字符 / the current character
     */
    public char read() {
        return str.charAt(idx);
    }
    /**
     * 从当前位置读取指定数量的字符并前进。
     * <p>
     * Read the specified number of characters from the current position and advance.
     *
     * @param num 要读取的字符数 / the number of characters to read
     * @return 读取的字符串 / the read string
     */
    public String read(int num) {
        String text=str.substring(idx,idx+num);
        idx+=num;
        return text;
    }
    /**
     * 返回当前字符并跳过（前进一位）。
     * <p>
     * Return the current character and skip it (advance by one).
     *
     * @return 当前字符 / the current character
     */
    public char eat() {
    	cache=null;
        return str.charAt(idx++);
    }
    /**
     * 如果当前字符匹配给定字符则跳过，并返回是否已跳过。
     * <p>
     * Skip the current character if it matches the given character, returning true if skipped.
     *
     * @param ch 要匹配的字符 / the character to match
     * @return 是否已跳过 / whether the character was skipped
     */
    public boolean eat(char ch) {
        if (read() == ch) {
            eat();
            return true;
        }
        return false;
    }

    /**
     * 保存当前索引以供后续使用。
     * <p>
     * Record the current index for later use.
     */
    public void saveIndex() {
        srecord = idx;
    }
    /**
     * 回退到保存的索引位置。
     * <p>
     * Go back to the saved index position.
     */
    public void restoreIndex() {
        idx=srecord;
    }
    /**
     * 跳过空白字符，直到当前字符不是空白。
     * <p>
     * Skip whitespace characters until the current character is not whitespace.
     */
    public void skipWhitespace() {
    	cache=null;
        while (has()&&Character.isWhitespace(read())) {
            idx++;
        }
    }
    /**
     * 获取当前解析位置用于调试，包含文件名、行号和字符位置。
     * <p>
     * Get the current parsing position for debug, including file name, line number and character position.
     *
     * @return 当前解析器状态 / the current parser state
     */
    public ParserState getCurrentState() {
    	if(cache==null)
    		cache=new ParserState(strs.getName(),lineNo,idx);
    	return cache;
    }

}
