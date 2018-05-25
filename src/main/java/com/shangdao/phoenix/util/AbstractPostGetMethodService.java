package com.shangdao.phoenix.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.interfaces.ILog.DiffItem;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.util.EntityStructure.ColumnStucture;
import com.shangdao.phoenix.util.EntityStructure.JoinType;

public abstract class AbstractPostGetMethodService {

	private final static Logger logger = LoggerFactory.getLogger(AbstractPostGetMethodService.class);
	
	protected abstract NamedParameterJdbcTemplate getJdbcTemplate();
	
	protected boolean currentUserCanDo(EntityStructure structure, String act, Mode mode, Long id) {
		String modeStr = mode.toString().toLowerCase()+"_can";
		String selectSql = "";
		String whereSql = "";
		if(mode.equals(Mode.ALL)||isCollectionAct(act)){
			if(isCollectionAct(act) || !structure.isStateMachineEntity()){
				selectSql = "select act.* from user_role,act_role,act ";
				whereSql = " where user_role.user_id = :userId and user_role.role_id = act_role.role_id and act_role.act_id = act.id and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and act."+ modeStr +" = 1";
			}else{
				selectSql = "select act.* from user_role,act_role,act,state_act," + structure.getTableName();
				whereSql = " where user_role.user_id = :userId and user_role.role_id = act_role.role_id and act_role.act_id = act.id and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and " + structure.getTableName() + ".id = :id" + " and act."+ modeStr +" = 1";
			}
		}else if(mode.equals(Mode.MANAGER)){
			if(!structure.isProjectEntity()){
				throw new InsideRuntimeException("不是项目类型的实体");
			}
			if( !structure.isStateMachineEntity()){
				selectSql = "select act.* from act, "+ structure.getTableName();
				whereSql = " where  " + structure.getTableName() + ".id = :id and "+ structure.getTableName()+".manager_id = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and act."+ modeStr +" = 1";
			}else{
				selectSql = "select act.* from act,state_act," + structure.getTableName();
				whereSql = " where " + structure.getTableName() + ".id = :id and " + structure.getTableName()+".manager_id = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and act."+ modeStr +" = 1";
			}
		}else if(mode.equals(Mode.CREATOR)){
			if(!structure.isStateMachineEntity()){
				selectSql = "select act.* from act, "+ structure.getTableName();
				whereSql = " where  " + structure.getTableName() + ".id = :id and "+ structure.getTableName()+".created_by = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and act."+ modeStr +" = 1";
			}else{
				selectSql = "select act.* from act,state_act," + structure.getTableName();
				whereSql = " where " + structure.getTableName() + ".id = :id and " + structure.getTableName()+".created_by = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and act."+ modeStr +" = 1";
			}
		}else if(mode.equals(Mode.MEMBER)){
			if(!structure.isProjectEntity()){
				throw new InsideRuntimeException("不是项目类型的实体");
			}
			if(!structure.isStateMachineEntity()){
				selectSql = "select act.* from act,"+structure.getObjectFields().get("members").getJoinTable();
				whereSql = " where  " + structure.getObjectFields().get("members").getJoinTable() + ".entity_id = :id and "+ structure.getObjectFields().get("members").getJoinTable()+".user_id = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and act."+ modeStr +" = 1";
			}else{
				selectSql = "select act.* from act,state_act," + structure.getTableName() + ","+structure.getObjectFields().get("members").getJoinTable();
				whereSql = " where  " + structure.getTableName() + ".id = :id and " + structure.getObjectFields().get("members").getJoinTable() + ".entity_id = :id and "+ structure.getObjectFields().get("members").getJoinTable()+".user_id = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and act."+ modeStr +" = 1";
			}
		}else if(mode.equals(Mode.SUBSCRIBER)){
			if(!structure.isProjectEntity()){
				throw new InsideRuntimeException("不是项目类型的实体");
			}
			if(!structure.isStateMachineEntity()){
				selectSql = "select act.* from act,"+structure.getObjectFields().get("subscribers").getJoinTable();
				whereSql = " where  " + structure.getObjectFields().get("subscribers").getJoinTable() + ".entity_id = :id and "+ structure.getObjectFields().get("subscribers").getJoinTable()+".user_id = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and act."+ modeStr +" = 1";
			}else{
				selectSql = "select act.* from act,state_act," + structure.getTableName() + ","+structure.getObjectFields().get("subscribers").getJoinTable();
				whereSql = " where  " + structure.getTableName() + ".id = :id and " + structure.getObjectFields().get("subscribers").getJoinTable() + ".entity_id = :id and "+ structure.getObjectFields().get("subscribers").getJoinTable()+".user_id = :userId and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and act."+ modeStr +" = 1";
			}
		}else if(mode.equals(Mode.DEPARTMENT)){
			if(!structure.isProjectEntity()){
				throw new InsideRuntimeException("不是项目类型的实体");
			}
			
			if(!structure.isStateMachineEntity()){
				selectSql = "select act.* from act,"+structure.getObjectFields().get("departments").getJoinTable();
				whereSql = " where  " + structure.getObjectFields().get("departments").getJoinTable() + ".entity_id = :id and "+ structure.getObjectFields().get("departments").getJoinTable()+".department_id in (:dids) and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and act."+ modeStr +" = 1";
			}else{
				selectSql = "select act.* from act,state_act," + structure.getTableName() + ","+structure.getObjectFields().get("departments").getJoinTable();
				whereSql = " where  " + structure.getTableName() + ".id = :id and "+ structure.getObjectFields().get("departments").getJoinTable() + ".entity_id = :id and "+ structure.getObjectFields().get("departments").getJoinTable()+".department_id in (:dids) and act.code = :operate and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and act."+ modeStr +" = 1";
			}
		}

		Set<Department> departments = CommonUtils.currentUser().getDepartments();
		ArrayList<Long> dids = new ArrayList<Long>();
		for (Department d : departments) {
			dids.add(d.getId());
		}
		
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		hashMap.put("operate", act);
		hashMap.put("userId", CommonUtils.currentUser().getId());
		hashMap.put("id", id);
		hashMap.put("dids", dids);
		try{
			System.out.println(hashMap);
			System.out.println(selectSql+whereSql);
			getJdbcTemplate().queryForMap(selectSql+whereSql+" limit 1", hashMap);
		}catch (EmptyResultDataAccessException e){
			return false;
		}
		return true;
	}
	
	
	protected Object currentUserCanDoAct(EntityStructure structure, Long id) {
		String selectSql = "";
		String whereSql = "";
			if( !structure.isStateMachineEntity()){
				selectSql = "select act.* from user_role,act_role,act ";
				whereSql = " where user_role.user_id = :userId and user_role.role_id = act_role.role_id and act_role.act_id = act.id  and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " order by act.sort_number asc";
			}else{
				selectSql = "select act.* from user_role,act_role,act,state_act," + structure.getTableName();
				whereSql = " where user_role.user_id = :userId and user_role.role_id = act_role.role_id and act_role.act_id = act.id and act.entity_manager_id= "
						+ structure.getEntityManagerId() + " and state_act.act_id = act.id and state_act.state_id = "
						+ structure.getTableName() + ".state_id and " + structure.getTableName() + ".id = :id" +  " order by act.sort_number asc";
			}


			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			hashMap.put("userId", CommonUtils.currentUser().getId());
			hashMap.put("id", id);
			System.out.println(selectSql+whereSql);
			List<Map<String, Object>> queryForList = getJdbcTemplate().queryForList(selectSql+whereSql, hashMap);
			EntityStructure actStructure = InitService.getStructure(Act.class);
			Object transformAsList = transformAsList(queryForList, actStructure);
			return transformAsList;
	}
	
	
	
