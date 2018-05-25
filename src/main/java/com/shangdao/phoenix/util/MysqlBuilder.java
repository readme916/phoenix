package com.shangdao.phoenix.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.util.EntityStructure.ColumnStucture;
import com.shangdao.phoenix.util.EntityStructure.JoinType;

public class MysqlBuilder {

	private final static Logger logger = LoggerFactory.getLogger(MysqlBuilder.class);

	private CacheManager cacheManager;

	private Set<String> fields = new TreeSet<>();
	private Map<String, Object> conditions = new TreeMap<>();
	private String name;
	private Pageable page;
	private EntityStructure table;

	private InnerJoin innerJoin = new InnerJoin();;
	private LeftJoin leftJoin = new LeftJoin(innerJoin);
	private Select select = new Select();
	private From from;
	private Where where = new Where(innerJoin);
	private OrderBy orderBy = new OrderBy(innerJoin, leftJoin);;
	private GroupBy groupBy;
	private Limit limit;
	private boolean isTree = false;

	private SqlCouple sqlCouple = new SqlCouple();

	public SqlCouple getSqlCouple() {
		return sqlCouple;
	}

	public MysqlBuilder setIsTree(boolean isTree) {
		this.isTree = isTree;
		return this;
	}

	public MysqlBuilder setConditions(Map<String, Object> conditions) {
		this.conditions.putAll(conditions);
		return this;
	}

	public MysqlBuilder setConditions(String key, String value) {
		String k = key;
		if (key.endsWith(">")) {
			k = key.replace(">", "]");
		} else if (key.endsWith("<")) {
			k = key.replace("<", "[");
		}
		this.conditions.put(k, value);
		return this;
	}

	public MysqlBuilder setFields(String fields) {
		// select
		if (fields == null || "".equals(fields)) {
			this.fields.add("*");
			return this;
		}
		String[] split = fields.split(",");
		for (String column : split) {
			this.fields.add(column);
		}
		
		return this;
	}

