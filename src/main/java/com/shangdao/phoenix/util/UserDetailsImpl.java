package com.shangdao.phoenix.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.user.User;

public class UserDetailsImpl implements UserDetails {

	private User user;

	private Set<Department> departments = new HashSet<Department>();

	private Collection<? extends GrantedAuthority> authorities;

	public UserDetailsImpl(User user) {
		this.user = user;
		if (user.getDepartments() != null) {
			for (Department d : user.getDepartments()) {
				this.departments.add(d);
			}
		}
		List<GrantedAuthority> auths = new ArrayList<>();
		if (user.getRoles() != null) {
			Set<Role> roles = user.getRoles();
			for (Role r : roles) {
				auths.add(new SimpleGrantedAuthority(r.getCode()));
			}
		}
		this.authorities = auths;
	}

	public User getUser() {
		return this.user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public Set<Department> getDepartments() {
		return departments;
	}

	public long getId() {
		return this.user.getId();
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return this.user.getPassword();
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return this.user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		if(this.user.getDeletedAt()!=null){
			return false;
		}else{
			return true;
		}
	}

}
