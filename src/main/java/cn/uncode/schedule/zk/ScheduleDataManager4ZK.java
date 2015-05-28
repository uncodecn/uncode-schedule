package cn.uncode.schedule.zk;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * zk实现类
 * 
 * @author juny.ye
 *
 */
public class ScheduleDataManager4ZK implements IScheduleDataManager {
	private static final transient Logger LOG = LoggerFactory.getLogger(ScheduleDataManager4ZK.class);
	
	private static final String NODE_SERVER = "server";
	private static final String NODE_TASK = "task";
	private static final long SERVER_EXPIRE_TIME = 5000 * 3;
	private Gson gson ;
	private ZKManager zkManager;
	private String pathServer;
	private String pathTask;
	private long zkBaseTime = 0;
	private long loclaBaseTime = 0;
	private Random random;
	
    public ScheduleDataManager4ZK(ZKManager aZkManager) throws Exception {
    	this.zkManager = aZkManager;
    	gson = new GsonBuilder().registerTypeAdapter(Timestamp.class,new TimestampTypeAdapter()).setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		this.pathServer = this.zkManager.getRootPath() +"/" + NODE_SERVER;
		this.pathTask = this.zkManager.getRootPath() +"/" + NODE_TASK;
		this.random = new Random();
		if (this.getZooKeeper().exists(this.pathServer, false) == null) {
			ZKTools.createPath(getZooKeeper(),this.pathServer, CreateMode.PERSISTENT, this.zkManager.getAcl());
		}
		loclaBaseTime = System.currentTimeMillis();
        String tempPath = this.zkManager.getZooKeeper().create(this.zkManager.getRootPath() + "/systime",null, this.zkManager.getAcl(), CreateMode.EPHEMERAL_SEQUENTIAL);
        Stat tempStat = this.zkManager.getZooKeeper().exists(tempPath, false);
        zkBaseTime = tempStat.getCtime();
        ZKTools.deleteTree(getZooKeeper(), tempPath);
        if(Math.abs(this.zkBaseTime - this.loclaBaseTime) > 5000){
        	LOG.error("请注意，Zookeeper服务器时间与本地时间相差 ： " + Math.abs(this.zkBaseTime - this.loclaBaseTime) +" ms");
        }
	}	
	
	public ZooKeeper getZooKeeper() throws Exception {
		return this.zkManager.getZooKeeper();
	}

	public boolean refreshScheduleServer(ScheduleServer server) throws Exception {
		Timestamp heartBeatTime = new Timestamp(this.getSystemTime());
    	String zkPath = this.pathServer +"/" + server.getUuid();
    	if(this.getZooKeeper().exists(zkPath, false)== null){
    		//数据可能被清除，先清除内存数据后，重新注册数据
    		server.setRegister(false);
    		return false;
    	}else{
    		Timestamp oldHeartBeatTime = server.getHeartBeatTime();
    		server.setHeartBeatTime(heartBeatTime);
    		server.setVersion(server.getVersion() + 1);
    		String valueString = this.gson.toJson(server);
    		try{
    			this.getZooKeeper().setData(zkPath,valueString.getBytes(),-1);
    		}catch(Exception e){
    			//恢复上次的心跳时间
    			server.setHeartBeatTime(oldHeartBeatTime);
    			server.setVersion(server.getVersion() - 1);
    			throw e;
    		}
    		return true;
    	}
	}

	@Override
	public void registerScheduleServer(ScheduleServer server) throws Exception {
		if(server.isRegister() == true){
			throw new Exception(server.getUuid() + " 被重复注册");
		}
		//clearExpireScheduleServer();
		String realPath = null;
		//此处必须增加UUID作为唯一性保障
		StringBuffer id = new StringBuffer();
		id.append(server.getIp()).append("$")
			.append(UUID.randomUUID().toString().replaceAll("-", "").toUpperCase());
		String zkServerPath = pathServer + "/" + id.toString() +"$";
		realPath = this.getZooKeeper().create(zkServerPath, null, this.zkManager.getAcl(),CreateMode.PERSISTENT_SEQUENTIAL);
		server.setUuid(realPath.substring(realPath.lastIndexOf("/") + 1));
		
		Timestamp heartBeatTime = new Timestamp(getSystemTime());
		server.setHeartBeatTime(heartBeatTime);
		
		String valueString = this.gson.toJson(server);
		this.getZooKeeper().setData(realPath,valueString.getBytes(),-1);
		server.setRegister(true);
	}
	
