package schedule.core;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * 任务定义，提供关键信息给使用者
 * @author juny.ye
 *
 */
public class TaskDefine {
	
	public static final String TYPE_UNCODE_TASK = "uncode-task";
	public static final String TYPE_SPRING_TASK = "spring-task";
	public static final String TYPE_QUARTZ_TASK = "quartz";
	
	public static final String STATUS_ERROR = "error";
	public static final String STATUS_STOP = "stop";
	public static final String STATUS_RUNNING = "running";
	
    /**
     * 目标bean
     */
    private String targetBean;
    
    /**
     * 目标方法
     */
    private String targetMethod;
    
    /**
     * cron表达式
     */
    private String cronExpression;
	
	/**
	 * 开始时间
	 */
	private Date startTime;
	
	/**
	 * 周期（秒）
	 */
	private long period;
	
	private String currentServer;
	
	/**
	 * 参数
	 */
	private String params;
	
	/**
	 * 类型
	 */
	private String type;
	
	/**
	 * 后台显示参数，无业务内含
	 */
	private int runTimes;
	
	/**
	 * 后台显示参数，无业务内含
	 */
	private long lastRunningTime;
	
	/**
	 * 后台显示参数，无业务内含
	 */
	private String status = STATUS_RUNNING;
	
	/**
	 * key的后缀
	 */
	private String extKeySuffix;
	
	public boolean begin(Date sysTime) {
		return null != sysTime && sysTime.after(startTime);
	}

	public String getTargetBean() {
		return targetBean;
	}

	public void setTargetBean(String targetBean) {
		this.targetBean = targetBean;
	}

	public String getTargetMethod() {
		return targetMethod;
	}

	public void setTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}

	public String getCurrentServer() {
		return currentServer;
	}

	public void setCurrentServer(String currentServer) {
		this.currentServer = currentServer;
	}
	
	public String stringKey(){
		String result = null;
		boolean notBlank = StringUtils.isNotBlank(getTargetBean()) && StringUtils.isNotBlank(getTargetMethod());
		if(notBlank){
			result = getTargetBean() + "#" + getTargetMethod();
		}
		if(StringUtils.isNotBlank(extKeySuffix)){
			result += "-" + extKeySuffix;
		}
		return result;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getRunTimes() {
		return runTimes;
	}

	public void setRunTimes(int runTimes) {
		this.runTimes = runTimes;
	}

	public long getLastRunningTime() {
		return lastRunningTime;
	}

	public void setLastRunningTime(long lastRunningTime) {
		this.lastRunningTime = lastRunningTime;
	}

	public boolean isStop() {
		return STATUS_STOP.equals(this.status);
	}

	public void setStop() {
		this.status = STATUS_STOP;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getExtKeySuffix() {
		return extKeySuffix;
	}

	public void setExtKeySuffix(String extKeySuffix) {
		this.extKeySuffix = extKeySuffix;
	}

	public void valueOf(TaskDefine taskDefine){
		if(StringUtils.isNotBlank(taskDefine.getTargetBean())){
			this.targetBean = taskDefine.getTargetBean();
		}
		if(StringUtils.isNotBlank(taskDefine.getTargetMethod())){
			this.targetMethod = taskDefine.getTargetMethod();
		}
		if(StringUtils.isNotBlank(taskDefine.getCronExpression())){
			this.cronExpression = taskDefine.getCronExpression();
		}
		if(startTime != null){
			this.startTime = taskDefine.getStartTime();
		}
		this.period = taskDefine.getPeriod();
		if(StringUtils.isNotBlank(taskDefine.getParams())){
			this.params = taskDefine.getParams();
		}
		if(StringUtils.isNotBlank(taskDefine.getType())){
			this.type = taskDefine.getType();
		}
		if(StringUtils.isNotBlank(taskDefine.getStatus())){
			this.status = taskDefine.getStatus();
		}
		if(StringUtils.isNotBlank(taskDefine.getExtKeySuffix())){
			this.extKeySuffix = taskDefine.getExtKeySuffix();
		}
	}


	
	
	
	
}