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

package com.teammoeg.frostedheart.content.scenario;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.joml.Vector3f;

import com.teammoeg.chorda.math.Point;
import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;

public class ScenarioExecutor<T> {
    static Marker MARKER = MarkerManager.getMarker("Scenario Executor");
    private static class MethodInfo<T> implements ScenarioMethod<T> {
        private static class ParamInfo {
            String[] paramName;
            TypeAdapter convertion;
            Supplier<Object> def=null;
            public ParamInfo(String[] paramName, TypeAdapter convertion) {
                this.paramName = paramName;
                this.convertion = convertion;
            }

			@Override
			public String toString() {
				return "[name=" + Arrays.toString(paramName) + "]";
			}


        }

        Method method;

        Object instance;
        ParamInfo[] params;
        public MethodInfo(Object instance, Method method,ScenarioExecutor<T> parent) {
            this.instance = instance;
            this.method = method;
            Parameter[] param = method.getParameters();
            params = new ParamInfo[param.length - 1];

            for (int i = 1; i < param.length; i++) {
            	TypeAdapter converter = null;
                Class<?> partype = param[i].getType();
                Param[] par=param[i].getAnnotationsByType(Param.class);
                int size=0;
                if(par!=null) {
                	size=par.length;
                }
                String[] names=new String[size];
                //names[0]=(param[i].isNamePresent()?param[i].getName():param[i].get);
                if(par!=null) {
                	for(int j=0;j<size;j++) {
                		names[j]=par[j].value();
                	}
                }
                Supplier<Object> def=null;
                if (partype.isAssignableFrom(Double.class) || partype == double.class) {
                    converter = number;
                    if(partype.isPrimitive())
                    	def=()->0d;
                } else if (partype.isAssignableFrom(String.class)) {
                    converter = null;
                } else if (partype.isAssignableFrom(Integer.class) || partype == int.class) {
                    converter = integer;
                    if(partype.isPrimitive())
                    	def=()->0;
                } else if (partype.isAssignableFrom(Float.class) || partype == float.class) {
                    converter = fnumber;
                    if(partype.isPrimitive())
                    	def=()->0f;
                } else if (partype.isAssignableFrom(Boolean.class)|| partype==boolean.class) {
                	converter=bo;
                	if(partype.isPrimitive())
                    	def=()->false;
                } else if(parent.types.containsKey(partype)){
                	converter=parent.types.get(partype);
                }else {
                    throw new ScenarioExecutionException("No matching type found for param " + Arrays.toString(names) + " of " + method.getName());
                }
                params[i - 1] = new ParamInfo(names, converter);
                params[i - 1].def=def;
            }
            //System.out.println(toString());
        }

        @Override
		public String toString() {
			return "[method=" +method.getDeclaringClass().getSimpleName()+"." + method.getName() + ", params=" + Arrays.toString(params) + "]";
		}

		@Override
        public void execute(T runner, Map<String, String> param) {
            Object[] pars = new Object[params.length + 1];
            for (int i = 0; i < params.length; i++) {
            	Object par=null;
            	
        		if(params[i].convertion==null) {
        			par=getFirstExists(param,params[i].paramName);
            	}else {
            		try {
                		par=params[i].convertion.convert(runner,params[i].paramName,param);
                	} catch (NumberFormatException | ClassCastException ex) {
                        throw new ScenarioExecutionException("Exception converting param " + Arrays.toString(params[i].paramName), ex);
                    }
            	}
            	
                if (par != null) {
                	pars[i+1]=par;
                }else {
                	if(params[i].def!=null)
                		pars[i+1] =params[i].def.get();
                }
            }
            pars[0] = runner;
            try {
                method.invoke(instance, pars);
            } catch (IllegalArgumentException | IllegalAccessException e) {
            	e.printStackTrace();
                throw new ScenarioExecutionException(e);
            } catch (InvocationTargetException e) {
                throw new ScenarioExecutionException(e.getTargetException());
            }
        }
    }
    @FunctionalInterface
    public interface ScenarioMethod<T> {
        void execute(T scenarioVM, Map<String, String> param);
    }
    Class<T> objcls;
    public ScenarioExecutor(Class<T> objcls) {
		super();
		this.objcls = objcls;
	}
    private static TypeAdapter<?,Object> number = (r,n,p) ->{
    	String s=getFirstExists(p,n);
    	if(s==null||s.isEmpty())return s;
    	return ((Double) Double.parseDouble(s));
    	
    };
    private static TypeAdapter<?,Object> integer = (r,n,p) ->{ 
    	String s=getFirstExists(p,n);
    	if(s==null||s.isEmpty())return s;
    	if(s.toLowerCase().startsWith("0x"))return (int)(Long.parseLong(s.substring(2),16));
    	return ((Double) Double.parseDouble(s)).intValue();
    	};
	private static TypeAdapter<?, Object> bo = (r,n,p) ->{ 
    	String s=getFirstExists(p,n);
    	if(s==null||s.isEmpty())return s;
    	if(s.toLowerCase().startsWith("0x"))return (int)(Long.parseLong(s.substring(2),16));
    	return ((Double) Double.parseDouble(s)).intValue()>0;
    	};