	public List<String> loadAllScheduleServer() throws Exception {
		String zkPath = this.pathServer;
		List<String> names = this.getZooKeeper().getChildren(zkPath,false);
		Collections.sort(names);
		return names;
	}
	
	public void clearExpireScheduleServer() throws Exception{
		 String zkPath = this.pathServer;
		 if(this.getZooKeeper().exists(zkPath,false)== null){
			 this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
		 }
		for (String name : this.zkManager.getZooKeeper().getChildren(zkPath, false)) {
			try {
				Stat stat = new Stat();
				this.getZooKeeper().getData(zkPath + "/" + name, null, stat);
				if (getSystemTime() - stat.getMtime() > SERVER_EXPIRE_TIME) {
					ZKTools.deleteTree(this.getZooKeeper(), zkPath + "/" + name);
					LOG.debug("ScheduleServer[" + zkPath + "/" + name + "]过期清除");
				}
			} catch (Exception e) {
				// 当有多台服务器时，存在并发清理的可能，忽略异常
			}
		}
	}
	
	public List<String> loadScheduleServerNames(String taskType)throws Exception {
		String zkPath = this.pathServer;
		if (this.getZooKeeper().exists(zkPath, false) == null) {
			return new ArrayList<String>();
		}
		List<String> serverList = this.getZooKeeper().getChildren(zkPath, false);
		Collections.sort(serverList, new Comparator<String>() {
			public int compare(String u1, String u2) {
				return u1.substring(u1.lastIndexOf("$") + 1).compareTo(
						u2.substring(u2.lastIndexOf("$") + 1));
			}
		});
		return serverList;
	}
	
	public List<String> loadScheduleServerNames() throws Exception {
		String zkPath = this.pathServer;
		if (this.getZooKeeper().exists(zkPath, false) == null) {
			return new ArrayList<String>();
		}
		List<String> serverList = this.getZooKeeper()
				.getChildren(zkPath, false);
		Collections.sort(serverList, new Comparator<String>() {
			public int compare(String u1, String u2) {
				return u1.substring(u1.lastIndexOf("$") + 1).compareTo(
						u2.substring(u2.lastIndexOf("$") + 1));
			}
		});
		return serverList;
	}
	
	
	@Override
	public void assignTask(String currentUuid, List<String> taskServerList) throws Exception {
		 if(this.isLeader(currentUuid,taskServerList)==false){
			 if(LOG.isDebugEnabled()){
				 LOG.debug(currentUuid +":不是负责任务分配的Leader,直接返回");
			 }
			 return;
		 }
		 if(LOG.isDebugEnabled()){
			 LOG.debug(currentUuid +":开始重新分配任务......");
		 }		
		 if(taskServerList.size()<=0){
			 //在服务器动态调整的时候，可能出现服务器列表为空的清空
			 return;
		 }
		 if(this.zkManager.checkZookeeperState()){
			 String zkPath = this.pathTask;
			 if(this.getZooKeeper().exists(zkPath,false)== null){
				 this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
			 }
			 List<String> children = this.getZooKeeper().getChildren(zkPath, false);
			 if(null != children && children.size() > 0){
				 for(int i = 0; i < children.size(); i++){
					 String taskName = children.get(i);
					 String taskPath = zkPath + "/" + taskName;
					 if(this.getZooKeeper().exists(taskPath, false) == null){
						 this.getZooKeeper().create(taskPath, null, this.zkManager.getAcl(),CreateMode.PERSISTENT);
					 }
					 List<String> taskServerIds = this.getZooKeeper().getChildren(taskPath, false);
					 if(null == taskServerIds || taskServerIds.size() == 0){
						 assignServer2Task(taskServerList, taskPath);
					 }else{
						 boolean hasAssignSuccess = false;
						 for(String serverId:taskServerIds){
							 if(taskServerList.contains(serverId)){
								 hasAssignSuccess = true;
								 continue;
							 }
							 ZKTools.deleteTree(this.getZooKeeper(), taskPath + "/" + serverId);
						 }
						 if(hasAssignSuccess == false){
							 assignServer2Task(taskServerList, taskPath);
						 }
					 }
				 }	
			 }else{
				 if(LOG.isDebugEnabled()){
					 LOG.debug(currentUuid +":没有集群任务");
				 }	
			 }
		 }
		 
	}

