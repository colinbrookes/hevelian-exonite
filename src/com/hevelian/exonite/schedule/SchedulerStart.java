package com.hevelian.exonite.schedule;

import com.hevelian.exonite.core.Configuration;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SchedulerStart extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private Configuration conf = new Configuration();
	private Scheduler sched = null;
	
	public void init() throws ServletException {
		super.init();
		
		SchedulerFactory sf = new StdSchedulerFactory();
		try {
			sched = sf.getScheduler();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		
		NodeList nodes = conf.getScheduledTasks();
		
		for(int i=0; i<nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			String id = null;
			String task = null;
			String cron = null;
			
			for(int n=0; n<node.getChildNodes().getLength(); n++) {
				Node nNode = node.getChildNodes().item(n);
				
				if(nNode.getNodeName().equalsIgnoreCase("id")) 	id = nNode.getTextContent();
				if(nNode.getNodeName().equalsIgnoreCase("task")) task = nNode.getTextContent();
				if(nNode.getNodeName().equalsIgnoreCase("cron")) cron = nNode.getTextContent();
			}
			
			JobDetail job = JobBuilder.newJob(ScheduleJob.class)
				    .withIdentity("eXonite:" + id, "eXonite")
				    .withDescription("eXonite:ScheduleJob:" + task)
				    .build();
			
			Trigger trigger = TriggerBuilder
					.newTrigger()
					.withIdentity("Trigger:eXonite:" + id, "eXonite")
					.withSchedule(CronScheduleBuilder.cronSchedule(cron))
					.build();
			try {
				sched.scheduleJob(job, trigger);
				sched.start();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SchedulerStart() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doExecute(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doExecute(request, response);
	}

	protected void doExecute(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException {
		System.out.println("SCHEDULER START: in doExecute");
		return;
	}
}
