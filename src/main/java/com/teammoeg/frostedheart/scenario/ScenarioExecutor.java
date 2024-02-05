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

package com.teammoeg.frostedheart.scenario;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class ScenarioExecutor<T> {
    private static class MethodInfo<T> implements ScenarioMethod<T> {
        private static class ParamInfo {
            String[] paramName;
            Function<String, Object> convertion;
            Supplier<Object> def=null;
            public ParamInfo(String[] paramName, Function<String, Object> convertion) {
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
        public MethodInfo(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
            Parameter[] param = method.getParameters();
            params = new ParamInfo[param.length - 1];

            for (int i = 1; i < param.length; i++) {
                Function<String, Object> converter = null;
                Class<?> partype = param[i].getType();
                Param[] par=param[i].getAnnotationsByType(Param.class);
                int size=0;
                if(par!=null) {
                	size=par.length;
                }
                String[] names=new String[size+1];
                names[0]=(param[i].isNamePresent()?param[i].getName():param[i].getName().substring(4));
                if(par!=null) {
                	for(int j=0;j<size;j++) {
                		names[j+1]=par[j].value();
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
                } else {
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
            	String par=null;
            	for(String name:params[i].paramName) {
            		par = param.get(name);
            		if(par!=null)break;
            	}
                if (par != null) {
                    try {
                    	if(params[i].convertion==null) {
                    		pars[i+1]=par;
                    	}else if(!par.isEmpty())
                    		pars[i + 1] = params[i].convertion.apply(par);
                    } catch (NumberFormatException | ClassCastException ex) {
                        throw new ScenarioExecutionException("Exception converting param " + Arrays.toString(params[i].paramName), ex);
                    }
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
    static Logger LOGGER = LogManager.getLogger("ScenarioExecutor");
    Class<T> objcls;
    public ScenarioExecutor(Class<T> objcls) {
		super();
		this.objcls = objcls;
	}
    private static Function<String, Object> number = s -> ((Double) Double.parseDouble(s));
    private static Function<String, Object> integer = s ->{ 
    	if(s==null)return s;
    	if(s.toLowerCase().startsWith("0x"))return (int)(Long.parseLong(s.substring(2),16));
    	return ((Double) Double.parseDouble(s)).intValue();
    	};

    private static Function<String, Object> fnumber = s -> ((Double) Double.parseDouble(s)).floatValue();

    Map<String, ScenarioMethod<T>> commands = new HashMap<>();

    public void callCommand(String name, T scenarioVM, Map<String, String> params) {
        ScenarioMethod<T> command = commands.get(name);
        if (command == null) {
            throw new ScenarioExecutionException("Can not find command " + name);
        }
        command.execute(scenarioVM, params);
    }

    public void register(Class<?> clazz) {
        try {
            Constructor<?> ctor = clazz.getConstructor();
            registerInst(ctor.newInstance());
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
                 IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
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
                		registerCommand(met.getName(), new MethodInfo<T>(Modifier.isStatic(met.getModifiers()) ? null :clazz,  met));
                } catch (ScenarioExecutionException ex) {
                    ex.printStackTrace();
                    LOGGER.warn(ex.getMessage());
                }
            }
        }
    }

    public void registerStatic(Class<?> clazz) {
        for (Method met : clazz.getMethods()) {
            if (Modifier.isPublic(met.getModifiers()) && Modifier.isStatic(met.getModifiers())) {
                try {
                    registerCommand(met.getName(), new MethodInfo(null, met));
                } catch (ScenarioExecutionException ex) {

                    ex.printStackTrace();
                    LOGGER.warn(ex.getMessage());
                }
            }
        }
    }
    static class Test{
    	public void test(ScenarioConductor sr,@Param("t")int t) {
    		System.out.println(t);

    	}
    }
    public static void main(String[] args) throws NoSuchMethodException, SecurityException {
    	Test t=new Test();
    	MethodInfo mi=new MethodInfo(t,t.getClass().getMethod("test", ScenarioConductor.class,int.class));
    	Map<String,String> mp=new HashMap<>();
    	mp.put("t", "20");
    	mi.execute(null, mp);
    }
}
