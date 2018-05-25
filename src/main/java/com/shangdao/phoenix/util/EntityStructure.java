package com.shangdao.phoenix.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.act.ActRepository;
import com.shangdao.phoenix.entity.entityManager.EntityManagerRepository;
import com.shangdao.phoenix.entity.interfaces.ILog;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.interfaces.IProjectEntity;
import com.shangdao.phoenix.entity.interfaces.IStateMachineEntity;
import com.shangdao.phoenix.entity.interfaces.ITagEntity;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.state.StateRepository;
import com.shangdao.phoenix.service.InterfaceEntityService;

public class EntityStructure {

	//唯一名字，一般是首字母小写的类名，通常用于前端的各种地方
	private String name;
	
	//数据库的表名，一般带下划线
	private String tableName;
	
	//数据库里的id
	private Long entityManagerId;
	
	//实体的类
	private Class<?> cls;
	
	//实体的简单属性集合
	private Map<String, ColumnStucture> simpleFields = new HashMap();
	
	//实体的对象属性集合
	private Map<String, ColumnStucture> objectFields = new HashMap();
	
	//实体的附加服务，后注入的可以为null
	private InterfaceEntityService entityService;
	
	
	//这个实体的repository
	@JsonIgnore
	private JpaRepository jpaRepository;
	
	//是否需要log
	private Boolean logEntity;
	//是否是项目实体，有成员，有订阅
	private Boolean projectEntity;
	//是否有tags
	private Boolean tagEntity;
	//是否是log
	private Boolean log;
	
	//是否是状态机实体，基于动作的状态切换
	private Boolean stateMachineEntity;
	
	public InterfaceEntityService getEntityService() {
		return entityService;
	}

	public void setEntityService(InterfaceEntityService entityService) {
		this.entityService = entityService;
	}


	public Long getEntityManagerId() {
		return entityManagerId;
	}

	public void setEntityManagerId(Long entityManagerId) {
		this.entityManagerId = entityManagerId;
	}

	
	public JpaRepository getJpaRepository() {
		return jpaRepository;
	}

