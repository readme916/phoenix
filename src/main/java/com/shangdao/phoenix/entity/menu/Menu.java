package com.shangdao.phoenix.entity.menu;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.IBaseEntity;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.interfaces.IProjectEntity;
import com.shangdao.phoenix.entity.interfaces.IStateMachineEntity;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.service.FileUploadService.OssImage;
import com.shangdao.phoenix.util.TreeBuilder.TreeNode;

@Entity
@Table(name="menu")
public class Menu implements TreeNode<Menu>,IBaseEntity,Serializable{
	
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

	@Column(name="path")
	private String path;

	@Column(name="title")
	private String title;
	
	@Column(name="icon")
	private String icon;
	
	@ManyToOne
	@JoinColumn(name="target_entity_manager_id")
	private EntityManager targetEntityManager;

	@ManyToMany
	@JoinTable(name="role_menu",joinColumns = { @JoinColumn(name = "menu_id") }, inverseJoinColumns = { @JoinColumn(name = "role_id") })
	private Set<Role> roles;
	
	@ManyToOne
	@JoinColumn(name="parent_id")
	private Menu parent;
	
	@OneToMany(mappedBy="parent")
	private Set<Menu> children;
	
	@Column(name="created_at")
	@JsonIgnore
	private Date createdAt;
	
	@Column(name="sort_number")
	private int sortNumber;
	
	@Column(name="deleted_at")
	@JsonIgnore
	private Date deletedAt;
	
	@ManyToOne
	@JoinColumn(name="created_by")
	@JsonIgnore
	private User createdBy;
	
	@ManyToOne
	@JoinColumn(name="state_id")
	private State state;
	
	
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
	
	public int getSortNumber() {
		return sortNumber;
	}

	public void setSortNumber(int sortNumber) {
		this.sortNumber = sortNumber;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public EntityManager getTargetEntityManager() {
		return targetEntityManager;
	}

	public void setTargetEntityManager(EntityManager targetEntityManager) {
		this.targetEntityManager = targetEntityManager;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Set<Menu> getChildren() {
		return children;
	}

	public void setChildren(Set<Menu> children) {
		this.children = children;
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

	public Menu getParent() {
		return parent;
	}

	public void setParent(Menu parent) {
		this.parent = parent;
	}

	@Override
	public Date getCreatedAt() {
		// TODO Auto-generated method stub
		return createdAt;
	}
	
	public void setCreatedAt(Date createdAt){
		this.createdAt = createdAt;
	}

	








}