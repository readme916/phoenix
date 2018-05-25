package com.shangdao.phoenix.notice;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.user.User;

public class SpelNoticeContext {
	
	private User fromUser;
	private User toUser;
	private ILogEntity entity;
	private Act act;
	
	public String getTime() {
		SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return myFmt.format(new Date());
	}
	public User getFromUser() {
		return fromUser;
	}
	public void setFromUser(User fromUser) {
		this.fromUser = fromUser;
	}
	public User getToUser() {
		return toUser;
	}
	public void setToUser(User toUser) {
		this.toUser = toUser;
	}
	public ILogEntity getEntity() {
		return entity;
	}
	public void setEntity(ILogEntity entity) {
		this.entity = entity;
	}
	public Act getAct() {
		return act;
	}
	public void setAct(Act act) {
		this.act = act;
	}
	public SpelNoticeContext(ILogEntity entity, Act act , User toUser, User fromUser) {
		super();
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.entity = entity;
		this.act = act;
	}
	

}
