package com.shangdao.phoenix.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.act.ActRepository;
import com.shangdao.phoenix.entity.department.DepartmentRepository;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.entityManager.EntityManager.ManagerGroup;
import com.shangdao.phoenix.entity.entityManager.EntityManagerRepository;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.state.StateRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.User.Source;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.EntityStructure;
import com.shangdao.phoenix.util.EntityStructure.ColumnStucture;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;
import com.shangdao.phoenix.util.InsideRuntimeException;
import com.shangdao.phoenix.util.UserDetailsImpl;

@Service
public class InitService {

	private final static Logger logger = LoggerFactory.getLogger(InitService.class);

	@Autowired
	private List<JpaRepository> repositories;

	@Autowired
	private EntityManagerRepository entityManagerRepository;

	@Autowired
	private StateRepository stateRepository;

	@Autowired
	private ActRepository actRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DepartmentRepository departmentRepository;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private WorkWeixinContactService weixinService;

	private static HashSet<String> stopwordSet = new HashSet<>();

	private final static HashMap<String, EntityStructure> nameToStructure = new HashMap();

	private final static HashMap<Class<?>, EntityStructure> classToStructure = new HashMap();

	private Role developerRole;

	private Role guestRole;

	private Role anonymousRole;

	private static UserDetailsImpl systemUser;

	private static UserDetailsImpl anonymousUser;

	@Value("${salt}")
	private String salt;

	@Value("${work.weixin.enable}")
	private boolean workEnable;

	public static boolean containStopWord(String word) {
		return stopwordSet.contains(word.toUpperCase());
	}

	public static EntityStructure getStructure(String name) {
		if (nameToStructure.containsKey(name)) {
			return nameToStructure.get(name);
		} else {
			String first = name.substring(0, 1);
			if (!first.equals(first.toLowerCase())) {
				throw new InsideRuntimeException("实体:" + name + "的首字母小写");
			}
			throw new InsideRuntimeException("没有这个实体：" + name);
		}
	}

	public static EntityStructure getStructure(Class<?> clz) {
		if (classToStructure.containsKey(clz)) {
			return classToStructure.get(clz);
		} else {
			throw new InsideRuntimeException("没有这个实体：" + clz.getSimpleName());
		}
	}

	public static UserDetailsImpl getSystemUser() {
		return systemUser;
	}

	public static UserDetailsImpl getAnonymousUser() {
		return anonymousUser;
	}
	// --------------------------------------------------------------------------

