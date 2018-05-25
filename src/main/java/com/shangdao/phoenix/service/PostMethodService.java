package com.shangdao.phoenix.service;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.act.ActNotice;
import com.shangdao.phoenix.entity.act.ActNotice.DelayType;
import com.shangdao.phoenix.entity.act.ActRepository;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.entityManager.EntityManagerRepository;
import com.shangdao.phoenix.entity.interfaces.IFile;
import com.shangdao.phoenix.entity.interfaces.ILog;
import com.shangdao.phoenix.entity.interfaces.ILog.DiffEntity;
import com.shangdao.phoenix.entity.interfaces.ILog.DiffItem;
import com.shangdao.phoenix.entity.interfaces.ILog.DiffType;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.state.StateRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.interfaces.INoticeLog;
import com.shangdao.phoenix.entity.interfaces.ITag;
import com.shangdao.phoenix.service.FileUploadService.OssImage;
import com.shangdao.phoenix.util.AbstractPostGetMethodService;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.EntityStructure;
import com.shangdao.phoenix.util.EntityStructure.ColumnStucture;
import com.shangdao.phoenix.util.EntityStructure.JoinType;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;
import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.InsideRuntimeException;
import com.shangdao.phoenix.util.QuartzNoticeJob;
import com.shangdao.phoenix.util.OutsideRuntimeException;
import com.shangdao.phoenix.util.UserDetailsImpl;

@Service
public class PostMethodService extends AbstractPostGetMethodService {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	private StateRepository stateRepository;
	@Autowired
	private ActRepository actRepository;
	@Autowired
	private HttpServletRequest request;
	@Autowired
	private EntityManagerRepository entityManagerRepository;
	@Autowired
	SchedulerFactoryBean schedulerFactoryBean;

	private final static Logger logger = LoggerFactory.getLogger(PostMethodService.class);

	@Transactional
	public HTTPResponse postDispatch(String entityName, String act, String mode, String body) {
		// post 必须带头terminal
		String terminalStr = request.getHeader("terminal");
		if (terminalStr == null) {
			throw new OutsideRuntimeException(8721, "Header缺少terminal参数");
		}

		try {
			Terminal.valueOf(terminalStr);
		} catch (IllegalArgumentException e) {
			throw new OutsideRuntimeException(3311, "terminal参数格式不合法");
		}

		// 封装对象，并格式化数据,并验证
		PostMethodWrapper postMethodWrapper = new PostMethodWrapper(entityName, act, mode, body);

		if (!currentUserCanDo(postMethodWrapper.getStructure(), act, postMethodWrapper.getMode(),
				postMethodWrapper.getId())) {
			if (!isCollectionAct(act) && postMethodWrapper.getStructure().isStateMachineEntity()) {
				throw new OutsideRuntimeException(8767,
						"当前状态下不允许该用户操作:" + act + "/" + postMethodWrapper.getMode().name().toLowerCase());
			} else {
				throw new OutsideRuntimeException(6589,
						"不允许该用户操作：" + act + "/" + postMethodWrapper.getMode().name().toLowerCase());
			}
		}
		System.out.println("成功授权");
		Object oldInstance = null;
		Object postBody = postMethodWrapper.getPostObject();

		if (!act.equals("create")) {
			oldInstance = postMethodWrapper.getStructure().getJpaRepository().findOne(postMethodWrapper.getId());
		}

		// 提交的时候的一系列流程
		long id = _template(postMethodWrapper, act, postBody, oldInstance);
		return new HTTPResponse(id);
	}

	// 用于后台主动触发动作，以某个身份
	@Transactional
	public HTTPResponse postDispatchByUser(String entityName, String act, String mode, String body, User user) {
		Authentication originAuthentication = changeCurrentUser(user);
		HTTPResponse postDispatch = innerPostDispatch(entityName, act, mode, body);
		changeBackCurrentUser(originAuthentication);
		return postDispatch;
	}

	// ------------------------------------------------------------------------------------------------------------

