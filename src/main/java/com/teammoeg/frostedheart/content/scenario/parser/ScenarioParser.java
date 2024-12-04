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

package com.teammoeg.frostedheart.content.scenario.parser;

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
import java.util.function.BiFunction;

import com.teammoeg.frostedheart.content.scenario.ScenarioExecutionException;
import com.teammoeg.frostedheart.content.scenario.parser.reader.CodeLineSource;
import com.teammoeg.frostedheart.content.scenario.parser.reader.ReaderLineSource;
import com.teammoeg.frostedheart.content.scenario.parser.reader.StringLineSource;
import com.teammoeg.frostedheart.content.scenario.parser.reader.StringListStringSource;
import com.teammoeg.frostedheart.content.scenario.parser.reader.StringParseReader;
import com.teammoeg.frostedheart.content.scenario.parser.reader.StringParseReader.ParserState;

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
    private static record ParseResult(List<NodeInfo> node,ParserState state) {
    	
    	
    }
    private NodeInfo createCommand(String command, Map<String, String> params,ParserState state) {
        return new NodeInfo(createCommandNode(command,params),state);
    }
    public static final Map<String,BiFunction<String,Map<String, String>,Node>> nodeFactory=new HashMap<>();
    static {
	    nodeFactory.put("eval",AssignNode::new);
	    nodeFactory.put("if",IfNode::new);
	    nodeFactory.put("elsif",ElsifNode::new);
	    nodeFactory.put("else",ElseNode::new);
	    nodeFactory.put("endif",EndIfNode::new);
	    nodeFactory.put("emb",EmbNode::new);
	    nodeFactory.put("label",LabelNode::new);
	    //nodeFactory.put("p",ParagraphNode::new);
	    nodeFactory.put("save",SavepointNode::new);
	    nodeFactory.put("sharp",ShpNode::new);
	    nodeFactory.put("include",IncludeNode::new);
	    nodeFactory.put("call",CallNode::new);
    }
    private Node createCommandNode(String command, Map<String, String> params) {
        BiFunction<String, Map<String, String>, Node> factory=nodeFactory.get(command);
        if(factory!=null)
        	return factory.apply(command, params);
        return new CommandNode(command, params);

    }
    public Scenario parseString(String name,List<String> code) {
        return process(name,parseLine(new StringListStringSource(name,code)));
    }
    public Scenario parseString(String name,String code){
        return process(name,parseLine(new StringLineSource(name,code)));
    }
    public Scenario parseFile(String name,File file) {
        try (FileInputStream fis = new FileInputStream(file);InputStreamReader isr = new InputStreamReader(fis,StandardCharsets.UTF_8)) {
        	return process(name,parseLine(new ReaderLineSource(name,isr)));

        }catch(IOException ex) {//ignored exception when closed
        }
        return new Scenario(name);
        
    }
    private Scenario process(String name,ParseResult result) {
        LinkedList<CommandStack> ifstack = new LinkedList<>();
        Map<String,Integer> labels=new HashMap<>();
        List<NodeInfo> nodes=result.node();
        int macro=0;
        for (int i = 0; i < nodes.size(); i++) {
        	NodeInfo ni = nodes.get(i);
        	Node n=ni.node();
            if(n instanceof LabelNode ln) {
            	labels.put(ln.name, i);
            }else if (n instanceof IfNode ifn) {
                ifstack.add(new CommandStack(ifn));
            }else if (n instanceof ElsifNode ifn) {
            	if(ifstack.isEmpty())
            		throw ni.state().generateException("Unexpected Elsif!");
                ifstack.peekLast().add(i, ifn);
            } else if (n instanceof ElseNode) {
                ElseNode els = (ElseNode) n;
                if(ifstack.isEmpty())
                	throw ni.state().generateException("Unexpected Else!");
                ifstack.peekLast().setElse(i,els);
            } else if (n instanceof EndIfNode) {
            	if(ifstack.isEmpty())
            		throw ni.state().generateException("Unexpected EndIf!");
                ifstack.pollLast().setEndif(i);
            }else if(n instanceof CommandNode cmd) {
            	if("macro".equals(cmd.command)){
            		macro++;
            	}
            	if("endmacro".equals(cmd.command)){
            		if(macro==0)
            			throw ni.state().generateException("Unexpected EndMacro!");
            		macro--;
            	}
            }else if(n instanceof IncludeNode) {
            	//IncludeNode in=(IncludeNode) n;
            }else if(n instanceof CallNode cn) {
            	//IncludeNode in=(IncludeNode) n;
            	if(cn.name!=null)
            		labels.put(cn.name, i);
            }
        }
        if(!ifstack.isEmpty()) {
        	result.state().generateException("Unclosed if");
        }
        if(macro!=0) {
        	result.state().generateException("Unclosed Macro");
        }
        List<Node> retlist=new ArrayList<>(nodes);
        return new Scenario(name,retlist,labels);
    }
    


    private NodeInfo parseAtCommand(StringParseReader reader) {
        Map<String, String> params = new HashMap<>();
        ParserState state=reader.getCurrentState();
        String command = parseLiteralOrString(reader, -1);
        reader.skipWhitespace();
        //System.out.println("cmd:"+command);
        if(!reader.has()) return createCommand(command, params,state);
        while (reader.has()) {
            String name = parseLiteralOrString(reader, '=');
            reader.skipWhitespace();
            if (!reader.eat('=')) {
                 break;
            }
            reader.skipWhitespace();
            
            String val = parseLiteralOrString(reader, -1);
            params.put(name, val);
            reader.skipWhitespace();
            
            if (!reader.has()||reader.eat('#')) return createCommand(command, params,state);
        }
        return new NodeInfo(new LiteralNode(reader.fromStart()),state);

    }

    private NodeInfo parseBarackCommand(StringParseReader reader) {
        Map<String, String> params = new HashMap<>();
        ParserState state=reader.getCurrentState();
        String command = parseLiteralOrString(reader, ']');
        reader.skipWhitespace();
        
        if(reader.eat(']')) return createCommand(command, params,state);
        while (reader.has()) {
            String name = parseLiteralOrString(reader, '=');
            reader.skipWhitespace();
            if (!reader.eat('=')) {
                break;
            }
            reader.skipWhitespace();
            String val = parseLiteralOrString(reader, ']');
            params.put(name, val);
            reader.skipWhitespace();
            if(reader.eat(']'))return createCommand(command, params,state);
        }
        return new NodeInfo(new LiteralNode(reader.fromStart()),state);
    }

    private ParseResult parseLine(CodeLineSource source) {
        StringParseReader reader = new StringParseReader(source);
        List<NodeInfo> nodes = new ArrayList<>();
        while(reader.nextLine()) {
        	try {
		        while (reader.has()) {
		        	reader.saveIndex();
		        	if(reader.eat('#')) {
		        		break;
		        	}else if (reader.eat('@')) {
		                nodes.add(parseAtCommand(reader));
		            } else if (reader.eat('[')) {
		                nodes.add(parseBarackCommand(reader));
		            }else{
		            	ParserState state=reader.getCurrentState();
		            	String lit=parseLiteral(reader);
		            	if(lit!=null&&!lit.isEmpty()) {
		            		if(lit.startsWith("*")) {
		            			nodes.add(new NodeInfo(new LabelNode(lit),state));
		                	}else
		            		nodes.add(new NodeInfo(new LiteralNode(lit),state));
		            	}
		            }
		        }
        	}catch(Exception ex) {
        		throw reader.getCurrentState().generateException(ex);
        	}
        }
        return new ParseResult(nodes,reader.getCurrentState());
    }

    private String parseLiteral(StringParseReader reader) {
        StringBuilder all = new StringBuilder();
        boolean isEscaping = false;
        while (reader.has()) {
            char r = reader.read();
            if (!isEscaping && r == '\\') {
                isEscaping = true;
                reader.eat();
                continue;
            }
            if (isEscaping) {
                all.append(r);
                isEscaping = false;
                reader.eat();
                continue;
            }
            if (r == '[' || r == '@'||r=='#') {
                break;
            }
            all.append(r);
            reader.eat();
        }
        return all.toString();
    }

    private String parseLiteralOrString(StringParseReader reader, int ch) {
        StringBuilder all = new StringBuilder();
        boolean isEscaping = false;
        boolean hasQuote = false;
        while (reader.has()) {
            char r = reader.read();
            if (!isEscaping && r == '\\') {
                isEscaping = true;
                reader.eat();
                continue;
            }
            if (isEscaping) {
                all.append(r);
                isEscaping = false;
                reader.eat();
                continue;
            }
            if (r == '"') {
                if (!hasQuote) {
                    hasQuote = true;
                } else {
                	reader.eat();
                    break;
                }
                reader.eat();
                continue;
            }
            if (!hasQuote && (r == ch || Character.isWhitespace(r) || r=='#')) {
                break;
            }
            all.append(r);
            reader.eat();
        }
        return all.toString();
    }
    /*public static void main(String[] args) throws IOException {
    	for(Node n:new ScenarioParser().parseFile("prelogue", new File("config\\fhscenario\\prelogue.ks")).pieces)
    		System.out.println(n.getText());
    }*/
}
