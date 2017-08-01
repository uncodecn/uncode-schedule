
package schedule.quartz;

/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import cn.uncode.schedule.ConsoleManager;
import cn.uncode.schedule.core.TaskDefine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.*;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.scheduling.quartz.JobMethodInvocationFailedException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ContextLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link FactoryBean} that exposes a
 * {@link JobDetail} object which delegates job execution to a
 * specified (static or non-static) method. Avoids the need for implementing
 * a one-line Quartz Job that just invokes an existing service method on a
 * Spring-managed target bean.
 *
 * <p>Inherits common configuration properties from the {@link MethodInvoker}
 * base class, such as {@link #setTargetObject "targetObject"} and
 * {@link #setTargetMethod "targetMethod"}, adding support for lookup of the target
 * bean by name through the {@link #setTargetBeanName "targetBeanName"} property
 * (as alternative to specifying a "targetObject" directly, allowing for
 * non-singleton target objects).
 *
 * <p>Supports both concurrently running jobs and non-currently running
 * jobs through the "concurrent" property. Jobs created by this
 * MethodInvokingJobDetailFactoryBean are by default volatile and durable
 * (according to Quartz terminology).
 *
 * <p><b>NOTE: JobDetails created via this FactoryBean are <i>not</i>
 * serializable and thus not suitable for persistent job stores.</b>
 * You need to implement your own Quartz Job as a thin wrapper for each case
 * where you want a persistent job to delegate to a specific service method.
 *
 * <p>Compatible with Quartz 1.5+ as well as Quartz 2.0/2.1, as of Spring 3.1.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @since 18.02.2004
 * @see #setTargetBeanName
 * @see #setTargetObject
 * @see #setTargetMethod
 * @see #setConcurrent
 */
public class MethodInvokingJobDetailFactoryBean extends ArgumentConvertingMethodInvoker
		implements FactoryBean<JobDetail>, BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

	private static final transient Logger LOGGER = LoggerFactory.getLogger(MethodInvokingJobDetailFactoryBean.class);

	private static Class<?> jobDetailImplClass;

	private static Method setResultMethod;

	static {
		try {
			jobDetailImplClass = Class.forName("org.quartz.impl.JobDetailImpl");
		}
		catch (ClassNotFoundException ex) {
			jobDetailImplClass = null;
		}
		try {
			Class<?> jobExecutionContextClass =
					QuartzJobBean.class.getClassLoader().loadClass("org.quartz.JobExecutionContext");
			setResultMethod = jobExecutionContextClass.getMethod("setResult", Object.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Incompatible Quartz API: " + ex);
		}
	}


	private String name;

	private String group = Scheduler.DEFAULT_GROUP;

	private boolean concurrent = true;

	private String targetBeanName;

	private String[] jobListenerNames;

	private String beanName;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private BeanFactory beanFactory;

	private JobDetail jobDetail;


	/**
	 * Set the name of the job.
	 * Default is the bean name of this FactoryBean.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the group of the job.
	 * <p>Default is the default group of the Scheduler.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Specify whether or not multiple jobs should be run in a concurrent
	 * fashion. The behavior when one does not want concurrent jobs to be
	 * executed is realized through adding the interface.
	 * More information on stateful versus stateless jobs can be found
	 * <a href="http://www.quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03">here</a>.
	 * <p>The default setting is to run jobs concurrently.
	 */
	public void setConcurrent(boolean concurrent) {
		this.concurrent = concurrent;
	}

	/**
	 * Set the name of the target bean in the Spring BeanFactory.
	 * <p>This is an alternative to specifying {@link #setTargetObject "targetObject"},
	 * allowing for non-singleton beans to be invoked. Note that specified
	 * "targetObject" and {@link #setTargetClass "targetClass"} values will
	 * override the corresponding effect of this "targetBeanName" setting
	 * (i.e. statically pre-define the bean type or even the bean object).
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Set a list of JobListener names for this job, referring to
	 * non-global JobListeners registered with the Scheduler.
	 * <p>A JobListener name always refers to the name returned
	 * by the JobListener implementation.
	 */
	public void setJobListenerNames(String[] names) {
		this.jobListenerNames = names;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}


	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();

		// Use specific name if given, else fall back to bean name.
		String name = (this.name != null ? this.name : this.beanName);

		// Consider the concurrent flag to choose between stateful and stateless job.
		Class<?> jobClass = (this.concurrent ? MethodInvokingJob.class : StatefulMethodInvokingJob.class);

		// Build JobDetail instance.
		if (jobDetailImplClass != null) {
			// Using Quartz 2.0 JobDetailImpl class...
			this.jobDetail = (JobDetail) BeanUtils.instantiate(jobDetailImplClass);
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this.jobDetail);
			bw.setPropertyValue("name", name);
			bw.setPropertyValue("group", this.group);
			bw.setPropertyValue("jobClass", jobClass);
			bw.setPropertyValue("durability", true);
			((JobDataMap) bw.getPropertyValue("jobDataMap")).put("methodInvoker", this);
		}
        else {
			// Using Quartz 1.x JobDetail class...
			/*this.jobDetail = new JobDetail(name, this.group, jobClass);
			this.jobDetail.setVolatility(true);
			this.jobDetail.setDurability(true);
			this.jobDetail.getJobDataMap().put("methodInvoker", this);*/
		}

		// Register job listener names.
		if (this.jobListenerNames != null) {
			for (String jobListenerName : this.jobListenerNames) {
				if (jobListenerName != null) {
					throw new IllegalStateException("Non-global JobListeners not supported on Quartz 2 - " +
							"manually register a Matcher against the Quartz ListenerManager instead");
				}
				//this.jobDetail.addJobListener(jobListenerName);
			}
		}

		postProcessJobDetail(this.jobDetail);
	}

	/**
	 * Callback for post-processing the JobDetail to be exposed by this FactoryBean.
	 * <p>The default implementation is empty. Can be overridden in subclasses.
	 * @param jobDetail the JobDetail prepared by this FactoryBean
	 */
	protected void postProcessJobDetail(JobDetail jobDetail) {
	}


	/**
	 * Overridden to support the {@link #setTargetBeanName "targetBeanName"} feature.
	 */
	@Override
	public Class<?> getTargetClass() {
		Class<?> targetClass = super.getTargetClass();
		if (targetClass == null && this.targetBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
			targetClass = this.beanFactory.getType(this.targetBeanName);
		}
		return targetClass;
	}

	/**
	 * Overridden to support the {@link #setTargetBeanName "targetBeanName"} feature.
	 */
	@Override
	public Object getTargetObject() {
		Object targetObject = super.getTargetObject();
		if (targetObject == null && this.targetBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
			targetObject = this.beanFactory.getBean(this.targetBeanName);
		}
		return targetObject;
	}

	public String getTargetBeanName(){
		String[] names = ContextLoader.getCurrentWebApplicationContext().getBeanNamesForType(getTargetClass());
		if(null != names){
			return names[0];
		}
		return null;
	}


	public JobDetail getObject() {
		return this.jobDetail;
	}

	public Class<? extends JobDetail> getObjectType() {
		return (this.jobDetail != null ? this.jobDetail.getClass() : JobDetail.class);
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Quartz Job implementation that invokes a specified method.
	 * Automatically applied by MethodInvokingJobDetailFactoryBean.
	 */
	public static class MethodInvokingJob extends QuartzJobBean {

		protected static final Log logger = LogFactory.getLog(MethodInvokingJob.class);

		private MethodInvoker methodInvoker;

		/**
		 * Set the MethodInvoker to use.
		 */
		public void setMethodInvoker(MethodInvoker methodInvoker) {
			this.methodInvoker = methodInvoker;
		}

		/**
		 * Invoke the method via the MethodInvoker.
		 */
		@Override
		protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
			try {
				TaskDefine taskDefine = new TaskDefine();
				MethodInvokingJob methodInvokingJob = (cn.uncode.schedule.quartz.MethodInvokingJobDetailFactoryBean.MethodInvokingJob) context.getJobInstance();
				cn.uncode.schedule.quartz.MethodInvokingJobDetailFactoryBean methodInvokingJobDetailFactoryBean= (cn.uncode.schedule.quartz.MethodInvokingJobDetailFactoryBean)methodInvokingJob.methodInvoker;
				taskDefine.setTargetBean(methodInvokingJobDetailFactoryBean.getTargetBeanName());
				taskDefine.setTargetMethod(this.methodInvoker.getTargetMethod());
				taskDefine.setType(TaskDefine.TYPE_QUARTZ_TASK);
				if(context.getTrigger() instanceof org.quartz.CronTrigger){
					CronTriggerImpl cronTriggerImpl = (CronTriggerImpl) context.getTrigger();
					if(null != cronTriggerImpl.getCronExpression()){
						taskDefine.setCronExpression(cronTriggerImpl.getCronExpression());
					}
					if(null != cronTriggerImpl.getStartTime()){
						taskDefine.setStartTime(cronTriggerImpl.getStartTime());
					}
					if(cronTriggerImpl.getPriority() > 0){
						taskDefine.setPeriod(cronTriggerImpl.getPriority());
					}
				}
				String name = taskDefine.stringKey();
				boolean isOwner = false;
				boolean isRunning = true;
				try {
					boolean isExists = true;
					if(ConsoleManager.getScheduleManager().getZkManager().checkZookeeperState()){
						isExists = ConsoleManager.isExistsTask(taskDefine);
						isOwner = ConsoleManager.getScheduleManager().getScheduleDataManager().isOwner(name, ConsoleManager.getScheduleManager().getScheduleServerUUid());
						ConsoleManager.getScheduleManager().getIsOwnerMap().put(name, isOwner);
					}else{
						// 如果zk不可用，使用历史数据
						if(null != ConsoleManager.getScheduleManager().getIsOwnerMap()){
							isOwner = ConsoleManager.getScheduleManager().getIsOwnerMap().get(name);
						}
						isRunning = ConsoleManager.getScheduleManager().getScheduleDataManager().isRunning(name);
					}
					if(!isExists){
						ConsoleManager.addScheduleTask(taskDefine);
					}
				} catch (Exception e) {
					LOGGER.error("Check task owner error.", e);
				}
	    		if(isOwner && isRunning){
	    			ReflectionUtils.invokeMethod(setResultMethod, context, this.methodInvoker.invoke());
	    			ConsoleManager.getScheduleManager().getScheduleDataManager().saveRunningInfo(name, ConsoleManager.getScheduleManager().getScheduleServerUUid());
	    			LOGGER.info("Cron job has been executed.");
	    		}
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof JobExecutionException) {
					// -> JobExecutionException, to be logged at info level by Quartz
					throw (JobExecutionException) ex.getTargetException();
				}
				else {
					// -> "unhandled exception", to be logged at error level by Quartz
					throw new JobMethodInvocationFailedException(this.methodInvoker, ex.getTargetException());
				}
			}
			catch (Exception ex) {
				// -> "unhandled exception", to be logged at error level by Quartz
				throw new JobMethodInvocationFailedException(this.methodInvoker, ex);
			}
		}
	}


	/**
	 * Extension of the MethodInvokingJob, implementing the StatefulJob interface.
	 * Quartz checks whether or not jobs are stateful and if so,
	 * won't let jobs interfere with each other.
	 */
	public static class StatefulMethodInvokingJob extends MethodInvokingJob implements Job {

		// No implementation, just an addition of the tag interface StatefulJob
		// in order to allow stateful method invoking jobs.
	}

}
