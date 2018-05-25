package com.shangdao.phoenix.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.shangdao.phoenix.controller.IndexController;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.util.AbstractPostGetMethodService;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.EntityStructure;
import com.shangdao.phoenix.util.HTTPDetailResponse;
import com.shangdao.phoenix.util.HTTPListResponse;
import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.InsideRuntimeException;
import com.shangdao.phoenix.util.MysqlBuilder;
import com.shangdao.phoenix.util.OutsideRuntimeException;

@Service
public class GetMethodService extends AbstractPostGetMethodService {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private PostMethodService postMethodService;

	private final static Logger logger = LoggerFactory.getLogger(GetMethodService.class);

	public HTTPResponse getDispatch(String entityName, String act, String mode, final Map<String, String> params,
			Pageable page) {

		// 封装对象，并格式化数据
		GetMethodWrapper getMethodWrapper = new GetMethodWrapper(entityName, act, mode, params, page);
		if (!currentUserCanDo(getMethodWrapper.getStructure(), act, getMethodWrapper.getMode(),
				getMethodWrapper.getId())) {
			throw new OutsideRuntimeException(8511,
					"不允许该用户操作：" + entityName + "/" + act + "/" + getMethodWrapper.getMode().name().toLowerCase());
		}
		System.out.println("成功授权");

		// 根据访问模式，添加需要的参数
		addParametersByMode(getMethodWrapper);

		HTTPResponse response;
		if (act.equals("list")) {
			response = _hook(getMethodWrapper, "list", list(getMethodWrapper));
		} else if (act.equals("tree")) {
			response = _hook(getMethodWrapper, "tree", tree(getMethodWrapper));
		} else if (act.equals("detail")) {
			response = _hook(getMethodWrapper, "detail", detail(getMethodWrapper));
		} else if (act.equals("group")) {
			response = _hook(getMethodWrapper, "group", group(getMethodWrapper));
		} else {
			HTTPResponse hook = _hook(getMethodWrapper, act, null);
			if (hook == null) {
				throw new OutsideRuntimeException(7651, "GET方法不支持" + mode.toLowerCase());
			} else {
				return hook;
			}
		}

		return response;
	}

