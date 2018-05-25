package com.shangdao.phoenix.entity.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.interfaces.IBaseEntity;
import com.shangdao.phoenix.entity.menu.Menu;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name="role")
public class Role implements IBaseEntity , Serializable{
	
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
	
	@Column(name="name",unique=true)
    private String name;
	@NotNull
	@Column(name="code",unique=true)
	private String code;
	
	@ManyToMany(mappedBy="roles")
	private Set<User> users;
	
	@ManyToMany(mappedBy="roles")
	private Set<Act> acts;
	
	@ManyToMany(mappedBy="roles")
	private Set<Menu> menus;

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
	
	@Column(name="created_at")
	@JsonIgnore
	private Date createdAt;

	@Override
	public State getState() {
		return state;
	}

	@Override
	public void setState(State state) {
		this.state = state;
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

	public Set<Menu> getMenus() {
		return menus;
	}

	public void setMenus(Set<Menu> menus) {
		this.menus = menus;
	}

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
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

	public Set<Act> getActs() {
		return acts;
	}

	public void setActs(Set<Act> acts) {
		this.acts = acts;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	

}