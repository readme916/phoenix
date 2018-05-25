package com.shangdao.phoenix.entity.example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.act.ActNotice.DelayType;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.IBaseEntity;
import com.shangdao.phoenix.entity.interfaces.IFile;
import com.shangdao.phoenix.entity.interfaces.ILog;
import com.shangdao.phoenix.entity.interfaces.INoticeLog;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate.NoticeChannel;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.util.FileFormat;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;

@Entity
@Table(name = "example_notice")
public class ExampleNotice implements  INoticeLog<Example,ExampleLog>{

	/**
	 * 
	 */
	@Transient
	public static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;
	
	@ManyToOne
	@JoinColumn(name="entity_manager_id")
	private EntityManager entityManager;

	@ManyToOne
	@JoinColumn(name = "entity_id")
	private Example entity;
	
	@ManyToOne
	@JoinColumn(name = "act_id")
	private Act act;
	
	@ManyToOne
	@JoinColumn(name="log_id")
	private ExampleLog log;

	@Column(name = "create_at")
	private Date createdAt;

	@Column(name = "name")
	private String name;
	
	@Column(name="deleted_at")
	private Date deletedAt;
	
	@ManyToOne
	@JoinColumn(name="created_by")
	@JsonIgnore
	private User createdBy;
	
	@ManyToOne
	@JoinColumn(name="state_id")
	private State state;
	
	@Column(name="content")
	@Lob
	private String content;
	
	@ManyToOne
	@JoinColumn(name="to_user_id")
	private User toUser;
	
	@Column(name="delay_type")
	@Enumerated(EnumType.STRING)
	private DelayType delayType;
	
	@Column(name="notice_channel")
	@Enumerated(EnumType.STRING)
	private NoticeChannel noticeChannel;
	
	@Column(name="role_code")
	private String roleCode;
	
	@ManyToOne
	@JoinColumn(name="notice_template_id")
	private NoticeTemplate noticeTemplate;
	
	@Column(name="success")
	private boolean success;

	public String getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	public NoticeTemplate getNoticeTemplate() {
		return noticeTemplate;
	}

	public void setNoticeTemplate(NoticeTemplate noticeTemplate) {
		this.noticeTemplate = noticeTemplate;
	}


	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public User getToUser() {
		return toUser;
	}

	public void setToUser(User toUser) {
		this.toUser = toUser;
	}


	public DelayType getDelayType() {
		return delayType;
	}

	public void setDelayType(DelayType delayType) {
		this.delayType = delayType;
	}


	public NoticeChannel getNoticeChannel() {
		return noticeChannel;
	}

	public void setNoticeChannel(NoticeChannel noticeChannel) {
		this.noticeChannel = noticeChannel;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public Date getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Date deletedAt) {
		this.deletedAt = deletedAt;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public ExampleLog getLog() {
		return log;
	}

	public void setLog(ExampleLog log) {
		this.log = log;
	}


	@Override
	public long getId() {
		return id;
	}

	
	@Override
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public Date getCreatedAt() {
		// TODO Auto-generated method stub
		return createdAt;
	}

	@Override
	public Example getEntity() {
		// TODO Auto-generated method stub
		return entity;
	}

	
	@Override
	public void setEntity(Example entity) {
		this.entity = entity;
	}

	
	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	
	
	@Override
	public Act getAct() {
		// TODO Auto-generated method stub
		return act;
	}

	
	
	@Override
	public void setAct(Act act) {
		this.act = act;
	}

	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}




	
	

}