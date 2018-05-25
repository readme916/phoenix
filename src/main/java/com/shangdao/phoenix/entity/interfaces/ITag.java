package com.shangdao.phoenix.entity.interfaces;

import java.util.Date;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.user.User;

public interface ITag<E> extends IBaseEntity{
	public E getEntity();
	public void setEntity(E entity);
}
