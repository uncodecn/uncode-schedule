package cn.uncode.schedule;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import cn.uncode.schedule.util.ScheduleUtil;
import cn.uncode.schedule.zk.IScheduleDataManager;
import cn.uncode.schedule.zk.ScheduleDataManager4ZK;
import cn.uncode.schedule.zk.ScheduleServer;
import cn.uncode.schedule.zk.ZKManager;

/**
 * 调度器核心管理
 * 
 * @author juny.ye
 * 
 */
public class ZKScheduleManager extends ThreadPoolTaskScheduler implements ApplicationContextAware {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final transient Logger LOGGER = LoggerFactory.getLogger(ZKScheduleManager.class);

	private Map<String, String> zkConfig;
	
	protected ZKManager zkManager;

	private IScheduleDataManager scheduleDataManager;

	/**
	 * 当前调度服务的信息
	 */
	protected ScheduleServer currenScheduleServer;

	/**
	 * 是否启动调度管理，如果只是做系统管理，应该设置为false
	 */
	public boolean start = true;

	/**
	 * 心跳间隔
	 */
	private int timerInterval = 2000;

	/**
	 * 是否注册成功
	 */
	private boolean isScheduleServerRegister = false;

	private static ApplicationContext applicationcontext;
	
	private Map<String, Boolean> isOwnerMap = new ConcurrentHashMap<String, Boolean>();

	private Timer hearBeatTimer;
	protected Lock initLock = new ReentrantLock();
	protected boolean isStopSchedule = false;
	protected Lock registerLock = new ReentrantLock();
	
	volatile String errorMessage = "No config Zookeeper connect infomation";
	private InitialThread initialThread;

	public ZKScheduleManager() {
		this.currenScheduleServer = ScheduleServer.createScheduleServer(null);
	}

	public void init() throws Exception {
		Properties properties = new Properties();
		for (Map.Entry<String, String> e : this.zkConfig.entrySet()) {
			properties.put(e.getKey(), e.getValue());
		}
		this.init(properties);
	}

	public void reInit(Properties p) throws Exception {
		if (this.start == true || this.hearBeatTimer != null) {
			throw new Exception("调度器有任务处理，不能重新初始化");
		}
		this.init(p);
	}

	public void init(Properties p) throws Exception {
		if (this.initialThread != null) {
			this.initialThread.stopThread();
		}
		this.initLock.lock();
		try {
			this.scheduleDataManager = null;
			if (this.zkManager != null) {
				this.zkManager.close();
			}
			this.zkManager = new ZKManager(p);
			this.errorMessage = "Zookeeper connecting ......"
					+ this.zkManager.getConnectStr();
			initialThread = new InitialThread(this);
			initialThread.setName("ScheduleManager-initialThread");
			initialThread.start();
		} finally {
			this.initLock.unlock();
		}
	}

