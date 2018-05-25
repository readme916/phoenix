package com.shangdao.phoenix.entity.interfaces;

import java.util.Set;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.user.User;

public interface IProjectEntity <L extends ILog,F extends IFile,N extends INoticeLog> extends ILogEntity<L, F, N>{
	public User getManager();
	public Set<User> getMembers();
	public Set<User> getSubscribers(); 
	public Set<Department> getDepartments();
}
