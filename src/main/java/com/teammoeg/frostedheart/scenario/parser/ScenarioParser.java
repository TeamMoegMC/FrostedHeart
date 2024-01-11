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
			if(!reader.hasNext())return new CommandNode(command,params);
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
			if(reader.last()==']')return new CommandNode(command,params);
		}
		return new LiteralNode(reader.fromStart());
	}
	public String parseLiteral(StringParseReader reader) throws IOException {
		StringBuilder all=new StringBuilder();
		boolean isEscaping=false;
		while(reader.hasNext()) {
			int r=reader.next();
			if(!isEscaping&&r=='\\') {
				isEscaping=true;
				continue;
			}
			if(isEscaping) {
				all.append(Character.toString(r));
				isEscaping=false;
				continue;
			}
			if(r=='['||r=='@') {
				break;
			}
			all.append(Character.toString(r));
		}
		return all.toString();
	}
	public String parseLiteralOrString(StringParseReader reader,int ch) throws IOException {
		StringBuilder all=new StringBuilder();
		boolean isEscaping=false;
		boolean hasQuote=false;
		while(reader.hasNext()) {
			int r=reader.next();
			if(!isEscaping&&r=='\\') {
				isEscaping=true;
				continue;
			}
			if(isEscaping) {
				all.append(Character.toString(r));
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
			all.append(Character.toString(r));
		}
		return all.toString();
	}
}
