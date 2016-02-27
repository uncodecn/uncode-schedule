package cn.uncode.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @author juny.ye
 */
@Component
public class SimpleTask {

    private static int i = 0;
    
    @Scheduled(fixedDelay = 1000) 
    public void print() {
        System.out.println("===========start!=========");
        System.out.println("I:"+i);i++;
        System.out.println("=========== end !=========");
    }
    
    public void print1() {
        System.out.println("===========start!=========");
        System.out.println("print1:"+i);i++;
        System.out.println("=========== end !=========");
    }
    
    public void print2() {
        System.out.println("===========start!=========");
        System.out.println("print2:"+i);i++;
        System.out.println("=========== end !=========");
    }
    
    public void print3() {
        System.out.println("===========start!=========");
        System.out.println("print3:"+i);i++;
        System.out.println("=========== end !=========");
    }
    
    public void print4() {
        System.out.println("===========start!=========");
        System.out.println("print4:"+i);i++;
        System.out.println("=========== end !=========");
    }
    


}
