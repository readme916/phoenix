package com.shangdao.phoenix.entity.interfaces;

import java.util.Date;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.act.ActNotice.DelayType;
import com.shangdao.phoenix.entity.example.Example;
import com.shangdao.phoenix.entity.example.ExampleLog;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate.NoticeChannel;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.util.FileFormat;

public interface INoticeLog<E extends ILogEntity,L extends ILog> extends IBaseEntity {
	E getEntity();
	void setEntity(E entity);

	Act getAct();
	void setAct(Act act);
	
	L getLog();
	void setLog(L log);
	
	String getContent();
	void setContent(String content);

	User getToUser();
	void setToUser(User user);
	
	DelayType getDelayType();
	void setDelayType(DelayType delayType);
	
	NoticeChannel getNoticeChannel();
	void setNoticeChannel(NoticeChannel noticeChannel);
	
	String getRoleCode();
	void setRoleCode(String roleCode);
	
	boolean isSuccess();
	void setSuccess(boolean success);
	
	public NoticeTemplate getNoticeTemplate();
	public void setNoticeTemplate(NoticeTemplate noticeTemplate);

}