	@PostConstruct
	private void init() {
		// 必须有超级管理员，匿名，普通的角色
		long roleCount = roleRepository.count();
		if (roleCount == 0) {
			Role developerRole = new Role();
			developerRole.setCode("DEVELOPER");
			developerRole.setName("开发者");
			developerRole.setCreatedAt(new Date());
			roleRepository.save(developerRole);
			// 匿名用户
			Role anonymousRole = new Role();
			anonymousRole.setCode("ANONYMOUS");
			anonymousRole.setName("匿名用户");
			anonymousRole.setCreatedAt(new Date());
			roleRepository.save(anonymousRole);
			// 和普通用户
			Role guestRole = new Role();
			guestRole.setCode("GUEST");
			guestRole.setName("普通登录用户");
			guestRole.setCreatedAt(new Date());
			roleRepository.save(guestRole);

			this.developerRole = developerRole;
			this.anonymousRole = anonymousRole;
			this.guestRole = guestRole;
		} else {
			this.developerRole = roleRepository.findOne(1L);
			this.anonymousRole = roleRepository.findOne(2L);
			this.guestRole = roleRepository.findOne(3L);
		}

		// 构建停止词集合
		String[] splitWords = EntityStructure.stopWords.split("\\s");
		for (String word : splitWords) {
			stopwordSet.add(word);
		}

		for (JpaRepository jpaRepository : repositories) {
			ResolvableType resolvableType = ResolvableType.forClass(jpaRepository.getClass());
			Class<?> entityClass = resolvableType.as(JpaRepository.class).getGeneric(0).resolve();
			logger.info("jpaRepository的泛型实体名字：" + entityClass.getName());
			// 在内存中检查并构建entity的结构
			entityStructureCheckAndBuild(entityClass, jpaRepository);

			// 如果通过把这个entity加入EntityManager数据库中，并自动生成初始数据库数据
			registerEntityManager(entityClass);
		}

		// 第一个用户为系统本身，角色为开发者
		long userCount = userRepository.count();
		if (userCount == 0) {
			EntityManager userEntityManager = entityManagerRepository.findByName("user");
			EntityManager roleEntityManager = entityManagerRepository.findByName("role");
			User system = new User();
			system.setUsername("root");
			system.setName("系统");
			system.setSource(Source.API);
			system.setCreatedAt(new Date());
			system.setEntityManager(userEntityManager);
			HashSet<Role> roleSet = new HashSet<Role>();
			roleSet.add(developerRole);
			system.setRoles(roleSet);
			userRepository.save(system);

			// 第二个用户为全局匿名，角色为匿名
			User anonymous = new User();
			anonymous.setUsername("anonymous");
			anonymous.setName("匿名");
			anonymous.setSource(Source.API);
			anonymous.setCreatedAt(new Date());
			anonymous.setEntityManager(userEntityManager);
			HashSet<Role> roleSet2 = new HashSet<Role>();
			roleSet2.add(anonymousRole);
			anonymous.setRoles(roleSet2);
			userRepository.save(anonymous);
			
			// 第三个用户为开发者
			User admin = new User();
			admin.setUsername("developer");
			admin.setName("开发者");
			admin.setPassword(CommonUtils.MD5Encode("123456", salt));
			admin.setSource(Source.WEB);
			admin.setCreatedAt(new Date());
			admin.setEntityManager(userEntityManager);
			HashSet<Role> roleSet3 = new HashSet<Role>();
			roleSet3.add(developerRole);
			admin.setRoles(roleSet3);
			userRepository.save(admin);
			

			// 第一次启动注册角色的entitymanager
			developerRole.setEntityManager(roleEntityManager);
			roleRepository.save(developerRole);
			guestRole.setEntityManager(roleEntityManager);
			roleRepository.save(guestRole);
			anonymousRole.setEntityManager(roleEntityManager);
			roleRepository.save(anonymousRole);
			

		}

		// 第一次注册企业微信内的企业
		if (workEnable) {
			long departmentCount = departmentRepository.count();
			if (departmentCount == 0) {
				weixinService.setupDepartments();
				weixinService.setupUsers();
//				weixinService.setupRoles();
			}
		}

		this.anonymousUser = new UserDetailsImpl(userRepository.findOne(2L));
		this.systemUser = new UserDetailsImpl(userRepository.findOne(1L));

	}

	private void registerEntityManager(Class<?> entityClass) {

		EntityStructure entityStructure = getStructure(entityClass);
		EntityManager findByName = entityManagerRepository.findByName(entityStructure.getName());
		if (findByName == null) {
			EntityManager entityManager = new EntityManager();
			if(entityStructure.getName().equals("act") || entityStructure.getName().equals("state")|| entityStructure.getName().equals("noticeTemplate")
					||entityStructure.getName().equals("role")||entityStructure.getName().equals("entityManager")
					||entityStructure.getName().equals("menu")||entityStructure.getName().equals("department")||entityStructure.getName().equals("message")
					||entityStructure.getName().equals("user")||entityStructure.getName().equals("tag")||entityStructure.getName().equals("userInfo")
					||entityStructure.getName().endsWith("File") || entityStructure.getName().endsWith("Log")
					||entityStructure.getName().endsWith("Tag")||entityStructure.getName().endsWith("Notice")
					){
				entityManager.setManagerGroup(ManagerGroup.DEVELOPER);
			}else{
				entityManager.setManagerGroup(ManagerGroup.ADMIN);
			}
			
			EntityManager entity = updateEntityIntefaceInfo(entityManager, entityStructure);
			entityStructure.setEntityManagerId(entity.getId());

			
			// 初始化实体的状态和动作，并关联
			HashMap<String, State> initStates = initStates(entity);
			HashMap<String, Act> initActs = initActs(entity);
			if (entityStructure.isStateMachineEntity()) {
				linkStateAndAct(initStates.get("CREATED"), initActs);
			}

			// 给所有动作设置超级管理员默认允许
			HashSet<Role> roleSet = new HashSet<Role>();
			roleSet.add(developerRole);
			Collection<Act> values = initActs.values();
			for (Act act : values) {
				act.setRoles(roleSet);
				actRepository.save(act);
			}
		} else {
			EntityManager entity = updateEntityIntefaceInfo(findByName, entityStructure);
			entityStructure.setEntityManagerId(findByName.getId());
		}
	}

