package cn.uncode.schedule.local;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.ReflectionUtils;

import cn.uncode.schedule.ConsoleManager;
import cn.uncode.schedule.zk.TaskDefine;



public class DynamicTaskManager {
	
	private static final transient Logger LOGGER = LoggerFactory.getLogger(DynamicTaskManager.class);
	
	
	private static final Map<String, ScheduledFuture<?>> SCHEDULE_FUTURES = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	
	
	/**
	 * 启动定时任务
	 * @param taskDefine
	 * @param currentTime
	 */
	public static void scheduleTask(TaskDefine taskDefine, Date currentTime){
		scheduleTask(taskDefine.getTargetBean(), taskDefine.getTargetMethod(),
				taskDefine.getCronExpression(), taskDefine.getStartTime(), taskDefine.getPeriod());
	}
	
	public static void clearLocalTask(List<String> existsTaskName){
		for(String name:SCHEDULE_FUTURES.keySet()){
			if(!existsTaskName.contains(name)){
				SCHEDULE_FUTURES.get(name).cancel(true);
				SCHEDULE_FUTURES.remove(name);
			}
		}
	}

	/**
	 * 启动定时任务
	 * 支持：
	 * 1 cron时间表达式，立即执行
	 * 2 startTime + period,指定时间，定时进行
	 * 3 period，定时进行，立即开始
	 * 4 startTime，指定时间执行
	 * 
	 * @param targetBean
	 * @param targetMethod
	 * @param cronExpression
	 * @param startTime
	 * @param period
	 */
	public static void scheduleTask(String targetBean, String targetMethod, String cronExpression, Date startTime, long period){
		String scheduleKey = buildScheduleKey(targetBean, targetMethod);
		try {
			ScheduledFuture<?> scheduledFuture = null;
			ScheduledMethodRunnable scheduledMethodRunnable = buildScheduledRunnable(targetBean, targetMethod);
			if(scheduledMethodRunnable != null){
				if (!SCHEDULE_FUTURES.containsKey(scheduleKey)) {
					if(StringUtils.isNotEmpty(cronExpression)){
						Trigger trigger = new CronTrigger(cronExpression);
						scheduledFuture = ConsoleManager.getScheduleManager().schedule(scheduledMethodRunnable, trigger);
					}else if(startTime != null){
						if(period > 0){
							scheduledFuture = ConsoleManager.getScheduleManager().scheduleAtFixedRate(scheduledMethodRunnable, startTime, period);
						}else{
							scheduledFuture = ConsoleManager.getScheduleManager().schedule(scheduledMethodRunnable, startTime);
						}
					}else if(period > 0){
						scheduledFuture = ConsoleManager.getScheduleManager().scheduleAtFixedRate(scheduledMethodRunnable, period);
					}
					SCHEDULE_FUTURES.put(scheduleKey, scheduledFuture);
					LOGGER.debug("Building new schedule task, target bean "+ targetBean + " target method " + targetMethod + ".");
				}
			}else{
				LOGGER.debug("Bean name is not exists.");
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	
	private static String buildScheduleKey(String targetBean, String targetMethod){
		return targetBean + "#" + targetMethod;
	}
	
	/**
	 * 封装任务对象
	 * @param targetBean
	 * @param targetMethod
	 * @return
	 */
	private static ScheduledMethodRunnable buildScheduledRunnable(String targetBean, String targetMethod){
		Object bean = null;
		Method method = null;
		ScheduledMethodRunnable scheduledMethodRunnable = null;
		try {
			bean = ConsoleManager.getScheduleManager().getApplicationcontext().getBean(targetBean);
			if(bean != null){
				if(AopUtils.isAopProxy(bean)){
					method = ReflectionUtils.findMethod(AopProxyUtils.ultimateTargetClass(bean), targetMethod);
				}else{
					method = ReflectionUtils.findMethod(bean.getClass(), targetMethod);
				}
				if(method != null){
					scheduledMethodRunnable = new ScheduledMethodRunnable(bean, method);
				}
			}
		} catch (Exception e) {
			LOGGER.debug(e.getLocalizedMessage(), e);
		}
		return scheduledMethodRunnable;
	}
	

}