    private static TypeAdapter<?,Object> fnumber = (r,n,p) ->{
    	String s=getFirstExists(p,n);
    	if(s==null||s.isEmpty())return s;
    	return ((Double) Double.parseDouble(s)).floatValue();
    	
    } ;
    Map<Class<?>,TypeAdapter<?,T>> types=new HashMap<>();
    public <V> void addTypeAdapter(Class<? super V> cls,TypeAdapter<V,T> conv) {
    	types.put(cls, conv);
    }
    {
    	addTypeAdapter(BlockPos.class,(r,n,p)->new BlockPos(
    		castParamType(r,p,int.class,s->s+"x",0,n),
    		castParamType(r,p,int.class,s->s+"y",0,n),
    		castParamType(r,p,int.class,s->s+"z",0,n)
    		));
    	addTypeAdapter(Vec3i.class,(r,n,p)->new Vec3i(
    		castParamType(r,p,int.class,s->s+"x",0,n),
    		castParamType(r,p,int.class,s->s+"y",0,n),
    		castParamType(r,p,int.class,s->s+"z",0,n)
    		));
    	addTypeAdapter(Vector3f.class,(r,n,p)->new Vector3f(
    		castParamType(r,p,float.class,s->s+"x",0f,n),
    		castParamType(r,p,float.class,s->s+"y",0f,n),
    		castParamType(r,p,float.class,s->s+"z",0f,n)
    		));
    	addTypeAdapter(Vec3.class,(r,n,p)->new Vec3(
    		castParamType(r,p,double.class,s->s+"x",0d,n),
    		castParamType(r,p,double.class,s->s+"y",0d,n),
    		castParamType(r,p,double.class,s->s+"z",0d,n)
    		));
    	addTypeAdapter(Rect.class,(r,n,p)->new Rect(
    		castParamType(r,p,int.class,s->s+"x",0,n),
    		castParamType(r,p,int.class,s->s+"y",0,n),
    		castParamType(r,p,int.class,s->s+"w",-1,n),
    		castParamType(r,p,int.class,s->s+"h",-1,n)
    		));
    	addTypeAdapter(Point.class,(r,n,p)->new Point(
    		castParamType(r,p,int.class,s->s+"x",0,n),
    		castParamType(r,p,int.class,s->s+"y",0,n)
    		));
    }

    Map<String, ScenarioMethod<T>> commands = new HashMap<>();
    public void callCommand(String name, T scenarioVM, Map<String, String> params) {
        ScenarioMethod<T> command = commands.get(name);
        if (command == null) {
            throw new CommandNotFoundException("Can not find command " + name);
        }
        command.execute(scenarioVM, params);
    }
    public static String getFirstExists(Map<String,String> params,String... pnames) {
		for(String name:pnames) {
			if(params.containsKey(name)) {
				return params.get(name);
			}
		}
		return null;
    }
    public <V> V castParamType(T runner,Map<String,String> params,Class<V> partype,String... pnames) {
    	return castParamType(runner,params,partype,null,pnames);
    }
    public <V> V castParamType(T runner,Map<String,String> params,Class<V> partype,Function<String,String> apply,V defval,String... pnames) {
    	String[] ss=Arrays.copyOf(pnames, pnames.length);
    	for(int i=0;i<ss.length;i++)
    		ss[i]=apply.apply(ss[i]);
    	return castParamType(runner,params,partype,defval,ss);
    }
    