	private void assignServer2Task(List<String> taskServerList, String taskPath)throws Exception {
		int index = random.nextInt(taskServerList.size());
		 String serverId = taskServerList.get(index);
		 this.getZooKeeper().create(taskPath + "/" + serverId, null, this.zkManager.getAcl(),CreateMode.PERSISTENT);
		 if(LOG.isDebugEnabled()){
			 StringBuffer buffer = new StringBuffer();
			 buffer.append("Assign server [").append(serverId).append("]").append(" to task [").append(taskPath).append("]");
			 LOG.debug(buffer.toString());
		 }
	}

	public boolean isLeader(String uuid,List<String> serverList){
    	return uuid.equals(getLeader(serverList));
    }
	
	public String getLeader(List<String> serverList){
		if(serverList == null || serverList.size() ==0){
			return "";
		}
		long no = Long.MAX_VALUE;
		long tmpNo = -1;
		String leader = null;
    	for(String server:serverList){
    		tmpNo =Long.parseLong( server.substring(server.lastIndexOf("$")+1));
    		if(no > tmpNo){
    			no = tmpNo;
    			leader = server;
    		}
    	}
    	return leader;
    }
	
	public long getSystemTime(){
		return this.zkBaseTime + ( System.currentTimeMillis() - this.loclaBaseTime);
	}
	
	class TimestampTypeAdapter implements JsonSerializer<Timestamp>, JsonDeserializer<Timestamp>{   
	    public JsonElement serialize(Timestamp src, Type arg1, JsonSerializationContext arg2) {   
	    	DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
	        String dateFormatAsString = format.format(new Date(src.getTime()));   
	        return new JsonPrimitive(dateFormatAsString);   
	    }   
	  
	    public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {   
	        if (!(json instanceof JsonPrimitive)) {   
	            throw new JsonParseException("The date should be a string value");   
	        }   
	  
	        try {   
	        	DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
	            Date date = (Date) format.parse(json.getAsString());   
	            return new Timestamp(date.getTime());   
	        } catch (Exception e) {   
	            throw new JsonParseException(e);   
	        }   
	    }
	}

	@Override
	public boolean isOwner(String name, String uuid) throws Exception {
		//查看集群中是否注册当前任务，如果没有就自动注册
		String zkPath = this.pathTask + "/" + name;
		if(this.zkManager.isAutoRegisterTask()){
			if(this.getZooKeeper().exists(zkPath,false) == null){
				this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(),CreateMode.PERSISTENT);
				if(LOG.isDebugEnabled()){
					 LOG.debug(uuid +":自动向集群注册任务[" + name + "]");
				 }
			}
		}
		//判断是否分配给当前节点
		zkPath = zkPath + "/" + uuid;
		if(this.getZooKeeper().exists(zkPath,false) != null){
			return true;
		}
		return false;
	}

	@Override
	public void addTask(String name) throws Exception {
		String zkPath = this.pathTask;
		if(this.getZooKeeper().exists(zkPath,false)== null){
			this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
		}
		if(this.getZooKeeper().exists(zkPath + "/" + name,false) == null){
			this.getZooKeeper().create(zkPath + "/" + name, null, this.zkManager.getAcl(), CreateMode.PERSISTENT);
		}
	}


}



 