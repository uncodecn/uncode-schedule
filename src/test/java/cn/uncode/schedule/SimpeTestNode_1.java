package cn.uncode.schedule;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author juny.ye
 */
public class SimpeTestNode_1 {

    public static void main(String[] args) throws InterruptedException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext1.xml");
        Thread.sleep(Long.MAX_VALUE);
    }

}
