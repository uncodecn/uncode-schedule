package cn.uncode.schedule.zk;

/**
 * 
 * @author juny.ye
 *
 */
public class Version {
	
   public final static String version="uncode-schedule-1.0.0";
   
   public static String getVersion(){
	   return version;
   }
   public static boolean isCompatible(String dataVersion){
	  if(version.compareTo(dataVersion)>=0){
		  return true;
	  }else{
		  return false;
	  }
   }
   
}
