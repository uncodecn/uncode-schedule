package cn.uncode.schedule.zk;

import java.util.List;


/**
 * 调度配置中心客户端接口，可以有基于数据库的实现，可以有基于ConfigServer的实现
 * 
 * @author juny.ye
 * 
 */
public interface IScheduleDataManager{

    /**
     * 发送心跳信息
     * 
     * @param server
     * @throws Exception
     */
	public boolean refreshScheduleServer(ScheduleServer server) throws Exception;

    /**
     * 注册服务器
     * 
     * @param server
     * @throws Exception
     */
    public void registerScheduleServer(ScheduleServer server) throws Exception;

    
    public boolean isLeader(String uuid,List<String> serverList);
    

	public void clearExpireScheduleServer() throws Exception;
	
	
	public List<String> loadScheduleServerNames() throws Exception;
	
	public void assignTask(String currentUuid, List<String> taskServerList) throws Exception;
	
	public boolean isOwner(String name, String uuid)throws Exception;
	
	public void addTask(String name)throws Exception;
    
    
     
}