	@Transactional
	private HTTPResponse innerPostDispatch(String entityName, String act, String mode, String body) {
		PostMethodWrapper postMethodWrapper = new PostMethodWrapper(entityName, act, mode, body);
		if (!currentUserCanDo(postMethodWrapper.getStructure(), act, postMethodWrapper.getMode(),
				postMethodWrapper.getId())) {
			if (!isCollectionAct(act) && postMethodWrapper.getStructure().isStateMachineEntity()) {
				throw new OutsideRuntimeException(8767,
						"当前状态下不允许该用户操作:" + act + "/" + postMethodWrapper.getMode().name().toLowerCase());
			} else {
				throw new OutsideRuntimeException(6589,
						"不允许该用户操作：" + act + "/" + postMethodWrapper.getMode().name().toLowerCase());
			}
		}
		Object oldInstance = null;
		Object postBody = postMethodWrapper.getPostObject();

		if (!act.equals("create")) {
			oldInstance = postMethodWrapper.getStructure().getJpaRepository().findOne(postMethodWrapper.getId());
		}

		// 提交的时候的一系列流程
		long id = _template(postMethodWrapper, act, postBody, oldInstance);
		return new HTTPResponse(id);
	}

	private void changeBackCurrentUser(Authentication originAuthentication) {
		SecurityContextHolder.getContext().setAuthentication(originAuthentication);
	}

	private Authentication changeCurrentUser(User user) {
		Authentication originAuthentication = SecurityContextHolder.getContext().getAuthentication();
		Authentication newAuthentication = new Authentication() {
			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return user.getUsername();
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getCredentials() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getDetails() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getPrincipal() {
				// TODO Auto-generated method stub
				return new UserDetailsImpl(user);
			}

			@Override
			public boolean isAuthenticated() {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

			}
		};
		SecurityContextHolder.getContext().setAuthentication(newAuthentication);
		return originAuthentication;

	}

	private long _template(PostMethodWrapper postMethodWrapper, String act, Object postBody, Object oldInstance) {

		// 状态装换,改变postObject的状态为新状态
		_stateMachine(postMethodWrapper, act, postBody, oldInstance);
		// 执行service里的自定义的钩子
		_hook(postMethodWrapper, act, postBody, oldInstance);

		// 保存之前的属性暂时保存在这里，同时diff
		BeforeSave beforeSave = new BeforeSave(postMethodWrapper, act, postBody, oldInstance);
		// 最后保存实体
		Object newInstance = _save(postMethodWrapper, act, postBody, oldInstance);
		// 写入log记录
		ILog _log = _log(postMethodWrapper, act, newInstance, beforeSave);
		// 如果有image和通知，这里处理
		if (_log != null) {
			_images(postMethodWrapper, newInstance, _log);
			_notice(postMethodWrapper, newInstance, _log);
		}
		BeanWrapperImpl newInstanceImpl = new BeanWrapperImpl(newInstance);
		Long id = (Long)newInstanceImpl.getPropertyValue("id");
		return id;
	}

