package com.shangdao.phoenix.entity.noticeTemplate;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.IBaseEntity;
import com.shangdao.phoenix.entity.menu.Menu;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;

@Entity
@Table(name="notice_template")
public class NoticeTemplate implements IBaseEntity , Serializable{
	
	/**
	 * 
	 */
	@Transient
	public static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private long id;
	
	@ManyToOne
	@JoinColumn(name="entity_manager_id")
	private EntityManager entityManager;
	
	@Column(name="name")
    private String name;
	
	@ManyToOne
	@JoinColumn(name="created_by")
	@JsonIgnore
	private User createdBy;
	
	@Column(name="created_at")
	@JsonIgnore
	private Date createdAt;

	@Column(name="deleted_at")
	@JsonIgnore
	private Date deletedAt;
	
	@ManyToOne
	@JoinColumn(name="state_id")
	private State state;
	
	@Column(name="notice_channel")
	@Enumerated(EnumType.STRING)
	@NotNull
	private NoticeChannel noticeChannel;
	
	@Column(name="ali_sms_sign_name")
	private String aliSmsSignName;
	
	@Column(name="ali_sms_template_code")
	private String aliSmsTemplateCode;
	
	@Column(name="ali_sms_template_param")
	private String aliSmsTemplateParam;
	
	@Column(name="ali_sms_template_content")
	@Lob
	private String aliSmsTemplateContent;
	
	
	@Column(name="wxwork_agent_id")
	private String wxworkAgentId;
	
	@Column(name="wxwork_agent_secret")
	private String wxworkAgentSecret;

	@Column(name="wxwork_template")
	@Lob
	private String wxworkTemplate;
	
	
	
	@Column(name="wxpublic_template_id")
	private String wxpublicTemplateId;
	
	@Column(name="wxpublic_template_content")
	@Lob
	private String wxpublicTemplateContent;
	
	@Column(name="wxpublic_template_url")
	private String wxpublicTemplateUrl;
	
	@Column(name="wxpublic_template_data")
	@Lob
	private String wxpublicTemplateData;
	
	
	@Column(name="message_template")
	@Lob
	private String messageTemplate;
	

	public String getAliSmsTemplateContent() {
		return aliSmsTemplateContent;
	}

	public void setAliSmsTemplateContent(String aliSmsTemplateContent) {
		this.aliSmsTemplateContent = aliSmsTemplateContent;
	}

	public String getWxpublicTemplateData() {
		return wxpublicTemplateData;
	}

	public void setWxpublicTemplateData(String wxpublicTemplateData) {
		this.wxpublicTemplateData = wxpublicTemplateData;
	}

	public String getWxpublicTemplateUrl() {
		return wxpublicTemplateUrl;
	}

	public void setWxpublicTemplateUrl(String wxpublicTemplateUrl) {
		this.wxpublicTemplateUrl = wxpublicTemplateUrl;
	}

	public String getWxpublicTemplateContent() {
		return wxpublicTemplateContent;
	}

	public void setWxpublicTemplateContent(String wxpublicTemplateContent) {
		this.wxpublicTemplateContent = wxpublicTemplateContent;
	}

	public String getMessageTemplate() {
		return messageTemplate;
	}

	public void setMessageTemplate(String messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	public NoticeChannel getNoticeChannel() {
		return noticeChannel;
	}

	public void setNoticeChannel(NoticeChannel noticeChannel) {
		this.noticeChannel = noticeChannel;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Date deletedAt) {
		this.deletedAt = deletedAt;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getAliSmsSignName() {
		return aliSmsSignName;
	}

	public void setAliSmsSignName(String aliSmsSignName) {
		this.aliSmsSignName = aliSmsSignName;
	}

	public String getAliSmsTemplateCode() {
		return aliSmsTemplateCode;
	}

	public void setAliSmsTemplateCode(String aliSmsTemplateCode) {
		this.aliSmsTemplateCode = aliSmsTemplateCode;
	}

	public String getAliSmsTemplateParam() {
		return aliSmsTemplateParam;
	}

	public void setAliSmsTemplateParam(String aliSmsTemplateParam) {
		this.aliSmsTemplateParam = aliSmsTemplateParam;
	}

	public String getWxworkAgentId() {
		return wxworkAgentId;
	}

	public void setWxworkAgentId(String wxworkAgentId) {
		this.wxworkAgentId = wxworkAgentId;
	}

	public String getWxworkAgentSecret() {
		return wxworkAgentSecret;
	}

	public void setWxworkAgentSecret(String wxworkAgentSecret) {
		this.wxworkAgentSecret = wxworkAgentSecret;
	}

	public String getWxworkTemplate() {
		return wxworkTemplate;
	}

	public void setWxworkTemplate(String wxworkTemplate) {
		this.wxworkTemplate = wxworkTemplate;
	}

	public String getWxpublicTemplateId() {
		return wxpublicTemplateId;
	}

	public void setWxpublicTemplateId(String wxpublicTemplateId) {
		this.wxpublicTemplateId = wxpublicTemplateId;
	}


	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}


	public enum NoticeChannel{
		SMS,MESSAGE,WXWORK,WXPUBLIC,EMAIL,APP
	}
	

}