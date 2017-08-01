package schedule.zk;

import cn.uncode.schedule.core.Version;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


/**
 * 
 * @author juny.ye
 *
 */
public class ZKManager{
    
    private static transient Logger log = LoggerFactory.getLogger(ZKManager.class);
    private ZooKeeper zk;
    private List<ACL> acl = new ArrayList<ACL>();
    private Properties properties;

    public enum keys {
        zkConnectString, rootPath, userName, password, zkSessionTimeout, autoRegisterTask, ipBlacklist
    }

    public ZKManager(Properties aProperties) throws Exception{
        this.properties = aProperties;
        this.connect();
    }
    
    /**
     * 重连zookeeper
     * @throws Exception
     */
    private synchronized void  reConnection() throws Exception{
        if (this.zk != null) {
            this.zk.close();
            this.zk = null;
            this.connect() ;
        }
    }
    
    private void connect() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        createZookeeper(connectionLatch);
        connectionLatch.await();
    }
    
    private void createZookeeper(final CountDownLatch connectionLatch) throws Exception {
        zk = new ZooKeeper(this.properties.getProperty(keys.zkConnectString
                .toString()), Integer.parseInt(this.properties
                .getProperty(keys.zkSessionTimeout.toString())),
                new Watcher() {
                    public void process(WatchedEvent event) {
                        sessionEvent(connectionLatch, event);
                    }
                });
        String authString = this.properties.getProperty(keys.userName.toString())
                + ":"+ this.properties.getProperty(keys.password.toString());
        zk.addAuthInfo("digest", authString.getBytes());
        acl.clear();
        acl.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
                DigestAuthenticationProvider.generateDigest(authString))));
        acl.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
    }
    
    private void sessionEvent(CountDownLatch connectionLatch, WatchedEvent event) {
        if (event.getState() == KeeperState.SyncConnected) {
            log.info("收到ZK连接成功事件！");
            connectionLatch.countDown();
        } else if (event.getState() == KeeperState.Expired) {
            log.error("会话超时，等待重新建立ZK连接...");
            try {
                reConnection();
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
        } // Disconnected：Zookeeper会自动处理Disconnected状态重连
    }
    
    public void close() throws InterruptedException {
        log.info("关闭zookeeper连接");
        this.zk.close();
    }
    
    String getRootPath(){
        return this.properties.getProperty(keys.rootPath.toString());
    }

    public List<String> getIpBlacklist(){
    	List<String> ips = new ArrayList<String>();
    	String list = this.properties.getProperty(keys.ipBlacklist.toString());
    	if(StringUtils.isNotEmpty(list)){
    		ips = Arrays.asList(list.split(","));
    	}
    	return ips;
    }
    public String getConnectStr(){
        return this.properties.getProperty(keys.zkConnectString.toString());
    }

    boolean isAutoRegisterTask(){
    	String autoRegisterTask = this.properties.getProperty(keys.autoRegisterTask.toString());
    	if(StringUtils.isNotEmpty(autoRegisterTask)){
    		return Boolean.valueOf(autoRegisterTask);
    	}
        return true;
    }

    public boolean checkZookeeperState() throws Exception{
        return zk != null && zk.getState() == States.CONNECTED;
    }

    public void initial() throws Exception {
        //当zk状态正常后才能调用
        checkParent(zk,this.getRootPath());
        if(zk.exists(this.getRootPath(), false) == null){
            ZKTools.createPath(zk, this.getRootPath(), CreateMode.PERSISTENT, acl);
            //设置版本信息
            zk.setData(this.getRootPath(),Version.getVersion().getBytes(),-1);
        }else{
            //先校验父亲节点，本身是否已经是schedule的目录
            byte[] value = zk.getData(this.getRootPath(), false, null);
            if(value == null){
                zk.setData(this.getRootPath(),Version.getVersion().getBytes(),-1);
            }else{
                String dataVersion = new String(value);
                if(!Version.isCompatible(dataVersion)){
                    throw new Exception("TBSchedule程序版本 "+ Version.getVersion() +" 不兼容Zookeeper中的数据版本 " + dataVersion );
                }
                log.info("当前的程序版本:" + Version.getVersion() + " 数据版本: " + dataVersion);
            }
        }
    }
    private static void checkParent(ZooKeeper zk, String path) throws Exception {
        String[] list = path.split("/");
        String zkPath = "";
        for (int i =0;i< list.length -1;i++){
            String str = list[i];
            if (StringUtils.isNotEmpty(str)) {
                zkPath = zkPath + "/" + str;
                if (zk.exists(zkPath, false) != null) {
                    byte[] value = zk.getData(zkPath, false, null);
                    if(value != null && new String(value).contains("uncode-schedule-")){
                        throw new Exception("\"" + zkPath +"\"  is already a schedule instance's root directory, its any subdirectory cannot as the root directory of others");
                    }
                }
            }
        }
    }   
    
    List<ACL> getAcl() {
        return acl;
    }

    ZooKeeper getZooKeeper() throws Exception {
        if(!this.checkZookeeperState()){
            reConnection();
        }
        return this.zk;
    }
    
}