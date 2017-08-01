package schedule.core;

/**
 * 
 * @author juny.ye
 *
 */
public class Version {
	
   private final static String version="uncode-schedule-1.0.0";
   
   public static String getVersion(){
	   return version;
   }
   public static boolean isCompatible(String dataVersion){
	   return version.compareTo(dataVersion) >= 0;
   }
   
}
