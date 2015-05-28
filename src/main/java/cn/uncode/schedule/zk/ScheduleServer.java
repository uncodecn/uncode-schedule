package cn.uncode.schedule.zk;

import java.sql.Timestamp;
import java.util.UUID;

import cn.uncode.schedule.util.ScheduleUtil;


/**
 * 调度服务器信息定义
 * 
 * @author juny.ye
 *
 */
public class ScheduleServer {
    /**
     * 全局唯一编号
     */
    private String uuid;


    private String ownSign;
    /**
     * 机器IP地址
     */
    private String ip;

    /**
     * 机器名称
     */
    private String hostName;

    /**
     * 服务开始时间
     */
    private Timestamp registerTime;
    /**
     * 最后一次心跳通知时间
     */
    private Timestamp heartBeatTime;
    /**
     * 最后一次取数据时间
     */
    private Timestamp lastFetchDataTime;
    /**
     * 处理描述信息，例如读取的任务数量，处理成功的任务数量，处理失败的数量，处理耗时
     * FetchDataCount=4430,FetcheDataNum=438570,DealDataSucess=438570,DealDataFail=0,DealSpendTime=651066
     */
    private String dealInfoDesc;

    private String nextRunStartTime;

    private String nextRunEndTime;
    /**
     * 配置中心的当前时间
     */
    private Timestamp centerServerTime;

    /**
     * 数据版本号
     */
    private long version;
    
    private boolean isRegister;
    
    public ScheduleServer() {

    }

    public static ScheduleServer createScheduleServer(String aOwnSign){
    	long currentTime = System.currentTimeMillis();
        ScheduleServer result = new ScheduleServer();
        result.ownSign = aOwnSign;
        result.ip = ScheduleUtil.getLocalIP();
        result.hostName = ScheduleUtil.getLocalHostName();
        result.registerTime = new Timestamp(currentTime);
        result.heartBeatTime = null;
        result.dealInfoDesc = "调度初始化";
        result.version = 0;
        result.uuid = result.ip
                + "$"
                + (UUID.randomUUID().toString().replaceAll("-", "")
                        .toUpperCase());
        return result;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }


    public Timestamp getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Timestamp registerTime) {
        this.registerTime = registerTime;
    }

    public Timestamp getHeartBeatTime() {
        return heartBeatTime;
    }

    public void setHeartBeatTime(Timestamp heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    public Timestamp getLastFetchDataTime() {
        return lastFetchDataTime;
    }

    public void setLastFetchDataTime(Timestamp lastFetchDataTime) {
        this.lastFetchDataTime = lastFetchDataTime;
    }

    public String getDealInfoDesc() {
        return dealInfoDesc;
    }

    public void setDealInfoDesc(String dealInfoDesc) {
        this.dealInfoDesc = dealInfoDesc;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public Timestamp getCenterServerTime() {
        return centerServerTime;
    }

    public void setCenterServerTime(Timestamp centerServerTime) {
        this.centerServerTime = centerServerTime;
    }

    public String getNextRunStartTime() {
        return nextRunStartTime;
    }

    public void setNextRunStartTime(String nextRunStartTime) {
        this.nextRunStartTime = nextRunStartTime;
    }

    public String getNextRunEndTime() {
        return nextRunEndTime;
    }

    public void setNextRunEndTime(String nextRunEndTime) {
        this.nextRunEndTime = nextRunEndTime;
    }

    public String getOwnSign() {
        return ownSign;
    }

    public void setOwnSign(String ownSign) {
        this.ownSign = ownSign;
    }


    public void setRegister(boolean isRegister) {
        this.isRegister = isRegister;
    }

    public boolean isRegister() {
        return isRegister;
    }


}