	private EntityManager updateEntityIntefaceInfo(EntityManager entityManager, EntityStructure entityStructure) {
		entityManager.setName(entityStructure.getName());
		entityManager.setHasProject(entityStructure.isProjectEntity());
		entityManager.setHasStateMachine(entityStructure.isStateMachineEntity());
		entityManager.setHasTag(entityStructure.isTagEntity());
		entityManager.setHasLog(entityStructure.isLogEntity());
		entityManager.setCreatedAt(new Date());
		return entityManagerRepository.save(entityManager);
	}

	private void linkStateAndAct(State state, HashMap<String, Act> acts) {
		Set<Act> collect = acts.values().stream().collect(Collectors.toSet());
		HashSet<State> hashSet = new HashSet<State>();
		hashSet.add(state);
		for (Act act : collect) {
			act.setStates(hashSet);
			actRepository.save(act);
		}
	}

	private HashMap<String, State> initStates(EntityManager entityManager) {
		HashMap<String, State> map = new HashMap<String, State>();
		State state1 = new State();
		state1.setEntityManager(entityManager);
		state1.setCode("CREATED");
		state1.setName("已创建");
		state1.setIconCls("icon-stopwatch_start");
		state1.setCreatedAt(new Date());
		State createdState = stateRepository.save(state1);

		State state2 = new State();
		state2.setEntityManager(entityManager);
		state2.setCreatedAt(new Date());
		state2.setCode("FINISHED");
		state2.setName("已完结");
		state2.setIconCls("icon-flag_finish");
		State finishedState = stateRepository.save(state2);

		map.put("CREATED", createdState);
		map.put("FINISHED", finishedState);
		return map;

	}

	private HashMap<String, Act> initActs(EntityManager entityManager) {
		HashMap<String, Act> map = new HashMap<String, Act>();

		Act act1 = new Act();
		act1.setEntityManager(entityManager);
		act1.setCode("create");
		act1.setName("创建");
		act1.setIconCls("icon-add");
		act1.setCreatedAt(new Date());
		act1.setAllCan(true);
		Act createAct = actRepository.save(act1);

		Act act2 = new Act();
		act2.setEntityManager(entityManager);
		act2.setCode("update");
		act2.setName("编辑");
		act2.setIconCls("icon-update");
		act2.setCreatedAt(new Date());
		act2.setAllCan(true);
		Act updateAct = actRepository.save(act2);

		Act act3 = new Act();
		act3.setEntityManager(entityManager);
		act3.setCode("delete");
		act3.setName("删除");
		act3.setIconCls("icon-delete");
		act3.setCreatedAt(new Date());
		act3.setAllCan(true);
		Act deleteAct = actRepository.save(act3);

		Act act4 = new Act();
		act4.setEntityManager(entityManager);
		act4.setCode("detail");
		act4.setName("详细");
		act4.setIconCls("icon-document_image");
		act4.setCreatedAt(new Date());
		act4.setAllCan(true);
		Act detailAct = actRepository.save(act4);

		Act act5 = new Act();
		act5.setEntityManager(entityManager);
		act5.setCode("list");
		act5.setName("列表");
		act5.setIconCls("icon-table");
		act5.setCreatedAt(new Date());
		act5.setAllCan(true);
		Act listAct = actRepository.save(act5);

		Act act6 = new Act();
		act6.setEntityManager(entityManager);
		act6.setCode("tree");
		act6.setName("树形列表");
		act6.setIconCls("icon-tree_list");
		act6.setCreatedAt(new Date());
		act6.setAllCan(true);
		Act treeAct = actRepository.save(act6);

		Act act7 = new Act();
		act7.setEntityManager(entityManager);
		act7.setCode("group");
		act7.setName("分组列表");
		act7.setIconCls("icon-table");
		act7.setCreatedAt(new Date());
		act7.setAllCan(true);
		Act groupAct = actRepository.save(act7);

		map.put("group", act7);
		map.put("tree", act6);
		map.put("list", act5);
		map.put("detail", act4);
		map.put("delete", act3);
		map.put("update", act2);
		map.put("create", act1);
		return map;
	}

