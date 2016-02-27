package cn.uncode.schedule.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import cn.uncode.schedule.ConsoleManager;
import cn.uncode.schedule.zk.TaskDefine;

public class ManagerServlet extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8160082230341182715L;
	
	private static final String HTML = 
		    "<html>"+ 
		    "<head>"+ 
		    "<title>Uncode-Schedule管理页面</title>"+ 
		    "</head>"+ 
		    "<h1>Uncode-Schedule管理页面</h1>"+ 
		    "<h1>集群节点</h1>"+ 
		    "<table  border=\"1\">"+ 
			"<thead>"+ 
			"<tr>"+ 
			"<th>序号</th>"+ 
			"<th>名称</th>"+ 
			"<th>调度节点</th>"+ 
			"</tr>"+ 
			"</thead>"+ 
			"<tbody>"+ 
				"%s"+
			"</tbody>"+ 
		    "</table>"+ 
		    "</body>"+ 
		    
			"<h1>定时任务列表</h1>"+ 
			"<table  border=\"1\">"+ 
			"<thead>"+ 
			"<tr>"+ 
			"<th>序号</th>"+ 
			"<th>目标bean</th>"+ 
			"<th>目标方法</th>"+ 
			"<th>cron表达式</th>"+ 
			"<th>开始时间</th>"+ 
			"<th>周期（秒）</th>"+ 
			"<th>执行节点</th>"+ 
			"<th>操作</th>"+ 
			"</tr>"+ 
			"</thead>"+ 
			"<tbody>"+ 
				"%s"+
			"</tbody>"+ 
			"</table>"+ 
			"</body>"+
		    "</html>";
	
	
	private static final String FORM = 
			"<h1>新增定时任务</h1>"+ 
			"<form id=\"addform\" method=\"post\" action=\"%s\">"+
				"bean名称*：<input name=\"bean\" type=\"text\"/><br/>"+
				"方法名称*：<input name=\"method\" type=\"text\"/><br/>"+
				"corn表达式：<input name=\"cronExpression\" type=\"text\"/><br/>"+
				"周期（秒）：<input name=\"period\" type=\"text\"/><br/>"+
				"开始时间：<input name=\"startTime\" type=\"text\"/><br/>"+
				"<input type=\"button\" onclick=\"formSubmit()\" value=\"Submit\">"+
			"</form>"+
			"<script type=\"text/javascript\">"+
				"function formSubmit(){"+
				 	"document.getElementById(\"addform\").submit();"+
				 "}"+
			"</script>";
	
	
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String del = request.getParameter("del");
		String bean = request.getParameter("bean");
		String method = request.getParameter("method");
		if(StringUtils.isNotEmpty(del)){
			String[] dels = del.split("_");
			ConsoleManager.delScheduleTask(dels[0], dels[1]);
		}else if(StringUtils.isNotEmpty(bean) && StringUtils.isNotEmpty(method)){
			TaskDefine taskDefine = new TaskDefine();
			taskDefine.setTargetBean(bean);
			taskDefine.setTargetMethod(method);
			String cronExpression = request.getParameter("cronExpression");
			if(StringUtils.isNotEmpty(cronExpression)){
				taskDefine.setCronExpression(cronExpression);
			}
			String period = request.getParameter("period");
			if(StringUtils.isNotEmpty(period)){
				taskDefine.setPeriod(Long.valueOf(period));
			}
			String startTime = request.getParameter("startTime");
			if(StringUtils.isNotEmpty(startTime)){
				taskDefine.setStartTime(new Date());
			}
			if(StringUtils.isNotEmpty(cronExpression) || StringUtils.isNotEmpty(period)){
				ConsoleManager.addScheduleTask(taskDefine);
			}
		}
		try {
			List<String> servers = ConsoleManager.getScheduleManager().getScheduleDataManager().loadScheduleServerNames();
			if(servers != null){
				response.setContentType("text/html");  
		        PrintWriter out = response.getWriter();  
		        StringBuffer sb = new StringBuffer();
	    		for(int i=0; i< servers.size();i++){
	    			String ser = servers.get(0);
	    			sb.append("<tr>")
	    			  .append("<td>").append(i+1).append("</td>")
	    			  .append("<td>").append(ser).append("</td>");
					if( ConsoleManager.getScheduleManager().getScheduleDataManager().isLeader(ser, servers)){
						sb.append("<td>").append("是").append("</td>");
					}else{
						sb.append("<td>").append("否").append("</td>");
					}
	    			sb.append("</tr>");
	    		}
	    		
	    		List<TaskDefine> tasks = ConsoleManager.queryScheduleTask();
	    		StringBuffer sbTask = new StringBuffer();
	    		for(int i=0; i< tasks.size();i++){
	    			TaskDefine taskDefine = tasks.get(i);
	    			sbTask.append("<tr>")
	    			  .append("<td>").append(i+1).append("</td>")
	    			  .append("<td>").append(taskDefine.getTargetBean()).append("</td>")
	    			  .append("<td>").append(taskDefine.getTargetMethod()).append("</td>")
	    			  .append("<td>").append(taskDefine.getCronExpression()).append("</td>")
	    			  .append("<td>").append(taskDefine.getStartTime()).append("</td>")
	    			  .append("<td>").append(taskDefine.getPeriod()).append("</td>")
	    			  .append("<td>").append(taskDefine.getCurrentServer()).append("</td>")
	    			  .append("<td>").append("<a href=\"").append(request.getSession().getServletContext().getContextPath())
	    			  				 .append("/uncode/schedule?del=")
	    			                 .append(taskDefine.getTargetBean())
	    			                 .append("_")
	    			                 .append(taskDefine.getTargetMethod())
	    			                 .append("\">删除</a>")
	    			                 .append("</td>");
					sbTask.append("</tr>");
	    		}
	    		out.println(String.format(HTML, sb.toString(), sbTask.toString()));
	    		out.println(String.format(FORM, request.getSession().getServletContext().getContextPath()+"/uncode/schedule"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
