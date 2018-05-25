package com.shangdao.phoenix.entity.interfaces;

import java.io.Serializable;
import java.util.Date;

import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.state.State;
import com.shangdao.phoenix.entity.user.User;

public interface IBaseEntity extends Serializable {
	public long getId();
	public void setId(long id);
	public String getName();
	public void setName(String name);
	public Date getCreatedAt();
	public void setCreatedAt(Date createdAt);
	public EntityManager getEntityManager();
	public void setEntityManager(EntityManager entityManager);
	public Date getDeletedAt();
	public void setDeletedAt(Date deletedAt);
	public User getCreatedBy();
	public void setCreatedBy(User user);
	public State getState();
	public void setState(State state);
	
}
