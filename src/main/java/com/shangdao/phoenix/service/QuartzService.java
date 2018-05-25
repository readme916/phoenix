package com.shangdao.phoenix.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;


@Service
public class QuartzService {
	@Autowired
	SchedulerFactoryBean schedulerFactoryBean;
	
	public Object scheduleJobs() throws SchedulerException {
		Scheduler scheduler = schedulerFactoryBean.getScheduler();
		ArrayList<Map> arrayList = new ArrayList<Map>();

		for (String groupName : scheduler.getJobGroupNames()) {

			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

				String jobName = jobKey.getName();
				String jobGroup = jobKey.getGroup();

				JobDetail jobDetail = scheduler.getJobDetail(jobKey);
				JobDataMap jobDataMap = jobDetail.getJobDataMap();

				// get job's trigger
				List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);

				Date nextFireTime = triggers.get(0).getNextFireTime();
				jobDataMap.put("nextFireTime", nextFireTime);
				arrayList.add(jobDataMap);

			}
		}

		Collections.sort(arrayList, (new Comparator<Map>() {
			@Override
			public int compare(Map o1, Map o2) {
				if(o1.get("nextFireTime")!=null && o2.get("nextFireTime")!=null){
					return ((Date)o1.get("nextFireTime")).compareTo((Date)o2.get("nextFireTime"));
				}
				return 0;
			}
		}));

		return arrayList;
	}
	
	
}



