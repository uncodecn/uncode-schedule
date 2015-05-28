# uncode-schedule

基于zookeeper+spring task的分布式任务调度组件，非常小巧，无需任何修改就可以使spring task具备分布式特性，确保所有任务在集群中不重复，不遗漏的执行。



# 功能概述

1. 基于zookeeper+spring task的分布任务调度系统。
2. 确保每个任务在集群中不同节点上不重复的执行。
3. 单个任务节点故障时自动转移到其他任务节点继续执行。
4. 任务节点启动时必须保证zookeeper可用，任务节点运行期zookeeper集群不可用时任务节点保持可用前状态运行，zookeeper集群恢复正常运期。
5. 支持已有任务动态停止和运行。


说明：
* 单节点故障时需要业务保障数据完整性或幂等性。
* 具体使用方式和spring task相同，只需要配置ZKScheduleManager即可。


项目地址：
oschina:http://git.oschina.net/uncode/uncode-schedule
github:




------------------------------------------------------------------------

# 基于Spring XML配置

## Spring bean

	public class SimpleTask {

		private static int i = 0;
		
		public void print() {
			System.out.println("===========start!=========");
			System.out.println("I:"+i);i++;
			System.out.println("=========== end !=========");
		}
	}

## xml配置

	<!-- 分布式任务管理器 -->
	<bean id="zkScheduleManager" class="cn.uncode.schedule.ZKScheduleManager"
		init-method="init">
		<property name="zkConfig">
			   <map>
				  <entry key="zkConnectString" value="127.0.0.1:2181" />
				  <entry key="rootPath" value="/uncode/schedule" />
				  <entry key="zkSessionTimeout" value="60000" />
				  <entry key="userName" value="ScheduleAdmin" />
				  <entry key="password" value="password" />
				  <!-- 是否自动注册本地任务到zk集群，默认为true -->
				  <entry key="autoRegisterTask" value="true" />
			   </map>
		</property>
	</bean>
	<!-- Spring bean配置 -->
	<bean id="taskObj" class="cn.uncode.schedule.SimpleTask"/>
	<!-- Spring task配置 -->
	<task:scheduled-tasks scheduler="zkScheduleManager">
		<task:scheduled ref="taskObj" method="print"  fixed-rate="5000"/>
	</task:scheduled-tasks>
	
------------------------------------------------------------------------

# 基于Spring Annotation配置

## Spring bean

	@Component
	public class SimpleTask {

		private static int i = 0;
		
		@Scheduled(fixedDelay = 1000) 
		public void print() {
			System.out.println("===========start!=========");
			System.out.println("I:"+i);i++;
			System.out.println("=========== end !=========");
		}
		
	}

## xml配置

	<!-- 配置注解扫描 -->
    <context:annotation-config />
	<!-- 自动扫描的包名 -->
    <context:component-scan base-package="cn.uncode.schedule" />
	<!-- 分布式任务管理器 -->
	<bean id="zkScheduleManager" class="cn.uncode.schedule.ZKScheduleManager"
		init-method="init">
		<property name="zkConfig">
			   <map>
				  <entry key="zkConnectString" value="127.0.0.1:2181" />
				  <entry key="rootPath" value="/uncode/schedule" />
				  <entry key="zkSessionTimeout" value="60000" />
				  <entry key="userName" value="ScheduleAdmin" />
				  <entry key="password" value="password" />
				  <!-- 是否自动注册本地任务到zk集群，默认为true -->
				  <entry key="autoRegisterTask" value="true" />
			   </map>
		</property>
	</bean>
	<!-- Spring定时器注解开关-->
	<task:annotation-driven scheduler="zkScheduleManager" />

------------------------------------------------------------------------

# 版权

作者：juny（ywj_316@qq.com）

技术支持QQ群：47306892

Copyright 2013 www.uncode.cn