package com.hevelian.exonite.schedule;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;

import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.core.Task;

public class ScheduleJob implements Job {

	Task task = new Task();
	
	@Override
	public void execute(JobExecutionContext _context) throws JobExecutionException {

		JobDetail jobDetail = _context.getJobDetail();
		Configuration config = new Configuration();
		
		System.out.println("EXONITE: SCHEDULED JOB: " + jobDetail.getDescription());
		
		try {
			System.out.println("EXONITE: loading task");
			// we need to read the task file and pass it to the Task runner
			String[] job 		= jobDetail.getDescription().split(":");
			String filename 	= config.getProperty("folder_home") + job[2] + ".xml";
			
			System.out.println("EXONITE: loading task: " + filename);
			
			File _xmlFile = new File(filename);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(_xmlFile);
			
			System.out.println("EXONITE: ABOUT TO RUN TASK");
			
			task.run(doc);
			
			System.out.println("EXONITE: FINISHED RUNNING TASK");
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
