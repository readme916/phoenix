package com.shangdao.phoenix.entity.interfaces;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;

public interface ILog<E extends ILogEntity , F extends IFile, N extends INoticeLog> extends IBaseEntity{

	E getEntity();

	void setEntity(E entity);

	Act getAct();

	void setAct(Act act);

	State getBeforeState();

	State getAfterState();

	String getDifference();

	String getIp();

	Double getLongitude();

	Double getLatitude();

	String getImei();

	Terminal getTerminal();
	
	String getNote();
	
	Set<F> getFiles();
	
	void setFiles(Set<F> files);
	
	Set<N> getNotices();
	
	void setNotices(Set<N> notices);


	void setBeforeState(State beforeState);

	void setAfterState(State afterState);

	void setDifference(String difference);

	void setIp(String ip);

	void setLongitude(Double longitude);

	void setLatitude(Double latitude);

	void setImei(String imei);

	void setTerminal(Terminal terminal);
	
	void setNote(String note);
	
	
	
	
	public static class DiffItem {
		private String name;
		private DiffType type;
		private Object oldString;
		private Object newString;
		private DiffEntity oldObject;
		private DiffEntity newObject;
		private List<DiffEntity> increaseObject=new ArrayList<DiffEntity>();
		private List<DiffEntity> decreaseObject=new ArrayList<DiffEntity>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public DiffType getType() {
			return type;
		}

		public void setType(DiffType type) {
			this.type = type;
		}

		public Object getOldString() {
			return oldString;
		}

		public void setOldString(Object oldString) {
			this.oldString = oldString;
		}

		public Object getNewString() {
			return newString;
		}

		public void setNewString(Object newString) {
			this.newString = newString;
		}

		public DiffEntity getOldObject() {
			return oldObject;
		}

		public void setOldObject(DiffEntity oldObject) {
			this.oldObject = oldObject;
		}

		public DiffEntity getNewObject() {
			return newObject;
		}

		public void setNewObject(DiffEntity newObject) {
			this.newObject = newObject;
		}

		public List<DiffEntity> getIncreaseObject() {
			return increaseObject;
		}

		public void setIncreaseObject(List<DiffEntity> increaseObject) {
			this.increaseObject = increaseObject;
		}

		public List<DiffEntity> getDecreaseObject() {
			return decreaseObject;
		}

		public void setDecreaseObject(List<DiffEntity> decreaseObject) {
			this.decreaseObject = decreaseObject;
		}

	}

	public enum DiffType {
		STRING, OBJECT, LIST
	}

	public static class DiffEntity {
		private String entity;
		private long id;
		private String name;

		public DiffEntity(){
			super();
		}
		
		public DiffEntity(String entity, long id, String name) {
			super();
			this.entity = entity;
			this.id = id;
			this.name = name;
		}

		public String getEntity() {
			return entity;
		}

		public void setEntity(String entity) {
			this.entity = entity;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
	
}