	protected boolean isCollectionAct(String act){
		if(act.equals("create") ||  act.equals("tree") || act.equals("group")||act.equals("list")){
			return true;
		}else{
			return false;
		}
	}
	
	
	protected Map<String, Object> prepareParameters(Map<String, String> params) {

		HashMap<String, Object> ret = new HashMap<String, Object>();

		if (params != null && !params.isEmpty()) {
			Set<Entry<String, String>> entrySet = params.entrySet();
			for (Entry<String, String> entry : entrySet) {
				String value = entry.getValue();
				String key = entry.getKey();
				if ("sort".equals(key) || "page".equals(key) || "fields".equals(key) || "size".equals(key)) {
					continue;
				}
				if (entry.getKey().endsWith(">")) {
					ret.put(key.replace(">", "]"), value);
				} else if (entry.getKey().endsWith("<")) {
					ret.put(key.replace("<", "["), value);
				} else if (entry.getKey().endsWith("~")) {
					ret.put(key, "%" + value + "%");
				} else if (entry.getKey().endsWith("@")) {
					String[] split = value.split(",");
					List<String> asList = Arrays.asList(split);
					ret.put(key, asList);
				} else if (entry.getKey().endsWith("$")) {
					String[] split = value.split(",");
					List<String> asList = Arrays.asList(split);
					ret.put(key, asList);
				} else {
					ret.put(key, value);
				}
			}

		}
		return ret;
	}