    public <V> V castParamType(T runner,Map<String,String> params,Class<V> partype,V defval,String... pnames) {
    	TypeAdapter<?,T> ta=types.get(partype);
		Object result=null;
    	
        if (partype.isAssignableFrom(Double.class) || partype == double.class) {
            result= number.convert(runner, pnames, params);
        } else if (partype.isAssignableFrom(String.class)) {
        	result=getFirstExists(params,pnames);
        } else if (partype.isAssignableFrom(Integer.class) || partype == int.class) {
            result= integer.convert(runner, pnames, params);
        } else if (partype.isAssignableFrom(Float.class) || partype == float.class) {
            result= fnumber.convert(runner, pnames, params);
        }else if(ta!=null) {
			result= ta.convert(runner, pnames, params);
        }else throw new ScenarioExecutionException("No matching type found for param " + Arrays.toString(pnames));
		if(result!=null)
			return (V) result;
    	
    	if(defval!=null)
    		return defval;
    	if(partype.isPrimitive()) {
    		if (partype == double.class) {
	            result= 0d;
	        } else if (partype == int.class) {
	            result= 0;
	        } else if (partype == float.class) {
	            result= 0f;
	        }
    	}
    	return (V) result;
    }
    public void register(Class<?> clazz) {
        try {
            Constructor<?> ctor = clazz.getConstructor();
            ctor.setAccessible(true);
            registerInst(ctor.newInstance());
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
                 IllegalArgumentException e) {
            FHMain.LOGGER.error(MARKER, "Error registering scenario class",e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error registering scenario class" + e.getTargetException());
        }

    }

    public void registerCommand(String cmdName, ScenarioMethod<T> method) {
        commands.put(cmdName.toLowerCase(), method);
    }

    public void registerInst(Object clazz) {
        for (Method met : clazz.getClass().getMethods()) {
            if (Modifier.isPublic(met.getModifiers())) {
                try {
                	if(met.getParameterCount()>0&&met.getParameters()[0].getType().isAssignableFrom(objcls))
                		registerCommand(met.getName(), new MethodInfo<>(Modifier.isStatic(met.getModifiers()) ? null : clazz, met, this));
                } catch (ScenarioExecutionException ex) {
                    ex.printStackTrace();
                    FHMain.LOGGER.warn(MARKER, ex.getMessage());
                }
            }
        }
    }

    public void registerStatic(Class<?> clazz) {
        for (Method met : clazz.getMethods()) {
            if (Modifier.isPublic(met.getModifiers()) && Modifier.isStatic(met.getModifiers())) {
                try {
                    registerCommand(met.getName(), new MethodInfo(null, met,this));
                } catch (ScenarioExecutionException ex) {

                    ex.printStackTrace();
                    FHMain.LOGGER.warn(MARKER, ex.getMessage());
                }
            }
        }
    }
    static class Test{
    	public void test(ScenarioConductor sr,@Param("s")String s,@Param("s")Rect r,@Param("")Rect r1) {
    		FHMain.LOGGER.error(s+":"+r+":"+r1);
    	}
    }
    public static void main(String[] args) throws SecurityException {
    	ScenarioExecutor<Object> exc= new ScenarioExecutor<>(Object.class);
    	exc.registerInst(new Test());
    	Map<String,String> mp=new HashMap<>();
    	mp.put("s", "twr_scenario:twr_logo_title.png");
    	mp.put("sx", "123");
    	mp.put("sy", "234");
    	mp.put("sw", "345");
    	mp.put("sh", "456");
    	mp.put("x", "999");
    	mp.put("y", "888");
    	mp.put("w", "777");
    	mp.put("h", "666");
    	//mp.put("z", "60");
    	exc.callCommand("test", null, mp);
    }
}