	// 实体类的注解检查
	private void entityStructureCheckAndBuild(Class<?> entityClass, JpaRepository jpaRepository) {
		Table tableAnnotation = entityClass.getDeclaredAnnotation(Table.class);
		if (tableAnnotation == null || "".equals(tableAnnotation.name())) {
			throw new InsideRuntimeException("实体类  " + entityClass.getSimpleName() + " 缺少带name属性的table注解");
		}
		if (stopwordSet.contains(tableAnnotation.name().toUpperCase())) {
			throw new InsideRuntimeException("实体类不允许用关键词 " + tableAnnotation.name() + " 做表名");
		}

		// 加入结构
		EntityStructure entityStructure = new EntityStructure();
		entityStructure.setCls(entityClass);
		entityStructure.setJpaRepository(jpaRepository);
		entityStructure.setTableName(tableAnnotation.name());
		entityStructure.setName(
				entityClass.getSimpleName().substring(0, 1).toLowerCase() + entityClass.getSimpleName().substring(1));
		// 检查每个属性
		Field[] declaredFields = entityClass.getDeclaredFields();
		for (Field field : declaredFields) {
			entityFieldCheck(field, entityStructure);
		}

		nameToStructure.put(
				entityClass.getSimpleName().substring(0, 1).toLowerCase() + entityClass.getSimpleName().substring(1),
				entityStructure);
		classToStructure.put(entityClass, entityStructure);

	}

