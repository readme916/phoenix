package com.shangdao.phoenix.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.act.ActNotice.DelayType;
import com.shangdao.phoenix.entity.interfaces.ILog;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.interfaces.INoticeLog;
import com.shangdao.phoenix.entity.interfaces.IProjectEntity;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplateRepository;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate.NoticeChannel;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.notice.INotice;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.service.PostMethodService.PostMethodWrapper;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class QuartzNoticeJob extends QuartzJobStructure implements Job {

	private final static Logger logger = LoggerFactory.getLogger(QuartzNoticeJob.class);

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NoticeTemplateRepository noticeTemplateRepository;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDetail jobDetail = context.getJobDetail();
		JobKey key = jobDetail.getKey();

		EntityStructure logStructure = InitService.getStructure(getLogName());
		JpaRepository logRepository = logStructure.getJpaRepository();
		ILog log = (ILog) logRepository.findOne(getLogId());

		ILogEntity entity = log.getEntity();
		Act act = log.getAct();

		logger.info("exec:" + key.toString());
		logger.info(jobDetail.getJobDataMap().getWrappedMap().toString());

		NoticeTemplate noticeTemplate = noticeTemplateRepository.findOne(getTemplateId());

		ArrayList<List<User>> _notices = _notice(entity, noticeTemplate, act);

		for (User user : _notices.get(0)) {
			_saveNotice(user,entity,act,noticeTemplate,log,true);
		}
		for (User user : _notices.get(1)) {
			_saveNotice(user,entity,act,noticeTemplate,log,false);
		}
	}

	private void _saveNotice(User toUser, ILogEntity entity, Act act, NoticeTemplate noticeTemplate,ILog log, boolean success) {
		EntityStructure entityStructure = InitService.getStructure(entity.getClass());
		Class<?> noticeClass = entityStructure.getObjectFields().get("notices").getTargetEntity();
		INotice noticeBean = CommonUtils.getNotice(noticeTemplate);
		JpaRepository noticeRepository = InitService.getStructure(noticeClass).getJpaRepository();
		User systemUser = InitService.getSystemUser().getUser();

		try {
			INoticeLog noticeEntity = (INoticeLog) noticeClass.newInstance();
			noticeEntity.setAct(act);
			String content = noticeBean.parseMessage(entity, noticeTemplate, act, toUser, systemUser);
			noticeEntity.setContent(content);
			noticeEntity.setCreatedAt(new Date());
			noticeEntity.setDelayType(DelayType.valueOf(getDelayType()));
			noticeEntity.setNoticeTemplate(noticeTemplate);
			noticeEntity.setEntity(entity);
			noticeEntity.setLog(log);
			noticeEntity.setNoticeChannel(noticeTemplate.getNoticeChannel());
			noticeEntity.setRoleCode(getRoleCode());
			noticeEntity.setSuccess(success);
			noticeEntity.setToUser(toUser);
			noticeRepository.save(noticeEntity);
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ArrayList<List<User>> _notice(ILogEntity entity, NoticeTemplate noticeTemplate, Act act) {

		ArrayList<List<User>> arrayList = new ArrayList<List<User>>();
		List<User> failureUsers = new ArrayList<User>();

		List<User> toUsers = _usersByRoleCode(entity);
		System.out.println("通知目标用户：" + toUsers);
		List<User> perhapsUsers = _filterUsersCannotNotice(toUsers, noticeTemplate);
		System.out.println("过滤后剩下的用户：" + perhapsUsers);
		List<User> successUsers = _noticeAll(entity, noticeTemplate, perhapsUsers, act);
		System.out.println("通知成功的用户：" + successUsers);
		for (User user : toUsers) {
			if (!successUsers.contains(user)) {
				failureUsers.add(user);
			}
		}
		arrayList.add(successUsers);
		arrayList.add(failureUsers);
		return arrayList;

	}

	private List<User> _filterUsersCannotNotice(List<User> toUsers, NoticeTemplate noticeTemplate) {
		// TODO Auto-generated method stub
		ArrayList<User> arrayList = new ArrayList<User>();
		INotice notice = CommonUtils.getNotice(noticeTemplate);
		for (User user : toUsers) {
			if (notice.canSend(user)) {
				arrayList.add(user);
			}
		}
		return arrayList;
	}

	private List<User> _noticeAll(ILogEntity entity, NoticeTemplate noticeTemplate, List<User> toUsers, Act act) {
		List<User> successUsers = new ArrayList<User>();
		User systemUser = InitService.getSystemUser().getUser();
		INotice notice = CommonUtils.getNotice(noticeTemplate);
		for (User user : toUsers) {
			System.out.println("打算通知给：" + user.getUsername());
			if (notice.sendMessage(entity, noticeTemplate, act, user, systemUser)) {
				successUsers.add(user);
			}
		}
		return successUsers;
	}

	private List<User> _usersByRoleCode(ILogEntity entity) {
		ArrayList<User> arrayList = new ArrayList<User>();
		if (getRoleCode().equalsIgnoreCase("CREATOR")) {
			if (entity.getCreatedBy() != null) {
				arrayList.add(entity.getCreatedBy());
				return arrayList;
			}
		}
		if (entity instanceof IProjectEntity) {
			IProjectEntity project = (IProjectEntity) entity;

			if (getRoleCode().equalsIgnoreCase("MEMBER")) {
				if (project.getMembers() != null) {
					arrayList.addAll(project.getMembers());
				}
			} else if (getRoleCode().equalsIgnoreCase("MANAGER")) {
				if (project.getManager() != null) {
					arrayList.add(project.getManager());
				}
			} else if (getRoleCode().equalsIgnoreCase("SUBSCRIBER")) {
				if (project.getSubscribers() != null) {
					arrayList.addAll(project.getSubscribers());
				}
			} else if (getRoleCode().equalsIgnoreCase("DEPARTMENT")) {
				if (project.getDepartments() != null) {
					Set departments = project.getDepartments();
					List<User> users = userRepository.findDistinctByDepartmentsIn(departments);
					if (users != null) {
						arrayList.addAll(userRepository.findDistinctByDepartmentsIn(departments));
					}
				}
			} else {
				Role findByCode = roleRepository.findByCode(getRoleCode());
				if (findByCode != null) {
					List<User> users = userRepository.findByRoles(findByCode);
					if (users != null) {
						arrayList.addAll(userRepository.findByRoles(findByCode));
					}
				}
			}
			return arrayList;
		} else {
			Role findByCode = roleRepository.findByCode(getRoleCode());
			if (findByCode != null) {
				List<User> users = userRepository.findByRoles(findByCode);
				if (users != null) {
					arrayList.addAll(userRepository.findByRoles(findByCode));
				}
			}
		}

		return arrayList;
	}

}