package cn.uncode.schedule;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.junit.Test;

import cn.uncode.schedule.zk.ZKTools;


/**
 * @author juny.ye
 */
public class ZookeeperTest {
	@Test
	public void testCloseStatus() throws Exception {
		ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, null);
		int i = 1;
		while (true) {
			try {
				StringWriter writer = new StringWriter();
				ZKTools.printTree(zk, "/uncode/schedule", writer, "");
				System.out
						.println(i++ + "----" + writer.getBuffer().toString());
				Thread.sleep(2000);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Test
	public void testPrint() throws Exception {
		ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, null);
		StringWriter writer = new StringWriter();
		ZKTools.printTree(zk, "/", writer, "\n");
		System.out.println(writer.getBuffer().toString());
	}

	@Test
	public void deletePath() throws Exception {
		ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, null);
		zk.addAuthInfo("digest", "ScheduleAdmin:password".getBytes());

		ZKTools.deleteTree(zk, "/uncode/schedule");
		StringWriter writer = new StringWriter();
		ZKTools.printTree(zk, "/", writer, "\n");
		System.out.println(writer.getBuffer().toString());
	}

	@Test
	public void testCreateTask() throws Exception {
		ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, null);
		List<ACL> acls = new ArrayList<ACL>();
		zk.addAuthInfo("digest", "ScheduleAdmin:password".getBytes());
		acls.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
				DigestAuthenticationProvider
						.generateDigest("ScheduleAdmin:password"))));
		acls.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
		zk.create("/uncode/schedule/task/taskObj#print", new byte[0], acls, CreateMode.PERSISTENT);
		zk.getData("/uncode/schedule/task/taskObj#print", false, null);
	}
}