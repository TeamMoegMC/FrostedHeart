package com.teammoeg.frostedheart.scenario;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.teammoeg.frostedheart.scenario.runner.ParagraphRunner;
import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class ScenarioExecutor {
	static Logger LOGGER=LogManager.getLogger("ScenarioExecutor");
	private static Function<String,Object> string=s->s;
	private static Function<String,Object> number=s->((Double)Double.parseDouble(s));
	private static Function<String,Object> integer=s->((Double)Double.parseDouble(s)).intValue();
	private static Function<String,Object> fnumber=s->((Double)Double.parseDouble(s)).floatValue();
	Map<String,ScenarioMethod> commands=new HashMap<>();
	@FunctionalInterface
	public interface ScenarioMethod{
		void execute(ScenarioRunner runner,Map<String,String> param);
	}
	private static class MethodInfo implements ScenarioMethod{
		private static class ParamInfo{
			String paramName;
			Function<String,Object> convertion;
			public ParamInfo(String paramName, Function<String, Object> convertion) {
				this.paramName = paramName;
				this.convertion = convertion;
			}
		}
		
		public MethodInfo(Object instance,Method method) {
			super();
			this.instance=instance;
			this.method = method;
			Parameter[] param=method.getParameters();
			params=new ParamInfo[param.length-1];
			
			for(int i=1;i<params.length;i++) {
				Function<String,Object> converter=null;
				Class<?> partype=param[i].getType();
				String name=param[i].getName();
				if(partype.isAssignableFrom(Double.class)||partype==double.class) {
					converter=number;
				}else if(partype.isAssignableFrom(String.class)) {
					converter=string;
				}else if(partype.isAssignableFrom(Integer.class)||partype==int.class) {
					converter=integer;
				}else if(partype.isAssignableFrom(Float.class)||partype==float.class) {
					converter=fnumber;
				}else {
					throw new ScenarioExecutionException("No matching type found for param "+name+" of "+method.getName());
				}
				params[i-1]=new ParamInfo(name,converter);
			}
		}
		Method method;
		Object instance;
		ParamInfo[] params;
		@Override
		public void execute(ScenarioRunner runner,Map<String,String> param) {
			Object[] pars=new Object[params.length+1];
			for(int i=0;i<params.length;i++) {
				String par=param.get(params[i].paramName);
				if(par!=null) {
					try{
						pars[i+1]=params[i].convertion.apply(par);
					}catch(NumberFormatException | ClassCastException ex) {
						throw new ScenarioExecutionException("Exception converting param "+params[i].paramName, ex);
					}
				}
				pars[0]=runner;
				try {
					method.invoke(instance, pars);
				} catch (IllegalArgumentException|IllegalAccessException e) {
					throw new ScenarioExecutionException(e);
				} catch (InvocationTargetException e) {
					throw new ScenarioExecutionException(e.getTargetException());
				}
			}
		}
	}
	public void registerCommand(String cmdName,ScenarioMethod method) {
		commands.put(cmdName.toLowerCase(), method);
	}
	public void regiser(Class<?> clazz) {
		registerStatic(clazz);
		try {
			Constructor<?> ctor=clazz.getConstructor();
			registerInst(ctor.newInstance());
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		}
		
	}
	public void registerStatic(Class<?> clazz) {
		for(Method met:clazz.getMethods()) {
			if(Modifier.isPublic(met.getModifiers())&&Modifier.isStatic(met.getModifiers())) {
				try {
					registerCommand(met.getName(),new MethodInfo(null,met));
				}catch(ScenarioExecutionException ex) {
					
					ex.printStackTrace();
					LOGGER.warn(ex.getMessage());
				}
			}
		}
	}
	public void registerInst(Object clazz) {
		for(Method met:clazz.getClass().getMethods()) {
			if(Modifier.isPublic(met.getModifiers())) {
				try {
					registerCommand(met.getName(),new MethodInfo(clazz,Modifier.isStatic(met.getModifiers())?null:met));
				}catch(ScenarioExecutionException ex) {
					ex.printStackTrace();
					LOGGER.warn(ex.getMessage());
				}
			}
		}
	}
	public void callCommand(String name,ScenarioRunner runner,Map<String,String> params) {
		ScenarioMethod command=commands.get(name);
		if(command==null) {
			throw new ScenarioExecutionException("Can not find command "+name);
		}
		command.execute(runner, params);
	}
}
