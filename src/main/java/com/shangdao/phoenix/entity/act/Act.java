package com.shangdao.phoenix.entity.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.IBaseEntity;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name="act")
public class Act implements IBaseEntity{
	
	@Transient
	public static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private long id;
	
	@Column(name="created_at")
	@JsonIgnore
	private Date createdAt;
	
	@Column(name="name")
	private String name;
	
	@Column(name="deleted_at")
	@JsonIgnore
	private Date deletedAt;
	
	@Column(name="code")
	@NotNull
	private String code;
	
	@Column(name="sort_number")
	private int sortNumber;
	
	@Column(name="list_visable")
	private boolean listVisable=false;
	
	@Column(name="detail_visable")
	private boolean detailVisable=false;

	@Column(name="icon_cls")
	private String iconCls;

	@Column(name="description")
	private String description;
	
	
	@ManyToOne
	@JoinColumn(name="target_state_id")
	private State targetState;
	
	@ManyToOne
	@JoinColumn(name="entity_manager_id")
	@NotNull
	private EntityManager entityManager;
	
	@ManyToMany
	@JoinTable(name="state_act",joinColumns = { @JoinColumn(name = "act_id") }, inverseJoinColumns = { @JoinColumn(name = "state_id") })
	private Set<State> states;
	
	@ManyToMany
    @JoinTable(name="act_role",joinColumns = { @JoinColumn(name = "act_id") }, inverseJoinColumns = { @JoinColumn(name = "role_id") })
    private Set<Role> roles;
	
	@Column(name="act_group")
	private String actGroup;
	
	@Column(name="all_can")
	private boolean allCan=false;
	@Column(name="creator_can")
	private boolean creatorCan=false;
	@Column(name="manager_can")
	private boolean managerCan=false;
	@Column(name="member_can")
	private boolean memberCan=false;
	@Column(name="department_can")
	private boolean departmentCan=false;
	@Column(name="subscriber_can")
	private boolean subscriberCan=false;

	@ManyToOne
	@JoinColumn(name="created_by")
	@JsonIgnore
	private User createdBy;

	@ManyToOne
	@JoinColumn(name="state_id")
	private State state;
	
	@OneToMany(mappedBy="act")
	private Set<ActNotice> actNotices;
	
	@Column(name="cancel_other_notice")
	private boolean cancelOtherNotice;

	
	public Set<ActNotice> getActNotices() {
		return actNotices;
	}

	public void setActNotices(Set<ActNotice> actNotices) {
		this.actNotices = actNotices;
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public void setState(State state) {
		this.state = state;
	}
	
	
	public boolean isListVisable() {
		return listVisable;
	}

	public void setListVisable(boolean listVisable) {
		this.listVisable = listVisable;
	}

	public boolean isDetailVisable() {
		return detailVisable;
	}

	public void setDetailVisable(boolean detailVisable) {
		this.detailVisable = detailVisable;
	}


	public String getIconCls() {
		return iconCls;
	}

	public void setIconCls(String iconCls) {
		this.iconCls = iconCls;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActGroup() {
		return actGroup;
	}

	public void setActGroup(String actGroup) {
		this.actGroup = actGroup;
	}

	@Override
	public User getCreatedBy() {
		return createdBy;
	}

	@Override
	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Date getDeletedAt() {
		return deletedAt;
	}

	@Override
	public void setDeletedAt(Date deletedAt) {
		this.deletedAt = deletedAt;
	}

	public int getSortNumber() {
		return sortNumber;
	}

	public void setSortNumber(int sortNumber) {
		this.sortNumber = sortNumber;
	}

	public State getTargetState() {
		return targetState;
	}

	public void setTargetState(State targetState) {
		this.targetState = targetState;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	public boolean isAllCan() {
		return allCan;
	}

	public void setAllCan(boolean allCan) {
		this.allCan = allCan;
	}

	public boolean isCreatorCan() {
		return creatorCan;
	}

	public void setCreatorCan(boolean creatorCan) {
		this.creatorCan = creatorCan;
	}

	public boolean isManagerCan() {
		return managerCan;
	}

	public void setManagerCan(boolean managerCan) {
		this.managerCan = managerCan;
	}

	public boolean isMemberCan() {
		return memberCan;
	}

	public void setMemberCan(boolean memberCan) {
		this.memberCan = memberCan;
	}

	public boolean isDepartmentCan() {
		return departmentCan;
	}

	public void setDepartmentCan(boolean departmentCan) {
		this.departmentCan = departmentCan;
	}

	public boolean isSubscriberCan() {
		return subscriberCan;
	}

	public void setSubscriberCan(boolean subscriberCan) {
		this.subscriberCan = subscriberCan;
	}


	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Set<State> getStates() {
		return states;
	}

	public void setStates(Set<State> states) {
		this.states = states;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isCancelOtherNotice() {
		return cancelOtherNotice;
	}

	public void setCancelOtherNotice(boolean cancelOtherNotice) {
		this.cancelOtherNotice = cancelOtherNotice;
	}

	
}