	public MysqlBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public MysqlBuilder setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
		return this;
	}

	public MysqlBuilder setPage(Pageable page) {
		this.page = page;
		return this;
	}

	public MysqlBuilder build() {
		SqlCouple sqlCouple = getCache();
		if (sqlCouple != null) {
			logger.info("缓存命中");
			logger.info(sqlCouple.getFindSql());
			this.sqlCouple = sqlCouple;
		} else {
			logger.info("缓存未命中,开始分析url,并生成sql");
			this.fields.add("id");
			checkStopWords(fields);
			this.table = InitService.getStructure(name);
			this.from = new From(table);
			this.where.setName(name);

			Set<Entry<String, Object>> entrySet = conditions.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				where.add(table, entry.getKey(), conditions);
			}
			System.out.println(isTree);
			if (isTree) {
				this.fields.add("parent");
			}
			for (String field : this.fields) {
				leftJoin.add(table, field, "", name);
				select.add(table, field, "");
			}
			if (page != null && page.getSort() != null) {
				orderBy.add(table, page);
			}
			this.limit = new Limit();

			boolean defaultOrder=false;
			for (String ex : orderBy.exists) {
				if(ex.contains("`" + name + "`")){
					defaultOrder=true;
				}
			}
			if(defaultOrder==false){
				orderBy.orders.add("`" + name + "`.id DESC");
			}
		
			String findSql = select.sql() + from.sql() + innerJoin.sql() + leftJoin.sql() + where.sql() + orderBy.sql()
					+ limit.sql();
			this.sqlCouple.setFindSql(findSql);

			this.groupBy = new GroupBy(name);
			String countSql = "SELECT COUNT(*) " + from.sql() + innerJoin.sql() + leftJoin.sql() + where.sql()
					+ groupBy.sql() + orderBy.sql();
			this.sqlCouple.setCountSql(countSql);
			putCache(this.sqlCouple);
		}
		return this;
	}

	// --------------------------------------------------------------------------------------------------------------

	private void checkStopWords(Map<String, String> params) {
		Set<Entry<String, String>> entrySet = params.entrySet();
		for (Entry<String, String> entry : entrySet) {
			_check(entry.getKey());
		}
	}

	private void checkStopWords(Set<String> columns) {
		for (String column : columns) {
			_check(column);
		}
	}

	private void _check(String word) {
		if (word.contains(".")) {
			String[] split = word.split("\\.");
			if (split.length > 2) {
				throw new InsideRuntimeException("参数" + word + "不允许多级嵌套");
			}
			for (String s : split) {
				if (InitService.containStopWord(s)) {
					throw new InsideRuntimeException("url参数中包含sql关键词" + s + "，不允许访问");
				}
			}
		} else {
			if (InitService.containStopWord(word)) {
				throw new InsideRuntimeException("url参数中包含sql关键词" + word + "，不允许访问");
			}
		}
	}

	private SqlCouple getCache() {
		String key = cacheKey();
		// ValueOperations<String, SqlCouple> valueops =
		// redisTemplate.opsForValue();
		// return valueops.get(key);
		return cacheManager.getCache("mysqlBuilder").get(key, SqlCouple.class);
	}

	private void putCache(SqlCouple sql) {
		String key = cacheKey();
		cacheManager.getCache("mysqlBuilder").put(key, sql);
	}

	private String cacheKey() {
		TreeSet<String> treeSet = new TreeSet<>();
		treeSet.add("db." + name);
		treeSet.addAll(conditions.keySet());

		for (String column : fields) {
			treeSet.add("field." + column);
		}

		if (page != null && page.getSort() != null) {
			Iterator<Order> iterator = page.getSort().iterator();
			while (iterator.hasNext()) {
				Order next = iterator.next();
				String property = next.getProperty();
				String direction = next.getDirection().toString();
				treeSet.add(property + "," + direction);
			}
		}
		return String.join("&", treeSet);
	}

	private static class Select implements Expression {
		private HashSet<String> selects = new HashSet<String>();

		public void add(EntityStructure entityStructure, String column, String preColumn) {

			String[] split = column.split("\\.");

			if (split.length == 1) {

				String fullName;
				if ("".equals(preColumn)) {
					fullName = column;
				} else {
					fullName = preColumn + "." + column;
				}

				if ("*".equals(column)) {
					Map<String, ColumnStucture> simpleFields = entityStructure.getSimpleFields();
					Set<Entry<String, ColumnStucture>> entrySet = simpleFields.entrySet();

					if (preColumn.equals("")) {
						for (Entry<String, ColumnStucture> entry : entrySet) {
							selects.add("`" + entityStructure.getName() + "`." + entry.getValue().getColumn() + " AS `"
									+ entry.getKey() + "`");
						}
					} else {
						for (Entry<String, ColumnStucture> entry : entrySet) {
							selects.add("`" + preColumn + "`." + entry.getValue().getColumn() + " AS `" + preColumn
									+ "." + entry.getKey() + "`");
						}
					}

				} else {
					if (entityStructure.getObjectFields().containsKey(column)) {
						ColumnStucture columnStucture = entityStructure.getObjectFields().get(column);
						EntityStructure targetEntityStructure = InitService
								.getStructure(columnStucture.getTargetEntity());
						Set<Entry<String, ColumnStucture>> entrySet = targetEntityStructure.getSimpleFields()
								.entrySet();
						for (Entry<String, ColumnStucture> entry : entrySet) {
							selects.add("`" + fullName + "`" + "." + entry.getValue().getColumn() + " AS `" + fullName
									+ "." + entry.getKey() + "`");
						}
					} else if (entityStructure.getSimpleFields().containsKey(column)) {
						ColumnStucture columnStucture = entityStructure.getSimpleFields().get(column);
						if (preColumn.equals("")) {
							selects.add("`" + entityStructure.getName() + "`." + columnStucture.getColumn() + " AS `"
									+ column + "`");
						} else {
							selects.add("`" + preColumn + "`." + columnStucture.getColumn() + " AS `" + preColumn + "."
									+ column + "`");
						}

					} else {
						throw new InsideRuntimeException("实体类：" + entityStructure.getTableName() + "，没有属性:" + column);
					}
				}

			} else if (split.length == 2) {

				// add(entityStructure, split[0], "");
				Class<?> targetEntity = entityStructure.getObjectFields().get(split[0]).getTargetEntity();
				EntityStructure classtostructure = InitService.getStructure(targetEntity);
				add(classtostructure, "*", split[0]);
				add(classtostructure, split[1], split[0]);
			}

		}

		@Override
		public String sql() {
			return "SELECT " + String.join(",", selects);
		}
	}

	private static class From implements Expression {

		private EntityStructure e;

		public From(EntityStructure e) {
			this.e = e;
		}

		@Override
		public String sql() {
			// TODO Auto-generated method stub
			return " FROM " + e.getTableName() + " AS " + e.getName();
		}
	}

	private static class Where implements Expression {

		private HashSet<String> orConditions = new HashSet<String>();
		private HashSet<String> andConditions = new HashSet<String>();
		private InnerJoin innerJoin;
		private String name;

		public Where(InnerJoin innerJoin) {
			this.innerJoin = innerJoin;
		}

		public void setName(String name) {
			this.name = name;

		}

		public void add(EntityStructure entityStructure, String condition, Map<String, Object> request) {
			String[] split = condition.split("\\.");
			String fullName;
			String Tablecolumn;
			if (split.length == 1) {
				FieldStructure field = new FieldStructure(split[0]);
				if (!entityStructure.getSimpleFields().containsKey(field.getField())) {
					throw new InsideRuntimeException("实体对象" + entityStructure.getName() + "不存在普通属性" + condition);
				}
				fullName = entityStructure.getName();
				Tablecolumn = entityStructure.getSimpleFields().get(field.getField()).getColumn();

				if (field.isSimple() || field.isLike()) {
					andConditions.add("`" + fullName + "`." + Tablecolumn + " " + field.getSymbol() + " :" + condition);
				} else if (field.isIn()) {
					andConditions.add(
							"`" + fullName + "`." + Tablecolumn + " " + field.getSymbol() + " (:" + condition + ")");
				} else if (field.isOr()) {
					orConditions.add(
							"`" + fullName + "`." + Tablecolumn + " " + field.getSymbol() + " (:" + condition + ")");
				}

			} else if (split.length == 2) {
				FieldStructure field = new FieldStructure(split[1]);
				if (!entityStructure.getObjectFields().containsKey(split[0])) {
					throw new InsideRuntimeException("实体对象" + entityStructure.getName() + "不存在属性" + condition);
				}
				EntityStructure classtostructure = InitService
						.getStructure(entityStructure.getObjectFields().get(split[0]).getTargetEntity());
				if (!classtostructure.getSimpleFields().containsKey(field.getField())) {
					throw new InsideRuntimeException("实体对象" + classtostructure.getName() + "不存在普通属性" + condition);
				}
				fullName = split[0];
				Tablecolumn = classtostructure.getSimpleFields().get(field.getField()).getColumn();

				if (field.isSimple() || field.isLike()) {
					andConditions.add("`" + fullName + "`." + Tablecolumn + " " + field.getSymbol() + " :" + condition);
				} else if (field.isIn()) {
					andConditions.add(
							"`" + fullName + "`." + Tablecolumn + " " + field.getSymbol() + " (:" + condition + ")");
				} else if (field.isOr()) {
					orConditions.add(
							"`" + fullName + "`." + Tablecolumn + " " + field.getSymbol() + " (:" + condition + ")");
				}

				if (!innerJoin.getJoinTables().contains(fullName)) {
					innerJoin.add(entityStructure, split[0], split[1]);
				}
			}
		}

		@Override
		public String sql() {
			String ret = " WHERE `" + name + "`.deleted_at IS NULL ";
			for (String string : andConditions) {
				ret += " AND " + string;
			}
			for (String str : orConditions) {
				ret += " OR " + str;
			}
			return ret;
		}

	}

	private static class LeftJoin implements Expression {
		private Set<String> joinTables = new HashSet<String>();
		private ArrayList<String> arrayList = new ArrayList<String>();
		private InnerJoin innerJoin;

		public Set<String> getJoinTables() {
			return joinTables;
		}

		public LeftJoin(InnerJoin innerJoin) {
			this.innerJoin = innerJoin;
		}

		public void add(EntityStructure entityStructure, String column, String preColumn, String name) {
			String[] split = column.split("\\.");
			if (split.length == 1) {
				String fullName;
				if ("*".equals(column)) {
					return;
				}
				if (!entityStructure.getObjectFields().containsKey(column)
						&& !entityStructure.getSimpleFields().containsKey(column)) {
					throw new InsideRuntimeException("实体对象" + entityStructure.getName() + "不存在属性" + column);
				}
				if (entityStructure.getSimpleFields().containsKey(column)) {
					return;
				} else {
					if ("".equals(preColumn)) {
						fullName = column;
					} else {
						fullName = preColumn + "." + column;
					}

					if (joinTables.contains(fullName) || innerJoin.getJoinTables().contains(fullName)) {
						return;
					}
					ColumnStucture columnStucture = entityStructure.getObjectFields().get(column);
					Class<?> targetEntity = entityStructure.getObjectFields().get(split[0]).getTargetEntity();
					EntityStructure classtostructure = InitService.getStructure(targetEntity);
					if (columnStucture.getJoinType().equals(JoinType.MANY_TO_MANY)) {
						innerJoin.getJoinTables().add(fullName);
						String joinTableName = preColumn + "." + columnStucture.getJoinTable();
						if ("".equals(preColumn)) {
							preColumn = entityStructure.getName();
							joinTableName = name + "." + columnStucture.getJoinTable();
						}

						arrayList.add(" LEFT JOIN " + columnStucture.getJoinTable() + " AS `" + joinTableName + "` ON `"
								+ preColumn + "`.id" + " = `" + joinTableName + "`." + columnStucture.getJoinColumn()
								+ " LEFT JOIN " + classtostructure.getTableName() + " AS `" + fullName + "`" + " ON `"
								+ fullName + "`.id = `" + joinTableName + "`." + columnStucture.getInverseJoinColumn());

					} else {

						joinTables.add(fullName);
						arrayList.add(" LEFT JOIN " + classtostructure.getTableName() + " AS `" + fullName + "`"
								+ on(entityStructure, column, preColumn));
					}
				}

			} else if (split.length == 2) {
				add(entityStructure, split[0], preColumn, name);
				Class<?> targetEntity = entityStructure.getObjectFields().get(split[0]).getTargetEntity();
				EntityStructure classtostructure = InitService.getStructure(targetEntity);
				if (classtostructure.getObjectFields().containsKey(split[1])) {
					add(classtostructure, split[1], split[0], name);
				}
			}
		}

		private String on(EntityStructure entityStructure, String column, String preColumn) {
			String fullName;
			if ("".equals(preColumn)) {
				preColumn = entityStructure.getName();
				fullName = column;
			} else {
				fullName = preColumn + "." + column;
			}
			String ret = "";
			ColumnStucture columnStucture = entityStructure.getObjectFields().get(column);
			if (!columnStucture.isMappedBy()) {
				ret = " ON `" + fullName + "`.id = `" + preColumn + "`."
						+ entityStructure.getObjectFields().get(column).getJoinColumn();
			} else {
				ret = " ON `" + fullName + "`." + entityStructure.getObjectFields().get(column).getJoinColumn() + " = `"
						+ preColumn + "`.id";
			}

			return ret;
		}

		@Override
		public String sql() {

			return String.join(" ", arrayList);
		}
	}

	private static class InnerJoin implements Expression {
		private Set<String> joinTables = new HashSet<String>();
		private ArrayList<String> arrayList = new ArrayList<String>();

		public Set<String> getJoinTables() {
			return joinTables;
		}

		@Override
		public String sql() {
			return String.join(" ", arrayList);
		}

		public void add(EntityStructure entityStructure, String split0, String split1) {

			if (!split1.equals("")) {
				String Tablecolumn;
				EntityStructure targetStructure = InitService
						.getStructure(entityStructure.getObjectFields().get(split0).getTargetEntity());
				if (joinTables.contains(split0)) {
					return;
				}
				joinTables.add(split0);
				ColumnStucture columnStucture = entityStructure.getObjectFields().get(split0);
				if (columnStucture.getJoinType().equals(JoinType.MANY_TO_MANY)) {

					arrayList.add(" INNER JOIN " + columnStucture.getJoinTable() + " ON `" + entityStructure.getName()
							+ "`.id" + " = `" + columnStucture.getJoinTable() + "`." + columnStucture.getJoinColumn()
							+ " INNER JOIN " + targetStructure.getTableName() + " AS `" + split0 + "`" + " ON `"
							+ split0 + "`.id = `" + columnStucture.getJoinTable() + "`."
							+ columnStucture.getInverseJoinColumn());

				} else {
					arrayList.add(" INNER JOIN " + targetStructure.getTableName() + " AS `" + split0 + "`"
							+ on(entityStructure, split0, split1));
				}
			}
		}

		private String on(EntityStructure entityStructure, String split0, String split1) {

			String ret = "";
			ColumnStucture columnStucture = entityStructure.getObjectFields().get(split0);
			if (!columnStucture.isMappedBy()) {
				ret = " ON `" + split0 + "`.id = `" + entityStructure.getName() + "`."
						+ entityStructure.getObjectFields().get(split0).getJoinColumn();
			} else {
				ret = " ON `" + split0 + "`." + entityStructure.getObjectFields().get(split0).getJoinColumn() + " = `"
						+ entityStructure.getName() + "`.id";
			}

			return ret;
		}

	}

	private static class OrderBy implements Expression {

		private InnerJoin innerJoin;
		private LeftJoin leftJoin;

		private Set<String> exists = new HashSet<>();
		private Set<String> orders = new LinkedHashSet<>();

		public OrderBy(InnerJoin innerJoin, LeftJoin leftJoin) {
			this.leftJoin = leftJoin;
			this.innerJoin = innerJoin;
		}

		public void add(EntityStructure entityStructure, Pageable page) {
			Iterator<Order> iterator = page.getSort().iterator();
			while (iterator.hasNext()) {
				Order next = iterator.next();
				String property = next.getProperty();
				String direction = next.getDirection().toString();

				String[] split = property.split("\\.");
				if (split.length == 1) {
					if (!entityStructure.getSimpleFields().containsKey(split[0])) {
						throw new InsideRuntimeException("实体对象" + entityStructure.getName() + "不存在可比较属性" + split[0]);
					}
					String fullName = entityStructure.getName();
					String tablecolumn = entityStructure.getSimpleFields().get(split[0]).getColumn();
					if (!exists.contains("`" + fullName + "`." + tablecolumn)) {
						exists.add("`" + fullName + "`." + tablecolumn);
						orders.add("`" + fullName + "`." + tablecolumn + " " + direction);
					}

				} else if (split.length == 2) {
					if (!entityStructure.getObjectFields().containsKey(split[0])) {
						throw new InsideRuntimeException("实体对象" + entityStructure.getName() + "不存在属性" + split[0]);
					}
					EntityStructure classtostructure = InitService
							.getStructure(entityStructure.getObjectFields().get(split[0]).getTargetEntity());
					if (!classtostructure.getSimpleFields().containsKey(split[1])) {
						throw new InsideRuntimeException("实体对象" + classtostructure.getName() + "不存在可比较属性" + split[1]);
					}
					String fullName = split[0];
					String tablecolumn = classtostructure.getSimpleFields().get(split[1]).getColumn();
					if (!exists.contains("`" + fullName + "`." + tablecolumn)) {
						exists.add("`" + fullName + "`." + tablecolumn);
						orders.add("`" + fullName + "`." + tablecolumn + " " + direction);
						if (leftJoin.getJoinTables().contains(fullName)
								|| innerJoin.getJoinTables().contains(fullName)) {
							continue;
						} else {
							innerJoin.add(entityStructure, split[0], split[1]);
						}
					}
				}
			}
		}

		@Override
		public String sql() {
			// TODO Auto-generated method stub
			if (orders.isEmpty()) {
				return "";
			} else {
				return " ORDER BY " + String.join(",", orders);
			}

		}

	}

	private static class Limit implements Expression {

		@Override
		public String sql() {
		
			return " LIMIT :pageStart , :pageSize";
		}

	}

	private static class GroupBy implements Expression {

		private String name;

		public GroupBy(String name) {
			this.name = name;
		}

		@Override
		public String sql() {
			return " GROUP BY `" + name + "`.id";
		}

	}

	interface Expression {

		public String sql();
	}

	public static class SqlCouple implements Serializable {
		String findSql;
		String countSql;

		public String getFindSql() {
			return findSql;
		}

		public void setFindSql(String findSql) {
			this.findSql = findSql;
		}

		public String getCountSql() {
			return countSql;
		}

		public void setCountSql(String countSql) {
			this.countSql = countSql;
		}

	}

	public static class FieldStructure {
		private String field;
		private String origin;
		private RelationShip relationship;
		private String symbol;

		public FieldStructure(String field) {
			this.origin = field;
			if (field.endsWith("]")) {
				this.field = field.replace("]", "");
				this.relationship = RelationShip.GT;
				this.symbol = ">=";
			} else if (field.endsWith("[")) {
				this.field = field.replace("[", "");
				this.relationship = RelationShip.LT;
				this.symbol = "<=";
			} else if (field.endsWith("!")) {
				this.field = field.replace("!", "");
				this.relationship = RelationShip.NOT;
				this.symbol = "<>";
			} else if (field.endsWith("@")) {
				this.field = field.replace("@", "");
				this.relationship = RelationShip.IN;
				this.symbol = "in";
			} else if (field.endsWith("~")) {
				this.field = field.replace("~", "");
				this.relationship = RelationShip.LIKE;
				this.symbol = "like";
			} else if (field.endsWith("$")) {
				this.field = field.replace("$", "");
				this.relationship = RelationShip.OR;
				this.symbol = "in";
			} else {
				this.field = field;
				this.relationship = RelationShip.EQ;
				this.symbol = "=";
			}
		}

		public String getSymbol() {
			return symbol;
		}

		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public String getOrigin() {
			return origin;
		}

		public void setOrigin(String origin) {
			this.origin = origin;
		}

		public RelationShip getRelationship() {
			return relationship;
		}

		public void setRelationship(RelationShip relationship) {
			this.relationship = relationship;
		}

		public boolean isSimple() {
			if (relationship.equals(RelationShip.EQ) || relationship.equals(RelationShip.GT)
					|| relationship.equals(RelationShip.LT) || relationship.equals(RelationShip.NOT)) {
				return true;
			} else {
				return false;
			}
		}

		public boolean isLike() {
			return relationship.equals(RelationShip.LIKE);
		}

		public boolean isIn() {
			return relationship.equals(RelationShip.IN);
		}

		public boolean isOr() {
			return relationship.equals(RelationShip.OR);
		}
	}

	public enum RelationShip {
		EQ, GT, LT, NOT, IN, LIKE, OR
	}

}
