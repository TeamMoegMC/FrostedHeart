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

package com.teammoeg.frostedheart.scenario.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScenarioParser {
	private static class CommandStack{
		int from;
		List<IfNode> allNodes=new ArrayList<>();
		public CommandStack(int from,IfNode f) {
			super();
			this.from = from;
			allNodes.add(f);
		}
		public void add(int idx,IfNode elsif) {
			allNodes.get(allNodes.size()-1).elseBlock=idx;
			elsif.parentBlock=from;
			allNodes.add(elsif);
		}
		public void addElse(int idx) {
			if(allNodes.get(allNodes.size()-1).elseBlock!=-1) {
				allNodes.get(allNodes.size()-1).elseBlock=idx;
			}
			throw new IllegalArgumentException("Illegal else block at node "+idx);
		}
		public void addEndif(int idx) {
			for(IfNode ifn:allNodes) {
				ifn.endBlock=idx;
			}
		}
	}
	public ScenarioPiece parse(File file) throws IOException {
		List<Node> nodes=new ArrayList<>();
		try (FileInputStream fis=new FileInputStream(file);
				InputStreamReader isr=new InputStreamReader(fis);
				BufferedReader reader=new BufferedReader(isr)) {
			String line;
			while((line=reader.readLine())!=null) {
				nodes.addAll(parseLine(line));
			}
		}
		List<Integer> paragraphs=new ArrayList<>();
		List<CommandStack> ifstack=new ArrayList<>();
		for(int i=0;i<nodes.size();i++) {
			Node n=nodes.get(i);
			if(n instanceof ParagraphNode) {
				paragraphs.add(i);
			}
			if(n instanceof IfNode) {
				IfNode ifn=(IfNode) n;
				if(ifn.cmd.equals("if")) {
					ifstack.add(new CommandStack(i,ifn));
				}else {
					ifstack.get(ifstack.size()-1).add(i, ifn);
				}
			}else if(n instanceof ElsEndifNode) {
				ElsEndifNode els=(ElsEndifNode) n;
				if(els.command.equals("else")) {
					ifstack.get(ifstack.size()-1).addElse(i);
				}else {
					ifstack.remove(ifstack.size()-1).addEndif(i);
				}
			}
		}
		return new ScenarioPiece(file.getName(),nodes);
	}
	public List<Node> parseLine(String line) throws IOException {
		StringParseReader reader=new StringParseReader(line);
		List<Node> nodes=new ArrayList<>();
		while(reader.hasNext()) {
			if(reader.peekLast()=='@') {
				nodes.add(parseAtCommand(reader));
			}else if(reader.peekLast()=='[') {
				nodes.add(parseBarackCommand(reader));
			}else {
				nodes.add(new LiteralNode(parseLiteral(reader)));	
			}			
		}
		return nodes;
	}
	public Node parseAtCommand(StringParseReader reader) throws IOException {
		Map<String,String> params=new HashMap<>();
		reader.saveIndex();
		String command=parseLiteralOrString(reader,-1);
		
		while(reader.hasNext()) {
			String name=parseLiteralOrString(reader,'=');
			if(reader.last()!='=') {
				reader.skipWhitespace();
				if(reader.last()!='=')
					break;
			}
			reader.skipWhitespace();
			String val=parseLiteralOrString(reader,-1);
			params.put(name, val);
			reader.skipWhitespace();
			if(!reader.hasNext())return createCommand(command,params);
		}
		return new LiteralNode(reader.fromStart());
		
	}
	public Node parseBarackCommand(StringParseReader reader) throws IOException {
		Map<String,String> params=new HashMap<>();
		reader.saveIndex();
		String command=parseLiteralOrString(reader,-1);
		while(reader.hasNext()) {
			String name=parseLiteralOrString(reader,'=');
			if(reader.last()!='=') {
				reader.skipWhitespace();
				if(reader.last()!='=')
					break;
			}
			reader.skipWhitespace();
			String val=parseLiteralOrString(reader,']');
			params.put(name, val);
			reader.skipWhitespace();
			if(reader.last()==']')return createCommand(command,params);
		}
		return new LiteralNode(reader.fromStart());
	}
	private Node createCommand(String command,Map<String,String> params) {
		switch(command) {
		case "eval":return new AssignNode(command,params);
		case "if":
		case "elsif":return new IfNode(command,params);
		case "else":
		case "endif":return new ElsEndifNode(command,params);
		case "emb":return new EmbNode(command,params);
		case "label":return new LabelNode(command,params);
		case "p":return new ParagraphNode(command,params);
		}
		return new CommandNode(command,params);
		
	}
	public String parseLiteral(StringParseReader reader) throws IOException {
		StringBuilder all=new StringBuilder();
		boolean isEscaping=false;
		while(reader.hasNext()) {
			char r=reader.next();
			if(!isEscaping&&r=='\\') {
				isEscaping=true;
				continue;
			}
			if(isEscaping) {
				all.append(r);
				isEscaping=false;
				continue;
			}
			if(r=='['||r=='@') {
				break;
			}
			all.append(r);
		}
		return all.toString();
	}
	public String parseLiteralOrString(StringParseReader reader,int ch) throws IOException {
		StringBuilder all=new StringBuilder();
		boolean isEscaping=false;
		boolean hasQuote=false;
		while(reader.hasNext()) {
			char r=reader.next();
			if(!isEscaping&&r=='\\') {
				isEscaping=true;
				continue;
			}
			if(isEscaping) {
				all.append(r);
				isEscaping=false;
				continue;
			}
			if(r=='"') {
				if(!hasQuote) {
					hasQuote=true;
				}else {
					break;
				}
				continue;
			}
			if(!hasQuote&&(r==ch||Character.isWhitespace(r))) {
				break;
			}
			all.append(r);
		}
		return all.toString();
	}
}
