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

public class ScenarioExecutor {
    private static class MethodInfo implements ScenarioMethod {
        private static class ParamInfo {
            String paramName;
            Function<String, Object> convertion;
            Supplier<Object> def=null;
            public ParamInfo(String paramName, Function<String, Object> convertion) {
                this.paramName = paramName;
                this.convertion = convertion;
            }

			@Override
			public String toString() {
				return "[name=" + paramName + "]";
			}
            
            
        }

        Method method;

        Object instance;
        ParamInfo[] params;
        public MethodInfo(Object instance, Method method) {
            super();
            this.instance = instance;
            this.method = method;
            Parameter[] param = method.getParameters();
            params = new ParamInfo[param.length - 1];

            for (int i = 1; i < param.length; i++) {
                Function<String, Object> converter = null;
                Class<?> partype = param[i].getType();
                Param par=param[i].getAnnotation(Param.class);
                String name = par!=null?par.value():(param[i].isNamePresent()?param[i].getName():param[i].getName().substring(4));
                Supplier<Object> def=null;
                if (partype.isAssignableFrom(Double.class) || partype == double.class) {
                    converter = number;
                    if(partype.isPrimitive())
                    	def=()->0d;
                } else if (partype.isAssignableFrom(String.class)) {
                    converter = string;
                } else if (partype.isAssignableFrom(Integer.class) || partype == int.class) {
                    converter = integer;
                    if(partype.isPrimitive())
                    	def=()->0;
                } else if (partype.isAssignableFrom(Float.class) || partype == float.class) {
                    converter = fnumber;
                    if(partype.isPrimitive())
                    	def=()->0f;
                } else {
                    throw new ScenarioExecutionException("No matching type found for param " + name + " of " + method.getName());
                }
                params[i - 1] = new ParamInfo(name, converter);
                params[i - 1].def=def;
            }
            //System.out.println(toString());
        }

        @Override
		public String toString() {
			return "[method=" +method.getDeclaringClass().getSimpleName()+"." + method.getName() + ", params=" + Arrays.toString(params) + "]";
		}

		@Override
        public void execute(ScenarioConductor runner, Map<String, String> param) {
            Object[] pars = new Object[params.length + 1];
            for (int i = 0; i < params.length; i++) {
                String par = param.get(params[i].paramName);
                if (par != null) {
                    try {
                        pars[i + 1] = params[i].convertion.apply(par);
                    } catch (NumberFormatException | ClassCastException ex) {
                        throw new ScenarioExecutionException("Exception converting param " + params[i].paramName, ex);
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
            	System.out.println(e.getMessage());
                throw new ScenarioExecutionException(e);
            } catch (InvocationTargetException e) {
                throw new ScenarioExecutionException(e.getTargetException());
            }
        }
    }
    @FunctionalInterface
    public interface ScenarioMethod {
        void execute(ScenarioConductor runner, Map<String, String> param);
    }
    static Logger LOGGER = LogManager.getLogger("ScenarioExecutor");
    private static Function<String, Object> string = s -> s;
    private static Function<String, Object> number = s -> ((Double) Double.parseDouble(s));
    private static Function<String, Object> integer = s -> ((Double) Double.parseDouble(s)).intValue();

    private static Function<String, Object> fnumber = s -> ((Double) Double.parseDouble(s)).floatValue();

    Map<String, ScenarioMethod> commands = new HashMap<>();

    public void callCommand(String name, ScenarioConductor runner, Map<String, String> params) {
        ScenarioMethod command = commands.get(name);
        if (command == null) {
            throw new ScenarioExecutionException("Can not find command " + name);
        }
        command.execute(runner, params);
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

    public void registerCommand(String cmdName, ScenarioMethod method) {
        commands.put(cmdName.toLowerCase(), method);
    }

    public void registerInst(Object clazz) {
        for (Method met : clazz.getClass().getMethods()) {
            if (Modifier.isPublic(met.getModifiers())) {
                try {
                	if(met.getParameterCount()>0&&met.getParameters()[0].getType()==ScenarioConductor.class)
                		registerCommand(met.getName(), new MethodInfo(Modifier.isStatic(met.getModifiers()) ? null :clazz,  met));
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
    		
    	};
    }
    public static void main(String[] args) throws NoSuchMethodException, SecurityException {
    	Test t=new Test();
    	MethodInfo mi=new MethodInfo(t,t.getClass().getMethod("test", ScenarioConductor.class,int.class));
    	Map<String,String> mp=new HashMap<>();
    	mp.put("t", "20");
    	mi.execute(null, mp);
    }
}