	public HTTPResponse myInfo() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("id", String.valueOf(CommonUtils.currentUser().getId()));
		params.put("fields", "*,state,departments,roles.menus");
		HTTPResponse userResponse = getDispatch("user", "detail", "all", params, null);
		List<Map> roles = (List<Map>) ((Map) userResponse.getData()).get("roles");
		List menuIds = (List) roles.stream().flatMap(GetMethodService::menuList).distinct()
				.map(e -> ((Map) e).get("id").toString()).collect(Collectors.toList());
		roles.stream().forEach(e -> ((Map) e).remove("menus"));
		HashMap<String, String> menuparams = new HashMap<String, String>();
		menuparams.put("id@", String.join(",", menuIds));
		HTTPResponse menuResponse = getDispatch("menu", "tree", "all", menuparams,
				new GetMethodService.MaxItemOnePage());
		Map userData = (Map) userResponse.getData();
		userData.put("menus", menuResponse.getData());
		userData.remove("currentUserCanDo");
		userData.remove("password");
		return userResponse;
	}

	// -------------------------------------------------------------------------------------------------------------------

	private static Stream menuList(Map m) {
		Object menus = m.get("menus");
		if (menus instanceof List) {
			return ((List) menus).stream();
		}
		return Collections.EMPTY_LIST.stream();
	}

	private HTTPResponse _hook(GetMethodWrapper getMethodWrapper, String act, HTTPResponse response) {

		if (getMethodWrapper.getStructure().getEntityService() != null) {
			try {
				Method declaredMethod = getMethodWrapper.getStructure().getEntityService().getClass()
						.getDeclaredMethod(act, GetMethodWrapper.class, HTTPResponse.class);
				return (HTTPResponse) declaredMethod.invoke(getMethodWrapper.getStructure().getEntityService(),
						getMethodWrapper, response);
			} catch (NoSuchMethodException | SecurityException e) {
				return response;
			} catch (IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
				return response;
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
				RuntimeException targetException = (RuntimeException) e.getTargetException();
				throw targetException;
			}
		}
		return response;
	}

	private void addParametersByMode(GetMethodWrapper getMethodObject) {
		Map<String, String> params = getMethodObject.getParams();
		Mode mode = getMethodObject.getMode();
		EntityStructure structure = getMethodObject.getStructure();

		if (mode.equals(Mode.ALL)) {

		} else if (mode.equals(Mode.DEPARTMENT)) {
			if (structure.isProjectEntity()) {
				Set<Department> departments = CommonUtils.currentUser().getDepartments();
				ArrayList<String> departmentIds = new ArrayList<String>();
				for (Department department : departments) {
					departmentIds.add(String.valueOf(department.getId()));
				}
				String ids = String.join(",", departmentIds);
				params.put("departments.id@", ids);
			} else {
				throw new InsideRuntimeException("模型" + structure.getName() + "没有实现ProjectEntity接口");
			}
		} else if (mode.equals(Mode.MANAGER)) {
			if (structure.isProjectEntity()) {
				params.put("manager.id", String.valueOf(CommonUtils.currentUser().getId()));
			} else {
				throw new InsideRuntimeException("模型" + structure.getName() + "没有实现ProjectEntity接口");
			}
		} else if (mode.equals(Mode.CREATOR)) {
			if (structure.isProjectEntity()) {
				params.put("createdBy.id", String.valueOf(CommonUtils.currentUser().getId()));
			} else {
				throw new InsideRuntimeException("模型" + structure.getName() + "没有实现BaseEntity接口");
			}
		} else if (mode.equals(Mode.MEMBER)) {
			if (structure.isProjectEntity()) {
				params.put("members.id", String.valueOf(CommonUtils.currentUser().getId()));
			} else {
				throw new InsideRuntimeException("模型" + structure.getName() + "没有实现ProjectEntity接口");
			}
		} else if (mode.equals(Mode.SUBSCRIBER)) {
			if (structure.isProjectEntity()) {
				params.put("subscribers.id", String.valueOf(CommonUtils.currentUser().getId()));
			} else {
				throw new InsideRuntimeException("模型" + structure.getName() + "没有实现ProjectEntity接口");
			}
		}

	}

	private HTTPListResponse list(GetMethodWrapper getMethodWrapper) {
		Map<String, String> params = getMethodWrapper.getParams();
		Pageable page = getMethodWrapper.getPage();

		Map<String, Object> preparedParams = prepareParameters(params);
		System.out.println("处理过的参数");
		System.out.println(preparedParams);

		MysqlBuilder builder = new MysqlBuilder().setFields(params.get("fields")).setConditions(preparedParams)
				.setName(getMethodWrapper.getStructure().getName()).setPage(page).setCacheManager(cacheManager).build();
		System.out.println(builder.getSqlCouple().getCountSql());
		System.out.println(builder.getSqlCouple().getFindSql());
		List<Map<String, Object>> queryForCount = jdbcTemplate.queryForList(builder.getSqlCouple().getCountSql(),
				preparedParams);
		int total = queryForCount.size();
		StartAndSize countStartAndSizeOfList = countStartAndSizeOfList(queryForCount, page);
		System.out.println(countStartAndSizeOfList);
		if (countStartAndSizeOfList == null) {
			return new HTTPListResponse(Collections.EMPTY_LIST, total, page.getPageSize(), page.getPageNumber());
		} else {
			preparedParams.put("pageStart", countStartAndSizeOfList.getStart());
			preparedParams.put("pageSize", countStartAndSizeOfList.getSize());

			System.out.println("最终参数值");
			System.out.println(preparedParams);

			List<Map<String, Object>> queryForList = jdbcTemplate.queryForList(builder.getSqlCouple().getFindSql(),
					preparedParams);
			return new HTTPListResponse(transformAsList(queryForList, getMethodWrapper.getStructure()), total,
					page.getPageSize(), page.getPageNumber());
		}
	}

	private HTTPListResponse tree(GetMethodWrapper getMethodWrapper) {

		Map<String, String> params = getMethodWrapper.getParams();
		Pageable page = getMethodWrapper.getPage();

		Map<String, Object> preparedParams = prepareParameters(params);
		System.out.println("处理过的参数");
		System.out.println(preparedParams);

		EntityStructure structure = getMethodWrapper.getStructure();
		if (!structure.getObjectFields().containsKey("parent")) {
			throw new InsideRuntimeException("树形结构，必须包含parent属性");
		}
		MysqlBuilder builder = new MysqlBuilder().setFields(params.get("fields")).setConditions(preparedParams)
				.setPage(page).setName(structure.getName()).setIsTree(true).setCacheManager(cacheManager).build();
		System.out.println(builder.getSqlCouple().getCountSql());
		System.out.println(builder.getSqlCouple().getFindSql());

		List<Map<String, Object>> queryForCount = jdbcTemplate.queryForList(builder.getSqlCouple().getCountSql(),
				preparedParams);
		System.out.println(queryForCount);
		int total = queryForCount.size();
		StartAndSize countStartAndSizeOfList = countStartAndSizeOfList(queryForCount, page);
		HashMap<String, Object> ret = new HashMap<String, Object>();
		if (countStartAndSizeOfList == null) {
			return new HTTPListResponse(Collections.EMPTY_LIST, 0, page.getPageSize(), page.getPageNumber());
		} else {
			preparedParams.put("pageStart", countStartAndSizeOfList.getStart());
			preparedParams.put("pageSize", countStartAndSizeOfList.getSize());
			List<Map<String, Object>> queryForList = jdbcTemplate.queryForList(builder.getSqlCouple().getFindSql(),
					preparedParams);
			Object transformAsTree = transformAsTree(queryForList, structure);
			return new HTTPListResponse(transformAsTree, total, page.getPageSize(), page.getPageNumber());
		}
	}

	private HTTPDetailResponse detail(GetMethodWrapper getMethodWrapper) {

		Map<String, String> params = getMethodWrapper.getParams();
		Pageable detailPage = new DetailPage(getMethodWrapper);

		Map<String, Object> preparedParams = prepareParameters(params);
		String fields = params.get("fields");
		if (getMethodWrapper.getStructure().isLogEntity()) {
			if (fields == null) {
				fields = "files.act,logs.afterState,logs.beforeState,logs.act,logs.notices";
			} else {
				fields += ",files.act,logs.afterState,logs.beforeState,logs.act";
			}
		}

		System.out.println("处理过的参数");
		System.out.println(preparedParams);

		// 开始查询
		MysqlBuilder builder = new MysqlBuilder().setFields(fields).setConditions(preparedParams)
				.setName(getMethodWrapper.getStructure().getName()).setPage(detailPage).setCacheManager(cacheManager)
				.build();
		System.out.println(builder.getSqlCouple().getCountSql());
		System.out.println(builder.getSqlCouple().getFindSql());
		List<Map<String, Object>> queryForCount = jdbcTemplate.queryForList(builder.getSqlCouple().getCountSql(),
				preparedParams);
		int total = queryForCount.size();
		StartAndSize countStartAndSizeOfList = countStartAndSizeOfList(queryForCount, detailPage);
		System.out.println(countStartAndSizeOfList);
		if (countStartAndSizeOfList == null) {
			return new HTTPDetailResponse(Collections.EMPTY_MAP);
		} else {
			preparedParams.put("pageStart", countStartAndSizeOfList.getStart());
			preparedParams.put("pageSize", countStartAndSizeOfList.getSize());
			System.out.println("最终参数值");
			System.out.println(preparedParams);
			List<Map<String, Object>> queryForList = jdbcTemplate.queryForList(builder.getSqlCouple().getFindSql(),
					preparedParams);

			Map detail = (Map) ((List) transformAsList(queryForList, getMethodWrapper.getStructure())).get(0);

			// 查询结果处理

			if (getMethodWrapper.getStructure().isLogEntity()) {

				// 处理文件，把files变成groupfiles
				Object files = ((Map) detail).remove("files");
				if (files != null && !(files instanceof LinkedHashMap) && !((List) files).isEmpty()) {
					List collect = (List) ((List) files).stream()
							.sorted(Comparator.comparing(GetMethodService::comparingByFileAct))
							.collect(Collectors.toList());
					LinkedHashMap<Object, Object> linkedHashMap = new LinkedHashMap<>();
					for (Object o : collect) {
						String groupValue = ((Map) ((Map) o).get("act")).get("name").toString();
						Object orDefault = linkedHashMap.getOrDefault(groupValue, new ArrayList<>());
						((List) orDefault).add(o);
						if (!linkedHashMap.containsKey(groupValue)) {
							linkedHashMap.put(groupValue, orDefault);
						}
					}

					detail.put("groupFiles", linkedHashMap);

				}

				// 如果是状态机,在detail中加入stateActFlow对象
				if (getMethodWrapper.getStructure().isStateMachineEntity()) {
					
					Object Logs =  detail.get("logs");
					List logList;
					if (Logs != null && !(Logs instanceof LinkedHashMap) && !((List) Logs).isEmpty()) {
						logList = (List) detail.get("logs");
					}else{
						logList = new ArrayList();
					}
					
					HashMap<String, Object> stateActParams = new HashMap<>();
					stateActParams.put("id", getMethodWrapper.getStructure().getEntityManagerId());
					MysqlBuilder stateActsBuilder = new MysqlBuilder().setFields("states,states.acts")
							.setConditions(stateActParams).setName("entityManager").setPage(new OneItemOnePage())
							.setCacheManager(cacheManager).build();
					stateActParams.put("pageStart", 0);
					stateActParams.put("pageSize", 5000);
					List<Map<String, Object>> queryForStateActs = jdbcTemplate
							.queryForList(stateActsBuilder.getSqlCouple().getFindSql(), stateActParams);
					Map entityManager = (Map) ((List) transformAsList(queryForStateActs,
							InitService.getStructure("entityManager"))).get(0);
					List stateActFlow = (List) entityManager.get("states");
					boolean end = false;
					for (Object object : logList) {
						Map act = (Map) ((Map) object).get("act");
						Map beforeState = null;
						if (!"create".equals(act.get("code").toString())) {
							beforeState = (Map) ((Map) object).get("beforeState");
						}
						Map afterState = (Map) ((Map) object).get("afterState");
						if (afterState.get("code")!=null && afterState.get("code").toString().equals("FINISHED")) {
							end = true;
						}
						fillStateActFlow(stateActFlow, beforeState, afterState, act, end);
					}
					detail.put("stateActFlow", sorted(stateActFlow));
				}

			}
			// 根据当前用户的角色权限，在detail中加入currentUserCanDo
			detail.put("currentUserCanDo",
					currentUserCanDoAct(getMethodWrapper.getStructure(), getMethodWrapper.getId()));

			return new HTTPDetailResponse(detail);
		}
	}

	private HTTPListResponse group(GetMethodWrapper getMethodWrapper) {

		Map<String, String> params = getMethodWrapper.getParams();
		Pageable page = getMethodWrapper.getPage();
		Map<String, Object> preparedParams = prepareParameters(params);
		if (preparedParams.get("group") == null) {
			throw new OutsideRuntimeException(2333, "group接口必须包含group参数");
		}
		String group = preparedParams.remove("group").toString();
		System.out.println("处理过的参数");
		System.out.println(preparedParams);

		MysqlBuilder builder = new MysqlBuilder().setFields(params.get("fields")).setConditions(preparedParams)
				.setName(getMethodWrapper.getStructure().getName()).setPage(page).setCacheManager(cacheManager).build();
		System.out.println(builder.getSqlCouple().getCountSql());
		System.out.println(builder.getSqlCouple().getFindSql());
		List<Map<String, Object>> queryForCount = jdbcTemplate.queryForList(builder.getSqlCouple().getCountSql(),
				preparedParams);
		int total = queryForCount.size();
		StartAndSize countStartAndSizeOfList = countStartAndSizeOfList(queryForCount, page);
		System.out.println(countStartAndSizeOfList);
		HashMap<String, Object> ret = new HashMap<String, Object>();
		if (countStartAndSizeOfList == null) {
			return new HTTPListResponse(Collections.EMPTY_MAP, 0, page.getPageSize(), page.getPageNumber());
		} else {
			preparedParams.put("pageStart", countStartAndSizeOfList.getStart());
			preparedParams.put("pageSize", countStartAndSizeOfList.getSize());

			System.out.println("最终参数值");
			System.out.println(preparedParams);

			List<Map<String, Object>> queryForList = jdbcTemplate.queryForList(builder.getSqlCouple().getFindSql(),
					preparedParams);

			String[] split = group.split("\\.");
			LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
			try {
				List temp = ((List) transformAsList(queryForList, getMethodWrapper.getStructure()));
				if (split.length == 1) {
					for (Object o : temp) {
						String groupValue = ((Map) o).get(group).toString();
						Object orDefault = linkedHashMap.getOrDefault(groupValue, new ArrayList<>());
						((List) orDefault).add(o);
						if (!linkedHashMap.containsKey(groupValue)) {
							linkedHashMap.put(groupValue, orDefault);
						}
					}

				} else if (split.length == 2) {

					for (Object o : temp) {
						if (((Map) o).get(split[0]) instanceof List) {
							throw new InsideRuntimeException("分组对象" + split[0] + "不允许是列表");
						}
						if (((Map) ((Map) o).get(split[0])) == null) {
							continue;
						}
						if (((Map) o).get(split[0]) != null && ((Map) ((Map) o).get(split[0])).get(split[1])!=null) {
							String groupValue = ((Map) ((Map) o).get(split[0])).get(split[1]).toString();
							Object orDefault = linkedHashMap.getOrDefault(groupValue, new ArrayList<>());

							((List) orDefault).add(o);
							if (!linkedHashMap.containsKey(groupValue)) {
								linkedHashMap.put(groupValue, orDefault);
							}
						} else {
							String groupValue = " ";
							Object orDefault = linkedHashMap.getOrDefault(groupValue, new ArrayList<>());

							((List) orDefault).add(o);
							if (!linkedHashMap.containsKey(groupValue)) {
								linkedHashMap.put(groupValue, orDefault);
							}
						}
					}
				}
			} catch (ClassCastException e) {
				e.printStackTrace();
				throw new InsideRuntimeException(1299, split[0] + "不是对象");
			} catch (NullPointerException e) {
				e.printStackTrace();
				throw new InsideRuntimeException(1299, "fields中必须包含group的列，且值不能为null");
			}
			return new HTTPListResponse(linkedHashMap, total, page.getPageSize(), page.getPageNumber());
		}
	}

	private void fillStateActFlow(List stateActFlow, Map beforeState, Map afterState, Map act, boolean end) {
		for (Object s : stateActFlow) {
			Map state = (Map) s;
			if (state.get("code")!=null && state.get("code").toString().equals("FINISHED") && end) {
				state.put("done", true);
			}
			if (beforeState != null && state.get("id").toString().equals(beforeState.get("id").toString())) {
				if (beforeState.get("id")!=null && afterState.get("id")!=null && !beforeState.get("id").toString().equals(afterState.get("id").toString())) {
					state.put("done", true);
				}
				if (state.get("acts") != null) {
					if (state.get("acts") instanceof Map) {
						continue;
					}
					for (Object a : (Collection) state.get("acts")) {
						Map action = (Map) a;
						if (action.get("id").toString().equals(act.get("id").toString())) {
							action.put("done", true);
						}
					}
				}
			}
		}
	}

	//
	private List sorted(List stateActFlow) {
		for (Object object : stateActFlow) {
			Object acts = ((Map) object).get("acts");
			if (acts instanceof List) {
				((Map) object).put("acts",
						((List) acts).stream().sorted(Comparator.comparing(GetMethodService::comparingBySortNumber))
								.collect(Collectors.toList()));
			}
		}
		return (List) stateActFlow.stream().sorted(Comparator.comparing(GetMethodService::comparingBySortNumber))
				.collect(Collectors.toList());

	}

	private static Integer comparingBySortNumber(Map<String, Object> map) {
		if (map.get("sortNumber") instanceof Integer) {
			return (Integer) map.get("sortNumber");
		} else {
			return Integer.valueOf((String) map.get("sortNumber"));
		}
	}

	private static Integer comparingByFileAct(Map<String, Object> map) {
		if (((Map) map.get("act")).get("sortNumber") instanceof Integer) {
			return (Integer) (((Map) map.get("act")).get("sortNumber"));
		} else {
			return Integer.valueOf((String) ((Map) map.get("act")).get("sortNumber"));
		}
	}

	@Override
	protected NamedParameterJdbcTemplate getJdbcTemplate() {
		// TODO Auto-generated method stub
		return jdbcTemplate;
	}

	public static class MaxItemOnePage extends OneItemOnePage {

		@Override
		public int getPageSize() {
			// TODO Auto-generated method stub
			return 5000;
		}
	}

	public static class OneItemOnePage implements Pageable {

		@Override
		public int getPageNumber() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getPageSize() {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public int getOffset() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Sort getSort() {
			return null;
		}

		@Override
		public Pageable next() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Pageable previousOrFirst() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Pageable first() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasPrevious() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	public static class DetailPage implements Pageable {

		private GetMethodWrapper wrapper;
		private Pageable page;

		public DetailPage(GetMethodWrapper wrapper) {
			this.page = wrapper.getPage();
			this.wrapper = wrapper;
		}

		@Override
		public int getPageNumber() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getPageSize() {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public int getOffset() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Sort getSort() {
			// TODO Auto-generated method stub
			if (page == null) {
				return null;
			}
			Sort sort = page.getSort();
			if (wrapper.getStructure().isLogEntity()) {
				Order order = new Sort.Order(Sort.Direction.DESC, "logs.id");
				Sort newSort = new Sort(order);
				if (sort != null) {
					sort.and(newSort);
				} else {
					sort = newSort;
				}
			}
			return sort;
		}

		@Override
		public Pageable next() {
			// TODO Auto-generated method stub
			return page.next();
		}

		@Override
		public Pageable previousOrFirst() {
			// TODO Auto-generated method stub
			return page.previousOrFirst();
		}

		@Override
		public Pageable first() {
			// TODO Auto-generated method stub
			return page.first();
		}

		@Override
		public boolean hasPrevious() {
			// TODO Auto-generated method stub
			return page.hasPrevious();
		}

	}

	public static class GetMethodWrapper {
		private Long id = 0L;
		private EntityStructure structure;
		private String act;
		private Mode mode;
		private Map<String, String> params;
		private Pageable page;

		public GetMethodWrapper(String entityName, String act, String mode, Map<String, String> params, Pageable page) {
			super();
			System.out.println("entityName:" + entityName);
			System.out.println("act:" + act);
			System.out.println("mode:" + mode);
			System.out.println("page" + page);
			System.out.println("参数:" + params);

			this.structure = InitService.getStructure(entityName);
			this.act = act;
			if (mode == null) {
				this.mode = Mode.ALL;
			} else {
				try {
					this.mode = Mode.valueOf(mode.toUpperCase());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw new OutsideRuntimeException(6251, "操作模式" + mode + " 无效");
				}
			}

			if (params != null && params.get("id") != null) {
				try {
					id = Long.valueOf(params.get("id"));
				} catch (NumberFormatException e) {
					e.printStackTrace();
					throw new OutsideRuntimeException(2525, "id格式错误");
				}
			}
			this.params = params;
			this.page = page;
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

		public Map<String, String> getParams() {
			return params;
		}

		public void setParams(Map<String, String> params) {
			this.params = params;
		}

		public Pageable getPage() {
			return page;
		}

		public void setPage(Pageable page) {
			this.page = page;
		}

	}
}