	public void setJpaRepository(JpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	public boolean isTagEntity() {
		if (tagEntity == null) {
			Class<?>[] interfaces = cls.getInterfaces();
			for (Class<?> class1 : interfaces) {
				if (class1.equals(ITagEntity.class)) {
					tagEntity = true;
					return tagEntity;
				}
			}
			tagEntity = false;
			return tagEntity;

		} else {
			return tagEntity;
		}

	}
	
	public boolean isLogEntity() {
		if (logEntity == null) {
			Class<?>[] interfaces = cls.getInterfaces();
			for (Class<?> class1 : interfaces) {
				if (class1.equals(ILogEntity.class)||class1.equals(IStateMachineEntity.class)) {
					logEntity = true;
					return logEntity;
				}
			}
			logEntity = false;
			return logEntity;

		} else {
			return logEntity;
		}

	}

	public boolean isProjectEntity() {
		if (projectEntity == null) {
			Class<?>[] interfaces = cls.getInterfaces();
			for (Class<?> class1 : interfaces) {
				if (class1.equals(IProjectEntity.class)) {
					projectEntity = true;
					return projectEntity;
				}
			}
			projectEntity = false;
			return projectEntity;

		} else {
			return projectEntity;
		}
	}
	public boolean isLog() {
		if (log == null) {
			Class<?>[] interfaces = cls.getInterfaces();
			for (Class<?> class1 : interfaces) {
				if (class1.equals(ILog.class)) {
					log = true;
					return log;
				}
			}
			log = false;
			return log;

		} else {
			return log;
		}
	}
	public boolean isStateMachineEntity() {
		if (stateMachineEntity == null) {
			Class<?>[] interfaces = cls.getInterfaces();
			for (Class<?> class1 : interfaces) {
				if (class1.equals(IStateMachineEntity.class)) {
					stateMachineEntity = true;
					return stateMachineEntity;
				}
			}
			stateMachineEntity = false;
			return stateMachineEntity;

		} else {
			return stateMachineEntity;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Class<?> getCls() {
		return cls;
	}

	public void setCls(Class<?> cls) {
		this.cls = cls;
	}

	public Map<String, ColumnStucture> getSimpleFields() {
		return simpleFields;
	}

	public void setSimpleFields(Map<String, ColumnStucture> simpleFields) {
		this.simpleFields = simpleFields;
	}

	public Map<String, ColumnStucture> getObjectFields() {
		return objectFields;
	}

	public void setObjectFields(Map<String, ColumnStucture> objectFields) {
		this.objectFields = objectFields;
	}

	public static class ColumnStucture {
		private Format format;
		private boolean mappedBy;
		private JoinType joinType;
		private Class<?> targetEntity;
		private String joinTable;
		private String joinColumn;
		private String inverseJoinColumn;
		private String column;

		public ColumnStucture(Format format, JoinType joinType, boolean mappedBy, Class<?> targetEntity,
				String joinTable, String joinColumn, String inverseJoinColumn, String column) {
			super();
			this.format = format;
			this.mappedBy = mappedBy;
			this.joinType = joinType;
			this.targetEntity = targetEntity;
			this.joinTable = joinTable;
			this.joinColumn = joinColumn;
			this.inverseJoinColumn = inverseJoinColumn;
			this.column = column;
		}

		public boolean isMappedBy() {
			return mappedBy;
		}

		public void setMappedBy(boolean mappedBy) {
			this.mappedBy = mappedBy;
		}

		public Format getFormat() {
			return format;
		}

		public void setFormat(Format format) {
			this.format = format;
		}

		public JoinType getJoinType() {
			return joinType;
		}

		public void setJoinType(JoinType joinType) {
			this.joinType = joinType;
		}

		public Class<?> getTargetEntity() {
			return targetEntity;
		}

		public void setTargetEntity(Class<?> targetEntity) {
			this.targetEntity = targetEntity;
		}

		public String getJoinTable() {
			return joinTable;
		}

		public void setJoinTable(String joinTable) {
			this.joinTable = joinTable;
		}

		public String getJoinColumn() {
			return joinColumn;
		}

		public void setJoinColumn(String joinColumn) {
			this.joinColumn = joinColumn;
		}

		public String getInverseJoinColumn() {
			return inverseJoinColumn;
		}

		public void setInverseJoinColumn(String inverseJoinColumn) {
			this.inverseJoinColumn = inverseJoinColumn;
		}

		public String getColumn() {
			return column;
		}

		public void setColumn(String column) {
			this.column = column;
		}

	}

	public enum Format {
		SIMPLE, OBJECT
	}

	public enum JoinType {
		MANY_TO_MANY, MANY_TO_ONE, ONE_TO_MANY, ONE_TO_ONE;
	}

	
	public static String stopWords = "ADD ALL ALTER ANALYZE AND AS ASC ASENSITIVE BEFORE BETWEEN BIGINT BINARY BLOB BOTH BY CALL CASCADE CASE CHANGE CHAR CHARACTER CHECK COLLATE COLUMN"
			+ " CONDITION CONNECTION CONSTRAINT CONTINUE CONVERT CREATE CROSS CURRENT_DATE CURRENT_TIME CURRENT_TIMESTAMP CURRENT_USER CURSOR DATABASE DATABASES DAY_HOUR DAY_MICROSECOND DAY_MINUTE DAY_SECOND"
			+ " DEC DECIMAL DECLARE DEFAULT DELAYED DELETE DESC DESCRIBE DETERMINISTIC DISTINCT DISTINCTROW DIV DOUBLE DROP DUAL EACH ELSE ELSEIF ENCLOSED ESCAPED EXISTS"
			+ " EXIT EXPLAIN FALSE FETCH FLOAT FLOAT4 FLOAT8 FOR FORCE FOREIGN FROM FULLTEXT GOTO GRANT GROUP HAVING HIGH_PRIORITY HOUR_MICROSECOND HOUR_MINUTE HOUR_SECOND IF"
			+ " IGNORE IN INDEX INFILE INNER INOUT INSENSITIVE INSERT INT INT1 INT2 INT3 INT4 INT8 INTEGER INTERVAL INTO IS ITERATE JOIN KEY KEYS KILL LABEL LEADING LEAVE LEFT"
			+ " LIKE LIMIT LINEAR LINES LOAD LOCALTIME LOCALTIMESTAMP LOCK LONG LONGBLOB LONGTEXT LOOP LOW_PRIORITY MATCH MEDIUMBLOB MEDIUMINT MEDIUMTEXT MIDDLEINT MINUTE_MICROSECOND MINUTE_SECOND MOD"
			+ " MODIFIES NATURAL NOT NO_WRITE_TO_BINLOG NULL NUMERIC ON OPTIMIZE OPTION OPTIONALLY OR ORDER OUT OUTER OUTFILE PRECISION PRIMARY PROCEDURE PURGE RAID0 RANGE READ READS REAL"
			+ " REFERENCES REGEXP RELEASE RENAME REPEAT REPLACE REQUIRE RESTRICT RETURN REVOKE RIGHT RLIKE SCHEMA SCHEMAS SECOND_MICROSECONDSELECT SENSITIVE SEPARATOR SET SHOW SMALLINT SPATIAL SPECIFIC SQL"
			+ " SQLEXCEPTION SQLSTATE SQLWARNING SQL_BIG_RESULT SQL_CALC_FOUND_ROWS SQL_SMALL_RESULT SSL STARTING STRAIGHT_JOIN TABLE TERMINATED THEN TINYBLOB TINYINT TINYTEXT TO TRAILING TRIGGER"
			+ " TRUE UNDO UNION UNIQUE UNLOCK UNSIGNED UPDATE USAGE USE USING UTC_DATE UTC_TIME UTC_TIMESTAMP VALUES VARBINARY VARCHAR VARCHARACTER VARYING WHEN WHERE WHILE WITH WRITE X509 XOR YEAR_MONTH ZEROFILL";
}
