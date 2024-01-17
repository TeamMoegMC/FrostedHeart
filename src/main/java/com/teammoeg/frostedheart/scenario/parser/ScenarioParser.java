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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.scenario.ScenarioExecutionException;

public class ScenarioParser {
    private static class CommandStack {
        IfNode f;
        List<ElseNode> elses=new ArrayList<>();
        public CommandStack( IfNode f) {
            super();
            this.f=f;
        }

        public void add(int idx, ElsifNode elsif) {
            f.elsifs.put(elsif.exp, idx+1);
            elses.add(elsif);
        }

        public void setElse(int idx,ElseNode els) {
            if(f.elseBlock==-1)
            	f.elseBlock=idx+1;
            elses.add(els);
        }

        public void setEndif(int idx) {
        	if(f.elseBlock==-1)
            	f.elseBlock=idx;
        	elses.forEach(t->t.target=idx);
        }
    }

    private Node createCommand(String command, Map<String, String> params) {
        switch (command) {
            case "eval":
                return new AssignNode(command, params);
            case "if":
            	return new IfNode(command, params);
            case "elsif":
            	return new ElsifNode(command, params);
            case "else":
            	return new ElseNode(command, params);
            case "endif":
                return new EndIfNode(command, params);
            case "emb":
                return new EmbNode(command, params);
            case "label":
                return new LabelNode(command, params);
            case "p":
                return new ParagraphNode(command, params);
            case "save":
            	return new SavepointNode(command, params);
            case "sharp":
            	return new ShpNode(command,params);
            case "include":
            	return new IncludeNode(command,params);
        }
        return new CommandNode(command, params);

    }
    public Scenario parseString(String name,List<String> code) throws IOException {
        List<Node> nodes = new ArrayList<>();
        
        try{
            int i=0;
            for(String line:code) {
            	i++;
            	try {
            		nodes.addAll(parseLine(line));
            	}catch(Exception ex) {
            		throw new ScenarioExecutionException("line "+i+":"+ex.getMessage(),ex);
            	}
            }
        }catch(Exception ex) {
        	throw new ScenarioExecutionException("At file "+name+" "+ex.getMessage(),ex);
        }
        return process(name,nodes);
    }
    public Scenario parseString(String name,String code){
        List<Node> nodes = new ArrayList<>();
        
        try  {
            int i=0;
            for(String line:code.split("[\r\n]")) {
            	i++;
            	try {
            		nodes.addAll(parseLine(line));
            	}catch(Exception ex) {
            		throw new ScenarioExecutionException("line "+i+":"+ex.getMessage(),ex);
            	}
            }
        }catch(Exception ex) {
        	throw new ScenarioExecutionException("At file "+name+" "+ex.getMessage(),ex);
        }
        return process(name,nodes);
    }
    public Scenario parseFile(String name,File file) throws IOException {
        List<Node> nodes = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis,StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            int i=0;
            while ((line = reader.readLine()) != null) {
            	i++;
            	try {
            		nodes.addAll(parseLine(line));
            	}catch(Exception ex) {
            		throw new ScenarioExecutionException("line "+i+":"+ex.getMessage(),ex);
            	}
            }
        }catch(Exception ex) {
        	throw new ScenarioExecutionException("At file "+name+" "+ex.getMessage(),ex);
        }
        return process(name,nodes);
    }
    private Scenario process(String name,List<Node> nodes) {
    	List<Integer> paragraphs = new ArrayList<>();
        LinkedList<CommandStack> ifstack = new LinkedList<>();
        Map<String,Integer> labels=new HashMap<>();
        int macro=0;
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n instanceof ParagraphNode) {
                paragraphs.add(i);
                ((ParagraphNode) n).nodeNum=paragraphs.size();
            }else if(n instanceof LabelNode) {
            	labels.put(((LabelNode)n).name, i);
            }else if (n instanceof IfNode) {
                IfNode ifn = (IfNode) n;
                ifstack.add(new CommandStack(ifn));
            }else if (n instanceof ElsifNode) {
            	ElsifNode ifn = (ElsifNode) n;
            	if(ifstack.isEmpty())
            		throw new ScenarioExecutionException("At file "+name+" Unexpected Elsif!");
                ifstack.peekLast().add(i, ifn);
            } else if (n instanceof ElseNode) {
                ElseNode els = (ElseNode) n;
                if(ifstack.isEmpty())
            		throw new ScenarioExecutionException("At file "+name+" Unexpected Else!");
                ifstack.peekLast().setElse(i,els);
            } else if (n instanceof EndIfNode) {
            	if(ifstack.isEmpty())
            		throw new ScenarioExecutionException("At file "+name+" Unexpected Endif!");
                ifstack.pollLast().setEndif(i);
            }else if(n instanceof CommandNode) {
            	CommandNode cmd=(CommandNode) n;
            	if("macro".equals(cmd.command)){
            		macro++;
            	}
            	if("endmacro".equals(cmd.command)){
            		macro--;
            	}
            }else if(n instanceof IncludeNode) {
            	//IncludeNode in=(IncludeNode) n;
            }
        }
        if(!ifstack.isEmpty()) {
        	throw new ScenarioExecutionException("At file "+name+" could not find endif for if!");
        }
        if(macro!=0) {
        	throw new ScenarioExecutionException("At file "+name+" macro and endmacro not match! "+Math.abs(macro)+" more "+(macro>0?"macro(s)":"endmacro(s)"));
        }
        return new Scenario(name, nodes,paragraphs.stream().mapToInt(t->t).toArray(),labels);
    }
    


    private Node parseAtCommand(StringParseReader reader) throws IOException {
        Map<String, String> params = new HashMap<>();
        reader.saveIndex();
        String command = parseLiteralOrString(reader, -1);
        reader.skipWhitespace();
        if(!reader.hasNext()) return createCommand(command, params);
        while (reader.hasNext()) {
            String name = parseLiteralOrString(reader, '=');
            if (reader.last() != '=') {
                reader.skipWhitespace();
                if (reader.last() != '=')
                    break;
            }
            reader.skipWhitespace();
            String val = parseLiteralOrString(reader, -1);
            params.put(name, val);
            reader.skipWhitespace();
            if (!reader.hasNext()||reader.eat('#')) return createCommand(command, params);
        }
        return new LiteralNode(reader.fromStart());

    }

    private Node parseBarackCommand(StringParseReader reader) throws IOException {
        Map<String, String> params = new HashMap<>();
        reader.saveIndex();
        String command = parseLiteralOrString(reader, ']');
        reader.skipWhitespace();
        if(reader.peekLast()==']') return createCommand(command, params);
        while (reader.hasNext()) {
            String name = parseLiteralOrString(reader, '=');
            if (reader.last() != '=') {
                reader.skipWhitespace();
                if (reader.last() != '=')
                    break;
            }
            reader.skipWhitespace();
            String val = parseLiteralOrString(reader, ']');
            params.put(name, val);
            reader.skipWhitespace();
            if (reader.last() == ']') return createCommand(command, params);
        }
        return new LiteralNode(reader.fromStart());
    }

    private List<Node> parseLine(String line) throws IOException {
        StringParseReader reader = new StringParseReader(line);
        List<Node> nodes = new ArrayList<>();
        while (reader.hasNext()) {
        	if(reader.peekLast()=='#') {
        		break;
        	}else
            if (reader.peekLast() == '@') {
            	if(reader.isBegin())reader.next();
                nodes.add(parseAtCommand(reader));
            } else if (reader.peekLast() == '[') {
            	if(reader.isBegin())reader.next();
                nodes.add(parseBarackCommand(reader));
            } else {
            	String lit=parseLiteral(reader);
            	if(lit!=null&&!lit.isEmpty())
                nodes.add(new LiteralNode(lit));
            }
        }
        return nodes;
    }

    private String parseLiteral(StringParseReader reader) throws IOException {
        StringBuilder all = new StringBuilder();
        boolean isEscaping = false;
        while (reader.hasNext()) {
            char r = reader.next();
            if (!isEscaping && r == '\\') {
                isEscaping = true;
                continue;
            }
            if (isEscaping) {
                all.append(r);
                isEscaping = false;
                continue;
            }
            if (r == '[' || r == '@'||r=='#') {
                break;
            }
            all.append(r);
        }
        return all.toString();
    }

    private String parseLiteralOrString(StringParseReader reader, int ch) throws IOException {
        StringBuilder all = new StringBuilder();
        boolean isEscaping = false;
        boolean hasQuote = false;
        while (reader.hasNext()) {
            char r = reader.next();
            if (!isEscaping && r == '\\') {
                isEscaping = true;
                continue;
            }
            if (isEscaping) {
                all.append(r);
                isEscaping = false;
                continue;
            }
            if (r == '"') {
                if (!hasQuote) {
                    hasQuote = true;
                } else {
                    break;
                }
                continue;
            }
            if (!hasQuote && (r == ch || Character.isWhitespace(r))) {
                break;
            }
            all.append(r);
        }
        return all.toString();
    }
}
