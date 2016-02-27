package cn.uncode.schedule.zk;

import java.util.Date;

/**
 * 任务定义，提供关键信息给使用者
 * @author juny.ye
 *
 */
public class TaskDefine {
	
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
	
	
	
	public boolean begin(Date sysTime){
		if(null == sysTime){
			return false;
		}
		return sysTime.after(startTime);
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

	

    
    

}