	private void _images(PostMethodWrapper postMethodWrapper, Object newInstance, ILog log) {
		Object postObject = postMethodWrapper.getPostObject();
		BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(postObject);
		List<OssImage> files = (List<OssImage>) beanWrapperImpl.getPropertyValue("uploadFiles");
		if (files != null && !files.isEmpty()) {
			Class<?> fileClass = postMethodWrapper.getStructure().getObjectFields().get("files").getTargetEntity();
			JpaRepository fileRepository = InitService.getStructure(fileClass).getJpaRepository();
			try {
				IFile fileEntity = (IFile) fileClass.newInstance();
				for (OssImage ossImage : files) {
					fileEntity.setAct(log.getAct());
					fileEntity.setCreatedAt(new Date());
					fileEntity.setEntity((ILogEntity) newInstance);
					fileEntity.setFileFormat(ossImage.getFileFormat());
					fileEntity.setFileSize(ossImage.getFileSize());
					fileEntity.setHeight(ossImage.getLarge().getHeight());
					fileEntity.setWidth(ossImage.getLarge().getWidth());
					fileEntity.setLargeImage(ossImage.getLarge().getUrl());
					fileEntity.setLog(log);
					fileEntity.setMiddleImage(ossImage.getMiddle().getUrl());
					fileEntity.setName(ossImage.getOriginalFileName());
					fileEntity.setNewFileName(ossImage.getNewFileName());
					fileEntity.setOriginalFileName(ossImage.getOriginalFileName());
					fileEntity.setSmallImage(ossImage.getSmall().getUrl());
					fileEntity.setUrl(ossImage.getUrl());
					fileRepository.save(fileEntity);
				}

			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void _notice(PostMethodWrapper postMethodWrapper, Object newInstance, ILog log) {

		Scheduler scheduler = schedulerFactoryBean.getScheduler();
		ILogEntity entity = (ILogEntity) newInstance;
		String entityName = postMethodWrapper.getStructure().getName();
		EntityStructure logStructure = InitService.getStructure(log.getClass());
		String logTableName = logStructure.getName();
		Act act = log.getAct();

		try {
			if (act.isCancelOtherNotice()) {
				for (JobKey jobKey : scheduler
						.getJobKeys(GroupMatcher.jobGroupEquals(entityName + "_" + String.valueOf(entity.getId())))) {
					String jobName = jobKey.getName();
					if (jobName.startsWith("cancel_")) {
						scheduler.deleteJob(jobKey);
					}
				}
			}

			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put("entityName", entityName);
			jobDataMap.put("entityId", entity.getId());
			jobDataMap.put("logName", logTableName);
			jobDataMap.put("logId", log.getId());
			jobDataMap.put("actName", act.getName());
			jobDataMap.put("actCode", act.getCode());
			jobDataMap.put("actId", act.getId());
			long time = new Date().getTime();
			long entityId = entity.getId();

			Set<ActNotice> actNotices = act.getActNotices();
			for (ActNotice actNotice : actNotices) {
				_addScheduler(actNotice, jobDataMap, entityName, entityId);
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	private void _fillJobDataMap(ActNotice actNotice, JobDataMap jobDataMap) {

		DelayType delayType = actNotice.getDelayType();
		String roleCode = actNotice.getRoleCode();
		long templateId = actNotice.getNoticeTemplate().getId();

		jobDataMap.put("delayType", delayType.toString());

		if (delayType.equals(DelayType.DELAY)) {
			if (actNotice.isCanBeCancelled()) {
				jobDataMap.put("canBeCancelled", true);
			} else {
				jobDataMap.put("canBeCancelled", false);
			}
		} else {
			jobDataMap.put("canBeCancelled", false);
		}

		if (roleCode == null || roleCode.equals("")) {
			throw new InsideRuntimeException(2371, jobDataMap.getString("entityName") + "的动作："
					+ jobDataMap.getString("actName") + "的，" + delayType + "类通知，的rolecode设置为空");
		}
		jobDataMap.put("roleCode", roleCode);
		if (templateId == 0) {
			throw new InsideRuntimeException(2372, jobDataMap.getString("entityName") + "的动作："
					+ jobDataMap.getString("actName") + "的，" + delayType + "类通知，的模板设置为空");
		}
		jobDataMap.put("templateId", templateId);
	}

	private void _addScheduler(ActNotice actNotice, JobDataMap jobDataMap, String entityName, long entityId) {

		_fillJobDataMap(actNotice, jobDataMap);
		int delayTime = 0;
		if (actNotice.getDelayType().equals(DelayType.DELAY)) {
			delayTime = actNotice.getDelayTime();
		}
		Date startTime = DateBuilder.futureDate(delayTime, IntervalUnit.SECOND);
		String prefix = "";
		if (actNotice.getDelayType().equals(DelayType.IMMEDIATELY)) {
			prefix = "immediately_";
		} else if (actNotice.getDelayType().equals(DelayType.DELAY)) {
			if (actNotice.isCanBeCancelled()) {
				prefix = "cancel_";
			} else {
				prefix = "delay_";
			}
		}
		JobKey jobKey = JobKey.jobKey(prefix + startTime.getTime(), entityName + "_" + String.valueOf(entityId));
		JobDetail jobDetail = JobBuilder.newJob(QuartzNoticeJob.class).setJobData(jobDataMap).withIdentity(jobKey)
				.build();
		Trigger trigger = TriggerBuilder.newTrigger().startAt(startTime).forJob(jobDetail).build();
		Scheduler scheduler = schedulerFactoryBean.getScheduler();
		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private ILog _log(PostMethodWrapper postMethodWrapper, String act, Object newInstance, BeforeSave beforeSave) {
		if (postMethodWrapper.getStructure().isLogEntity()) {

			try {
				String terminalStr = request.getHeader("terminal");
				Terminal terminal = Terminal.valueOf(terminalStr);
				String imei = request.getHeader("imei");
				String ip = request.getRemoteHost();
				Double longitude = null;
				Double latitude = null;
				if (request.getHeader("longitude") != null) {
					longitude = Double.valueOf(request.getHeader("longitude"));
					latitude = Double.valueOf(request.getHeader("latitude"));
				}

				Class<?> logClass = postMethodWrapper.getStructure().getObjectFields().get("logs").getTargetEntity();
				BeanWrapperImpl newInstanceWrapper = new BeanWrapperImpl(newInstance);

				BeanWrapperImpl postBodyWrapper = new BeanWrapperImpl(postMethodWrapper.getPostObject());
				Object noteObject = postBodyWrapper.getPropertyValue("note");
				String note = "";
				if (noteObject != null) {
					note = noteObject.toString();
				}
				JpaRepository logRepository = InitService.getStructure(logClass).getJpaRepository();
				ILog logEntity = (ILog) logClass.newInstance();
				Act actFind = actRepository
						.findByEntityManagerIdAndCode(postMethodWrapper.getStructure().getEntityManagerId(), act);
				logEntity.setAct(actFind);
				if (postMethodWrapper.getStructure().isStateMachineEntity()) {
					logEntity.setBeforeState(beforeSave.getBeforeState());
					logEntity.setAfterState((State) newInstanceWrapper.getPropertyValue("state"));
				}
				ObjectMapper objectMapper = new ObjectMapper();
				String differenceString = objectMapper.writeValueAsString(beforeSave.getDifference());
				logEntity.setDifference(differenceString);
				logEntity.setNote(note);
				logEntity.setCreatedAt(new Date());
				logEntity.setEntity((ILogEntity) newInstance);
				logEntity.setImei(imei);
				logEntity.setIp(ip);
				logEntity.setLatitude(latitude);
				logEntity.setLongitude(longitude);
				logEntity.setName(actFind.getName());
				logEntity.setCreatedBy(CommonUtils.currentUser().getUser());
				logEntity.setTerminal(terminal);
				logEntity.setNote(note);
				ILog save = (ILog) logRepository.save(logEntity);
				return save;

			} catch (IllegalArgumentException | InstantiationException | IllegalAccessException
					| JsonProcessingException e) {
				e.printStackTrace();
				throw new OutsideRuntimeException(1313, "log保存失败");
			}

		}
		return null;

	}

	private void _stateMachine(PostMethodWrapper postMethodWrapper, String act, Object postBody, Object oldInstance) {
		if (act.equals("create")) {
			State stateFind = stateRepository
					.findByEntityManagerIdAndCode(postMethodWrapper.getStructure().getEntityManagerId(), "CREATED");
			if (stateFind == null) {
				throw new InsideRuntimeException(2221,
						"实体" + postMethodWrapper.getStructure().getName() + "的CREATED状态不存在");
			} else {
				BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(postBody);
				beanWrapperImpl.setPropertyValue("state", stateFind);
			}
		} else {
			if (postMethodWrapper.getStructure().isStateMachineEntity()) {
				Act actFind = actRepository
						.findByEntityManagerIdAndCode(postMethodWrapper.getStructure().getEntityManagerId(), act);
				if (actFind == null) {
					throw new InsideRuntimeException(2221,
							"实体" + postMethodWrapper.getStructure().getName() + "的" + act + "动作不存在");
				} else if (actFind.getTargetState() != null) {
					BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(postBody);
					beanWrapperImpl.setPropertyValue("state", actFind.getTargetState());
				}
			}
		}
	}

	private void _hook(PostMethodWrapper postMethodWrapper, String act, Object postBody, Object oldInstance) {
		if (postMethodWrapper.getStructure().getEntityService() != null) {
			try {
				Method declaredMethod = postMethodWrapper.getStructure().getEntityService().getClass()
						.getDeclaredMethod(act, PostMethodWrapper.class, Object.class, Object.class);
				declaredMethod.invoke(postMethodWrapper.getStructure().getEntityService(), postMethodWrapper, postBody,
						oldInstance);
			} catch (NoSuchMethodException | SecurityException e) {

			} catch (IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
				RuntimeException targetException = (RuntimeException) e.getTargetException();
				throw targetException;
			}
		}

	}

	private Object _save(PostMethodWrapper postMethodWrapper, String act, Object postObject, Object oldInstance) {
		Object newInstance = null;
		try {

			BeanWrapperImpl postObjectImpl = new BeanWrapperImpl(postObject);
			if (postMethodWrapper.getStructure().isLogEntity()) {
				postObjectImpl.setPropertyValue("lastModifiedAt", new Date());
			}

			if (postMethodWrapper.getStructure().isTagEntity()) {
				Object tags = postObjectImpl.getPropertyValue("tags");
				if (tags != null) {
					Class<?> tagEntity = postMethodWrapper.getStructure().getObjectFields().get("tags")
							.getTargetEntity();
					Long entityManagerId = InitService.getStructure(tagEntity).getEntityManagerId();
					EntityManager entityManager = entityManagerRepository.findOne(entityManagerId);
					Set<ITag> t = (Set<ITag>) tags;
					for (ITag iTag : t) {
						iTag.setCreatedAt(new Date());
						iTag.setCreatedBy(CommonUtils.currentUser().getUser());
						iTag.setEntityManager(entityManager);
						iTag.setEntity(oldInstance);
					}
				}
			}

			if (act.equals("delete")) {
				postObjectImpl.setPropertyValue("deletedAt", new Date());
			}

			// 创建
			if (oldInstance == null) {
				Set<Department> departments = CommonUtils.currentUser().getDepartments();
				if (postMethodWrapper.getStructure().isProjectEntity()) {
					if (departments != null && !departments.isEmpty())
						postObjectImpl.setPropertyValue("departments", departments);
				}
				
				if(!postMethodWrapper.getStructure().getName().equals("act")  || !postMethodWrapper.getStructure().getName().equals("state")){
					EntityManager entityManager = entityManagerRepository
							.findOne(postMethodWrapper.getStructure().getEntityManagerId());
					postObjectImpl.setPropertyValue("entityManager", entityManager);
				}else{
					EntityManager entityManager = entityManagerRepository
							.findOne(((Act)postObject).getEntityManager().getId());
					postObjectImpl.setPropertyValue("entityManager", entityManager);
				}
				postObjectImpl.setPropertyValue("createdAt", new Date());
				postObjectImpl.setPropertyValue("createdBy", CommonUtils.currentUser().getUser());
				CommonUtils.validate(postObject);
				newInstance = postMethodWrapper.getStructure().getJpaRepository().save(postObject);
			} else {
				CommonUtils.copyPropertiesIgnoreNull(postObject, oldInstance);
				CommonUtils.validate(oldInstance);
				Map<String, ColumnStucture> objectFields = postMethodWrapper.getStructure().getObjectFields();
				Set<Entry<String, ColumnStucture>> entrySet = objectFields.entrySet();
				BeanWrapperImpl oldInstanceImpl = new BeanWrapperImpl(oldInstance);

				for (Entry<String, ColumnStucture> entry : entrySet) {
					try {
						if (entry.getValue().getJoinType().equals(JoinType.ONE_TO_ONE)
								|| entry.getValue().getJoinType().equals(JoinType.MANY_TO_ONE)) {

							Object propertyValue = oldInstanceImpl.getPropertyValue(entry.getKey() + ".id");
							if (propertyValue.equals(0L)) {
								oldInstanceImpl.setPropertyValue(entry.getKey(), null);
							}
						}
					} catch (NullValueInNestedPathException e) {

					}
				}

				newInstance = postMethodWrapper.getStructure().getJpaRepository().save(oldInstance);
			}

		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new OutsideRuntimeException(8611, "数据库保存错误");
		}
		return newInstance;

	}

	@Override
	protected NamedParameterJdbcTemplate getJdbcTemplate() {
		// TODO Auto-generated method stub
		return jdbcTemplate;
	}

	public static class PostMethodWrapper implements Serializable {
		private Long id;
		private EntityStructure structure;
		private String act;
		private Mode mode;
		private Object postObject;

		public PostMethodWrapper(String entityName, String act, String mode, String body) {
			super();
			// System.out.println("entityName:" + entityName);
			// System.out.println("act:" + act);
			// System.out.println("mode:" + mode);
			// System.out.println("body:" + body);
			this.structure = InitService.getStructure(entityName);
			this.act = act;
			if (mode == null) {
				this.mode = Mode.ALL;
			} else {
				try {
					this.mode = Mode.valueOf(mode.toUpperCase());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw new OutsideRuntimeException(6512, "操作模式" + mode + " 无效");
				}
			}
			ObjectMapper mapper = new ObjectMapper();
			try {
				postObject = mapper.readValue(body, structure.getCls());
				BeanWrapperImpl readWrapper = new BeanWrapperImpl(postObject);
				id = (Long) readWrapper.getPropertyValue("id");
				if (act.equals("create")) {
					if (!id.equals(0L)) {
						throw new OutsideRuntimeException(3662, "创建对象不允许带id");
					}
				} else if (id.equals(0L)) {
					throw new OutsideRuntimeException(7761, "结构中缺少id");
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new OutsideRuntimeException(3456, "解析post数据异常");
			}

		}

		public Object getPostObject() {
			return postObject;
		}

		public void setPostObject(Object postObject) {
			this.postObject = postObject;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EntityStructure getStructure() {
			return structure;
		}

		public void setStructure(EntityStructure structure) {
			this.structure = structure;
		}

		public String getAct() {
			return act;
		}

		public void setAct(String act) {
			this.act = act;
		}

		public Mode getMode() {
			return mode;
		}

		public void setMode(Mode mode) {
			this.mode = mode;
		}
	}

	protected static class BeforeSave {
		private List<DiffItem> difference = new ArrayList<DiffItem>();
		private State beforeState;

		public List<DiffItem> getDifference() {
			return difference;
		}

		public void setDifference(List<DiffItem> difference) {
			this.difference = difference;
		}

		public State getBeforeState() {
			return beforeState;
		}

		public void setBeforeState(State beforeState) {
			this.beforeState = beforeState;
		}

		public BeforeSave(PostMethodWrapper postMethodWrapper, String act, Object postBody, Object oldInstance) {
			if (oldInstance != null) {
				BeanWrapperImpl oldInstanceWrapper = new BeanWrapperImpl(oldInstance);
				BeanWrapperImpl postBodyWrapper = new BeanWrapperImpl(postBody);

				if (postMethodWrapper.getStructure().isStateMachineEntity()) {
					this.beforeState = (State) oldInstanceWrapper.getPropertyValue("state");
				}
				if (act.equals("delete")) {
					return;
				}
				Set<String> notNullPropertyNames = CommonUtils.getNotNullPropertyNames(postBody);
				EntityStructure structure = postMethodWrapper.getStructure();
				for (String str : notNullPropertyNames) {
					DiffItem diffItem = new DiffItem();
					diffItem.setName(str);
					if (structure.getSimpleFields().containsKey(str)) {
						Object oldValue = oldInstanceWrapper.getPropertyValue(str);
						Object newValue = postBodyWrapper.getPropertyValue(str);
						if (oldValue == null || !oldValue.toString().equals(newValue.toString())) {
							diffItem.setType(DiffType.STRING);
							diffItem.setOldString(oldValue);
							diffItem.setNewString(newValue);
							difference.add(diffItem);
						}

					} else if (structure.getObjectFields().containsKey(str)) {
						ColumnStucture columnStucture = structure.getObjectFields().get(str);
						if (columnStucture.getJoinType().equals(JoinType.ONE_TO_ONE)
								|| columnStucture.getJoinType().equals(JoinType.MANY_TO_ONE)) {
							diffItem.setType(DiffType.OBJECT);
							String columnEntityName = InitService.getStructure(columnStucture.getTargetEntity())
									.getName();

							Long newTargetId = (Long) postBodyWrapper.getPropertyValue(str + ".id");
							JpaRepository targetJpaRepository = InitService
									.getStructure(columnStucture.getTargetEntity()).getJpaRepository();
							Object newTarget = targetJpaRepository.findOne(newTargetId);
							DiffEntity newEntity = null;
							try {
								if (newTarget == null) {
									Long oldTargetId = (Long) oldInstanceWrapper.getPropertyValue(str + ".id");
									Object oldTarget = targetJpaRepository.findOne(oldTargetId);
									BeanWrapperImpl oldTargetWrapper = new BeanWrapperImpl(oldTarget);
									String oldTargetName = (String) oldTargetWrapper.getPropertyValue("name");
									DiffEntity oldEntity = new DiffEntity();
									oldEntity.setEntity(columnEntityName);
									oldEntity.setId(oldTargetId);
									oldEntity.setName(oldTargetName);
									diffItem.setOldObject(oldEntity);
									diffItem.setNewObject(newEntity);
									difference.add(diffItem);
								} else {
									BeanWrapperImpl newTargetWrapper = new BeanWrapperImpl(newTarget);
									String newTargetName = (String) newTargetWrapper.getPropertyValue("name");
									newEntity = new DiffEntity(columnEntityName, newTargetId, newTargetName);

									Long oldTargetId = (Long) oldInstanceWrapper.getPropertyValue(str + ".id");
									DiffEntity oldEntity = new DiffEntity();
									oldEntity.setEntity(columnEntityName);
									oldEntity.setId(oldTargetId);
									if (!newTargetId.equals(oldTargetId)) {
										Object oldTarget = targetJpaRepository.findOne(oldTargetId);
										BeanWrapperImpl oldTargetWrapper = new BeanWrapperImpl(oldTarget);
										String oldTargetName = (String) oldTargetWrapper.getPropertyValue("name");
										oldEntity.setName(oldTargetName);
										diffItem.setOldObject(oldEntity);
										diffItem.setNewObject(newEntity);
										difference.add(diffItem);
									}
								}
							} catch (NullValueInNestedPathException e) {
								diffItem.setNewObject(newEntity);
								difference.add(diffItem);
							}

						} else if (columnStucture.getJoinType().equals(JoinType.ONE_TO_MANY)
								|| columnStucture.getJoinType().equals(JoinType.MANY_TO_MANY)) {

							diffItem.setType(DiffType.LIST);
							String entity = InitService.getStructure(columnStucture.getTargetEntity()).getName();

							Set postBodySet = (Set) postBodyWrapper.getPropertyValue(str);
							Set newTargetIdSet = (Set) postBodySet.stream()
									.map(e -> (new BeanWrapperImpl(e).getPropertyValue("id")))
									.collect(Collectors.toSet());
							JpaRepository jpaRepository = InitService.getStructure(columnStucture.getTargetEntity())
									.getJpaRepository();

							Set oldSet = (Set) oldInstanceWrapper.getPropertyValue(str);
							Set oldTargetIdSet = (Set) oldSet.stream()
									.map(e -> (new BeanWrapperImpl(e).getPropertyValue("id")))
									.collect(Collectors.toSet());

							Set decreaseIdSet = (Set) oldTargetIdSet.stream().filter(e -> !newTargetIdSet.contains(e))
									.collect(Collectors.toSet());
							Set increaseIdSet = (Set) newTargetIdSet.stream().filter(e -> !oldTargetIdSet.contains(e))
									.collect(Collectors.toSet());

							List decreaseAll = jpaRepository.findAll(decreaseIdSet);
							List increaseAll = jpaRepository.findAll(increaseIdSet);
							for (Object object : increaseAll) {
								BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(object);
								Long id = (Long) beanWrapperImpl.getPropertyValue("id");
								String name = (String) beanWrapperImpl.getPropertyValue("name");
								DiffEntity diffEntity = new DiffEntity();
								diffEntity.setEntity(entity);
								diffEntity.setId(id);
								diffEntity.setName(name);
								diffItem.getIncreaseObject().add(diffEntity);
							}
							for (Object object : decreaseAll) {
								BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(object);
								Long id = (Long) beanWrapperImpl.getPropertyValue("id");
								String name = (String) beanWrapperImpl.getPropertyValue("name");
								DiffEntity diffEntity = new DiffEntity();
								diffEntity.setEntity(entity);
								diffEntity.setId(id);
								diffEntity.setName(name);
								diffItem.getDecreaseObject().add(diffEntity);
							}
							if (diffItem.getDecreaseObject().isEmpty() && diffItem.getIncreaseObject().isEmpty()) {
								continue;
							}
							difference.add(diffItem);
						}

					}
				}
			}
		}
	}
}