	// 属性检查，并加入结构中
	private void entityFieldCheck(Field field, EntityStructure entityStructure) {

		// 普通属性
		Column columnAnnotation = field.getDeclaredAnnotation(Column.class);
		if (columnAnnotation != null) {
			if ("".equals(columnAnnotation.name())) {
				throw new InsideRuntimeException("实体：" + field.getDeclaringClass().getSimpleName() + "的属性"
						+ field.getName() + "的@Column注解没有name");
			}
			ColumnStucture column = new ColumnStucture(EntityStructure.Format.SIMPLE, null, false, field.getType(),
					entityStructure.getTableName(), null, null, columnAnnotation.name());
			entityStructure.getSimpleFields().put(field.getName(), column);
			return;
		}

		// manytoone
		ManyToOne manyToOneAnnotation = field.getDeclaredAnnotation(ManyToOne.class);
		if (manyToOneAnnotation != null) {
			JoinColumn joinColumnAnnotation = field.getDeclaredAnnotation(JoinColumn.class);
			if (joinColumnAnnotation == null || "".equals(joinColumnAnnotation.name())) {
				throw new InsideRuntimeException("实体：" + field.getDeclaringClass().getSimpleName() + "的属性"
						+ field.getName() + "，没有@JoinColumn注解，或者注解没有设置name");
			}
			ColumnStucture column = new ColumnStucture(EntityStructure.Format.OBJECT,
					EntityStructure.JoinType.MANY_TO_ONE, false, field.getType(), entityStructure.getTableName(),
					joinColumnAnnotation.name(), null, null);
			entityStructure.getObjectFields().put(field.getName(), column);
			return;
		}

		// manytomany
		ManyToMany manyToManyAnnotation = field.getDeclaredAnnotation(ManyToMany.class);
		if (manyToManyAnnotation != null) {
			try {
				ResolvableType resolvableType = ResolvableType.forField(field);
				Class<?> resolve = resolvableType.getGeneric(0).resolve();

				if ("".equals(manyToManyAnnotation.mappedBy())) {
					JoinTable joinTableAnnotation = field.getDeclaredAnnotation(JoinTable.class);
					if (joinTableAnnotation == null || "".equals(joinTableAnnotation.name())
							|| joinTableAnnotation.joinColumns().length == 0
							|| joinTableAnnotation.inverseJoinColumns().length == 0) {
						throw new InsideRuntimeException("实体：" + field.getDeclaringClass().getSimpleName() + "的属性"
								+ field.getName() + "，没有@JoinTable注解，或者注解设置不全");
					}
					ColumnStucture column = new ColumnStucture(EntityStructure.Format.OBJECT,
							EntityStructure.JoinType.MANY_TO_MANY, false, resolve, joinTableAnnotation.name(),
							joinTableAnnotation.joinColumns()[0].name(),
							joinTableAnnotation.inverseJoinColumns()[0].name(), null);
					entityStructure.getObjectFields().put(field.getName(), column);
				} else {

					Field otherField;
					otherField = resolve.getDeclaredField(manyToManyAnnotation.mappedBy());
					String joinTable = otherField.getDeclaredAnnotation(JoinTable.class).name();
					String joinColumn = otherField.getDeclaredAnnotation(JoinTable.class).inverseJoinColumns()[0]
							.name();
					String inverseJoinColumn = otherField.getDeclaredAnnotation(JoinTable.class).joinColumns()[0]
							.name();
					ColumnStucture column = new ColumnStucture(EntityStructure.Format.OBJECT,
							EntityStructure.JoinType.MANY_TO_MANY, true, resolve, joinTable, joinColumn,
							inverseJoinColumn, null);
					entityStructure.getObjectFields().put(field.getName(), column);

				}
			} catch (NoSuchFieldException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		// onetoone
		OneToOne oneToOneAnnotation = field.getDeclaredAnnotation(OneToOne.class);
		if (oneToOneAnnotation != null) {
			if ("".equals(oneToOneAnnotation.mappedBy())) {
				JoinColumn joinColumnAnnotation = field.getDeclaredAnnotation(JoinColumn.class);
				if (joinColumnAnnotation == null || "".equals(joinColumnAnnotation.name())) {
					throw new InsideRuntimeException("实体：" + field.getDeclaringClass().getSimpleName() + "的属性"
							+ field.getName() + "，没有@JoinColumn注解，或者没有设置name");
				}

				ColumnStucture column = new ColumnStucture(EntityStructure.Format.OBJECT,
						EntityStructure.JoinType.ONE_TO_ONE, false, field.getType(), entityStructure.getTableName(),
						joinColumnAnnotation.name(), null, null);
				entityStructure.getObjectFields().put(field.getName(), column);
			} else {
				try {
					Field otherField;
					otherField = field.getType().getDeclaredField(oneToOneAnnotation.mappedBy());
					String joinTable = field.getType().getDeclaredAnnotation(Table.class).name();
					String joinColumn = otherField.getDeclaredAnnotation(JoinColumn.class).name();

					ColumnStucture column = new ColumnStucture(EntityStructure.Format.OBJECT,
							EntityStructure.JoinType.ONE_TO_ONE, true, field.getType(), joinTable, joinColumn, null,
							null);
					entityStructure.getObjectFields().put(field.getName(), column);
				} catch (NoSuchFieldException | SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return;
		}

		// onetomany
		OneToMany oneToManyAnnotation = field.getDeclaredAnnotation(OneToMany.class);
		if (oneToManyAnnotation != null) {
			if ("".equals(oneToManyAnnotation.mappedBy())) {
				throw new InsideRuntimeException(
						"实体：" + field.getDeclaringClass().getSimpleName() + "的属性" + field.getName() + "，没有设置mappedBy");
			}
			try {
				Field otherField;

				ResolvableType resolvableType = ResolvableType.forField(field);
				Class<?> resolve = resolvableType.getGeneric(0).resolve();
				otherField = resolve.getDeclaredField(oneToManyAnnotation.mappedBy());

				String joinTable = resolve.getDeclaredAnnotation(Table.class).name();
				String joinColumn = otherField.getDeclaredAnnotation(JoinColumn.class).name();

				ColumnStucture column = new ColumnStucture(EntityStructure.Format.OBJECT,
						EntityStructure.JoinType.ONE_TO_MANY, true, resolve, joinTable, joinColumn, null, null);
				entityStructure.getObjectFields().put(field.getName(), column);
			} catch (NoSuchFieldException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return;
		}

		// transient
		Transient transientAnnotation = field.getDeclaredAnnotation(Transient.class);
		if (transientAnnotation != null) {
			return;
		} else {
			throw new InsideRuntimeException(
					"实体：" + field.getDeclaringClass().getSimpleName() + "的属性" + field.getName() + "没有设置正确");
		}

	}
}