	public void rewriteScheduleInfo() throws Exception {
		registerLock.lock();
		try {
			if (this.isStopSchedule == true) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("外部命令终止调度,不在注册调度服务，避免遗留垃圾数据："
							+ currenScheduleServer.getUuid());
				}
				return;
			}
			// 先发送心跳信息
			if (errorMessage != null) {
				this.currenScheduleServer.setDealInfoDesc(errorMessage);
			}
			if (this.scheduleDataManager
					.refreshScheduleServer(this.currenScheduleServer) == false) {
				// 更新信息失败，清除内存数据后重新注册
				this.clearMemoInfo();
				this.scheduleDataManager.registerScheduleServer(this.currenScheduleServer);
			}
			isScheduleServerRegister = true;
		} finally {
			registerLock.unlock();
		}
	}

	/**
	 * 清除内存中所有的已经取得的数据和任务队列,在心态更新失败，或者发现注册中心的调度信息被删除
	 */
	public void clearMemoInfo() {
		try {

		} finally {
		}

	}

	/**
	 * 根据当前调度服务器的信息，重新计算分配所有的调度任务
	 * 任务的分配是需要加锁，避免数据分配错误。为了避免数据锁带来的负面作用，通过版本号来达到锁的目的
	 * 
	 * 1、获取任务状态的版本号 2、获取所有的服务器注册信息和任务队列信息 3、清除已经超过心跳周期的服务器注册信息 3、重新计算任务分配
	 * 4、更新任务状态的版本号【乐观锁】 5、根系任务队列的分配信息
	 * 
	 * @throws Exception
	 */
	public void assignScheduleTask() throws Exception {
		scheduleDataManager.clearExpireScheduleServer();
		List<String> serverList = scheduleDataManager.loadScheduleServerNames();
		if (scheduleDataManager.isLeader(this.currenScheduleServer.getUuid(),
				serverList) == false) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(this.currenScheduleServer.getUuid()
						+ ":不是负责任务分配的Leader,直接返回");
			}
			return;
		}
		//黑名单
		for(String ip:zkManager.getIpBlacklist()){
			if(serverList.contains(ip)){
				serverList.remove(ip);
			}
		}
		// 设置初始化成功标准，避免在leader转换的时候，新增的线程组初始化失败
		scheduleDataManager.assignTask(this.currenScheduleServer.getUuid(), serverList);
	}

	/**
	 * 定时向数据配置中心更新当前服务器的心跳信息。 如果发现本次更新的时间如果已经超过了，服务器死亡的心跳周期，则不能在向服务器更新信息。
	 * 而应该当作新的服务器，进行重新注册。
	 * 
	 * @throws Exception
	 */
	public void refreshScheduleServer() throws Exception {
		try {
			rewriteScheduleInfo();
			// 如果任务信息没有初始化成功，不做任务相关的处理
			if (this.isScheduleServerRegister == false) {
				return;
			}

			// 重新分配任务
			this.assignScheduleTask();
			// 检查本地任务
			this.checkLocalTask();
		} catch (Throwable e) {
			// 清除内存中所有的已经取得的数据和任务队列,避免心跳线程失败时候导致的数据重复
			this.clearMemoInfo();
			if (e instanceof Exception) {
				throw (Exception) e;
			} else {
				throw new Exception(e.getMessage(), e);
			}
		}
	}
	
	public void checkLocalTask() throws Exception {
		// 检查系统任务执行情况
		scheduleDataManager.checkLocalTask(this.currenScheduleServer.getUuid());
	}

	/**
	 * 在Zk状态正常后回调数据初始化
	 * 
	 * @throws Exception
	 */
	public void initialData() throws Exception {
		this.zkManager.initial();
		this.scheduleDataManager = new ScheduleDataManager4ZK(this.zkManager);
		if (this.start == true) {
			// 注册调度管理器
			this.scheduleDataManager.registerScheduleServer(this.currenScheduleServer);
			if (hearBeatTimer == null) {
				hearBeatTimer = new Timer("ScheduleManager-"
						+ this.currenScheduleServer.getUuid() + "-HearBeat");
			}
			hearBeatTimer.schedule(new HeartBeatTimerTask(this), 2000, this.timerInterval);
		}
	}
	
	private Runnable taskWrapper(final Runnable task){
		return new Runnable(){
			public void run(){
				ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable)task;
		    	Method targetMethod = scheduledMethodRunnable.getMethod();
		    	String[] beanNames = applicationcontext.getBeanNamesForType(targetMethod.getDeclaringClass());
		    	if(null != beanNames && StringUtils.isNotEmpty(beanNames[0])){
		    		String name = ScheduleUtil.getTaskNameFormBean(beanNames[0], targetMethod.getName());
		    		boolean isOwner = false;
					try {
						if(isScheduleServerRegister == false){
							Thread.sleep(1000);
						}
						if(zkManager.checkZookeeperState()){
							isOwner = scheduleDataManager.isOwner(name, currenScheduleServer.getUuid());
							isOwnerMap.put(name, isOwner);
						}else{
							// 如果zk不可用，使用历史数据
							if(null != isOwnerMap){
								isOwner = isOwnerMap.get(name);
							}
						}
					} catch (Exception e) {
						LOGGER.error("Check task owner error.", e);
					}
		    		if(isOwner){
		    			task.run();
		    			LOGGER.info("Cron job has been executed.");
		    		}
		    	}
			}
		};
	}

	class HeartBeatTimerTask extends java.util.TimerTask {
		private transient final Logger log = LoggerFactory.getLogger(HeartBeatTimerTask.class);
		ZKScheduleManager manager;

		public HeartBeatTimerTask(ZKScheduleManager aManager) {
			manager = aManager;
		}

		public void run() {
			try {
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				manager.refreshScheduleServer();
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
	}

	class InitialThread extends Thread {
		private transient Logger log = LoggerFactory.getLogger(InitialThread.class);
		ZKScheduleManager sm;

		public InitialThread(ZKScheduleManager sm) {
			this.sm = sm;
		}

		boolean isStop = false;

		public void stopThread() {
			this.isStop = true;
		}

		@Override
		public void run() {
			sm.initLock.lock();
			try {
				int count = 0;
				while (sm.zkManager.checkZookeeperState() == false) {
					count = count + 1;
					if (count % 50 == 0) {
						sm.errorMessage = "Zookeeper connecting ......"
								+ sm.zkManager.getConnectStr() + " spendTime:"
								+ count * 20 + "(ms)";
						log.error(sm.errorMessage);
					}
					Thread.sleep(20);
					if (this.isStop == true) {
						return;
					}
				}
				sm.initialData();
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			} finally {
				sm.initLock.unlock();
			}

		}

	}

	public IScheduleDataManager getScheduleDataManager() {
		return scheduleDataManager;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ZKScheduleManager.applicationcontext = applicationcontext;
	}
	
	public void setZkManager(ZKManager zkManager) {
		this.zkManager = zkManager;
	}
	
	public ZKManager getZkManager() {
		return zkManager;
	}

	public void setZkConfig(Map<String, String> zkConfig) {
		this.zkConfig = zkConfig;
	}
	
	@Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
        return super.scheduleAtFixedRate(taskWrapper(task), period);
    }
	
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		return super.schedule(taskWrapper(task), trigger);
	}

	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		return super.schedule(taskWrapper(task), startTime);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		return super.scheduleAtFixedRate(taskWrapper(task), startTime, period);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		return super.scheduleWithFixedDelay(taskWrapper(task), startTime, delay);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		return super.scheduleWithFixedDelay(taskWrapper(task), delay);
	}
	
	public String getScheduleServerUUid(){
		if(null != currenScheduleServer){
			return currenScheduleServer.getUuid();
		}
		return null;
	}

	public Map<String, Boolean> getIsOwnerMap() {
		return isOwnerMap;
	}

	public static ApplicationContext getApplicationcontext() {
		return ZKScheduleManager.applicationcontext;
	}
	
	


}