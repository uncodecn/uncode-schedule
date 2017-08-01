package schedule.core;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

public class ScheduledMethodRunnable implements Runnable {

	private final Object target;

	private final Method method;
	
	private final String params;

	private final String suffix;


	public ScheduledMethodRunnable(Object target, Method method, String params,String suffix) {
		this.target = target;
		this.method = method;
		this.params = params;
		this.suffix = suffix;
	}

	public ScheduledMethodRunnable(Object target, String methodName, String params,String suffix) throws NoSuchMethodException {
		this.target = target;
		this.method = target.getClass().getMethod(methodName);
		this.params = params;
		this.suffix = suffix;
	}


	public Object getTarget() {
		return this.target;
	}

	public Method getMethod() {
		return this.method;
	}
	
	public String getParams() {
		return params;
	}

    public String getSuffix() {
        return suffix;
    }

    @Override
	public void run() {
		try {
			ReflectionUtils.makeAccessible(this.method);
			if(this.getParams() != null){
				this.method.invoke(this.target, this.getParams());
			}else{
				this.method.invoke(this.target);
			}
		}
		catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
		}
		catch (IllegalAccessException ex) {
			throw new UndeclaredThrowableException(ex);
		}
	}

}
