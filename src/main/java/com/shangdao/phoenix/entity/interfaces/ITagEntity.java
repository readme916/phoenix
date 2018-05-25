package com.shangdao.phoenix.entity.interfaces;

import java.util.Date;
import java.util.Set;

public interface ITagEntity<T extends ITag> extends IBaseEntity{
	public Set<T> getTags();
}
