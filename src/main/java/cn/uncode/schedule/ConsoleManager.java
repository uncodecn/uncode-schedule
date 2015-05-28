package cn.uncode.schedule;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;












import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.uncode.schedule.zk.IScheduleDataManager;
import cn.uncode.schedule.zk.ZKManager;



public class ConsoleManager {   
    protected static transient Logger log = LoggerFactory.getLogger(ConsoleManager.class);

    public final static String configFile = System.getProperty("user.dir") + File.separator
            + "pamirsScheduleConfig.properties";

    private static ZKScheduleManager scheduleManager;
    
    public static boolean isInitial() throws Exception{
        return scheduleManager != null;
    }
    public static boolean  initial() throws Exception{
        if(scheduleManager != null){
            return true;
        }
        File file = new File(configFile);
        scheduleManager = new ZKScheduleManager();
        scheduleManager.start = false;
        
        if(file.exists() == true){
            //Console不启动调度能力
            Properties p = new Properties();
            FileReader reader = new FileReader(file);
            p.load(reader);
            reader.close();
            scheduleManager.init(p);
            log.info("加载Schedule配置文件：" +configFile );
            return true;
        }else{
            return false;
        }
    }   
    public static ZKScheduleManager getScheduleManager() throws Exception {
        if(isInitial() == false){
            initial();
        }
        return scheduleManager;
    }
    public static IScheduleDataManager getScheduleDataManager() throws Exception{
        if(isInitial() == false){
            initial();
        }
        return scheduleManager.getScheduleDataManager();
    }

    public static Properties loadConfig() throws IOException{
        File file = new File(configFile);
        Properties properties;
        if(file.exists() == false){
            properties = ZKManager.createProperties();
        }else{
            properties = new Properties();
            FileReader reader = new FileReader(file);
            properties.load(reader);
            reader.close();
        }
        return properties;
    }
    public static void saveConfigInfo(Properties p) throws Exception {
        try {
            FileWriter writer = new FileWriter(configFile);
            p.store(writer, "");
            writer.close();
        } catch (Exception ex) {
            throw new Exception("不能写入配置信息到文件：" + configFile,ex);
        }
            if(scheduleManager == null){
                initial();
            }else{
            	scheduleManager.reInit(p);
            }
    }
    public static void setScheduleManagerFactory(ZKScheduleManager scheduleManager) {
        ConsoleManager.scheduleManager = scheduleManager;
    }
    
    public static void addScheduleTask(String taskName) {
        try {
			ConsoleManager.scheduleManager.getScheduleDataManager().addTask(taskName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
}