	protected List transformAsTree(List<Map<String, Object>> queryForList, EntityStructure nametostructure) {
		List list = (List) transformAsList(queryForList, nametostructure);
		
		System.out.println(list);
		
		return TreeBuilder.bulid(list);
	}

	protected Object transformAsList(List<Map<String, Object>> queryForList, EntityStructure nametostructure) {

		LinkedHashMap<Long, Object> mapList = new LinkedHashMap<>();
		
		for (Map<String, Object> row : queryForList) {
			rowToMapListFirstMerge(row, mapList, nametostructure);
		}
		
		for (Map<String, Object> row : queryForList) {
			rowToMapListSecondMerge(row, mapList, nametostructure);
		}
		return mapListToMap(mapList,nametostructure);
	}

	protected void rowToMapListFirstMerge(Map<String, Object> row, LinkedHashMap<Long, Object> mapList,
			EntityStructure nametostructure) {

		Object old = mapList.get(row.get("id"));
		if (old == null) {
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			mapList.put((Long) row.get("id"), hashMap);
			_merge(hashMap, row, nametostructure);
		} else {
			_merge((HashMap<String, Object>) old, row, nametostructure);
		}
	}

	protected void rowToMapListSecondMerge(Map<String, Object> row, LinkedHashMap<Long, Object> mapList,
			EntityStructure nametostructure) {
		Set<Entry<String, Object>> entrySet = row.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			String key = entry.getKey();
			String[] split = key.split("\\.");
			if (split.length == 3) {
				Object mapLv1 = mapList.get((Long) row.get("id"));
				ColumnStucture columnStucture = nametostructure.getObjectFields().get(split[0]);
				Map mapLv2;
				if (columnStucture.getJoinType().equals(JoinType.MANY_TO_MANY) || columnStucture.getJoinType().equals(JoinType.ONE_TO_MANY)) {
					mapLv2 = (Map) ((Map) ((Map) mapLv1).get(split[0])).get((Long) row.get(split[0] + ".id"));
				} else {
					mapLv2 = (Map) ((Map) mapLv1).get(split[0]);
				}

				if(mapLv2 == null){
					continue;
				}
				Map mapLv3=null;
				ColumnStucture columnStucture2 = InitService.getStructure(columnStucture.getTargetEntity()).getObjectFields().get(split[1]);
				if (columnStucture2.getJoinType().equals(JoinType.MANY_TO_MANY) || columnStucture2.getJoinType().equals(JoinType.ONE_TO_MANY)) {
					
						mapLv3 = (Map) mapLv2.getOrDefault(split[1], new LinkedHashMap<Long, Object>());
						mapLv2.put(split[1], mapLv3);
						Object mapLv4 = mapLv3.getOrDefault(row.get(split[0] + "." + split[1] + ".id"),
								new HashMap<String, Object>());
						((Map) mapLv4).put(split[2], entry.getValue());
						if (row.get(split[0] + "." + split[1] + ".id") != null) {
							mapLv3.put(row.get(split[0] + "." + split[1] + ".id"), mapLv4);
						}
					
				} else {
				
						mapLv3 = (Map) ((Map) mapLv2).getOrDefault(split[1], new HashMap<String, Object>());
						mapLv3.put(split[2], entry.getValue());
						mapLv2.put(split[1], mapLv3);
					
				
				}
				
			}
		}

	}

	protected Object mapListToMap(Object o, EntityStructure nametostructure) {
		if (o instanceof List) {
			ArrayList<Object> arrayList = new ArrayList<Object>();
			for (Object i : (List) o) {
				arrayList.add(mapListToMap(i,nametostructure));
			}
			return arrayList;
		} else if (o instanceof Map) {
			Map origin = (Map) o;
			Set keySet = origin.keySet();
			Iterator iterator = keySet.iterator();
			if (iterator.hasNext()) {
				Object next = iterator.next();
				if (next instanceof Long || next instanceof Integer) {
					ArrayList<Map> arrayList = new ArrayList<Map>();
					arrayList.addAll(origin.values());
					return mapListToMap(arrayList,nametostructure);

				} else {
					HashMap hashMap = new HashMap();
					Set<Entry<String, Object>> entrySet = origin.entrySet();
					for (Entry<String, Object> entry : entrySet) {
						
						ColumnStucture columnStucture = nametostructure.getObjectFields().get(entry.getKey());
						if(columnStucture!=null){
							EntityStructure structure = InitService.getStructure(columnStucture.getTargetEntity());
							hashMap.put(entry.getKey(), mapListToMap(entry.getValue(),structure));
						}else{
							if(nametostructure.isLog() && entry.getKey().equals("difference")){
								ObjectMapper objectMapper = new ObjectMapper();
								try {
									List<DiffItem> li = objectMapper.readValue((String)entry.getValue(), new TypeReference<List<DiffItem>>(){});
									hashMap.put(entry.getKey(), li);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}else{
								hashMap.put(entry.getKey(), mapListToMap(entry.getValue(),nametostructure));
							}
						}
						
					}
					return hashMap;
				}
			}
		} else {
			return o;
		}
		return o;
	}

	protected void _merge(HashMap<String, Object> old, Map<String, Object> row, EntityStructure nametostructure) {

		Set<Entry<String, Object>> entrySet = row.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			String key = entry.getKey();
			String[] split = key.split("\\.");
			if (split.length == 1) {
					old.put(key, entry.getValue());
			} else if (split.length == 2) {
				ColumnStucture columnStucture = nametostructure.getObjectFields().get(split[0]);
				if (columnStucture.getJoinType().equals(JoinType.MANY_TO_MANY)
						|| columnStucture.getJoinType().equals(JoinType.ONE_TO_MANY)) {
					Object mapListLv1 = old.getOrDefault(split[0], new LinkedHashMap<>());
					old.put(split[0], mapListLv1);
					Object mapLv2 = ((Map) mapListLv1).getOrDefault(row.get(split[0] + ".id"), new HashMap<>());
					((Map) mapLv2).put(split[1], entry.getValue());
					if (row.get(split[0] + ".id") != null) {
						((Map) mapListLv1).putIfAbsent(row.get(split[0] + ".id"), mapLv2);
					}
				} else {
					Object mapLv1 = old.getOrDefault(split[0], new LinkedHashMap<>());
					if(entry.getValue()!=null){
						((Map) mapLv1).put(split[1], entry.getValue());
					}
					old.put(split[0], mapLv1);
				}
			}
		}
	}

	protected StartAndSize countStartAndSizeOfList(List<Map<String, Object>> queryForList, Pageable page) {
		if (queryForList == null) {
			return null;
		}

		long pageNumber = page.getPageNumber();
		long pageSize = page.getPageSize();

		if (pageNumber * pageSize >= queryForList.size()) {
			return null;
		}

		long start = 0;
		long size = 0;
		long p = 0;
		for (Map<String, Object> map : queryForList) {
			long row = (long) map.get("COUNT(*)");
			if (p < pageNumber * pageSize) {
				p++;
				start += row;
			} else if (p >= pageNumber * pageSize && p < pageNumber * pageSize + pageSize) {
				p++;
				size += row;
			} else {
				break;
			}
		}

		StartAndSize startAndSize = new StartAndSize();
		if(size>5000){
			throw new OutsideRuntimeException(3421, "一次查询数据量太大");
		}
		startAndSize.setSize(size);
		startAndSize.setStart(start);
		return startAndSize;

	}

	public static class StartAndSize {
		private long start;
		private long size;

		public long getStart() {
			return start;
		}

		public void setStart(long start) {
			this.start = start;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		@Override
		public String toString() {
			return "StartAndSize [start=" + start + ", size=" + size + "]";
		}

	}

	public enum Mode {
		ALL, CREATOR, DEPARTMENT, MEMBER, MANAGER, SUBSCRIBER
	}

}
