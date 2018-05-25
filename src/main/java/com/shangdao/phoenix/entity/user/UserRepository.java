package com.shangdao.phoenix.entity.user;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.user.User.Source;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;

public interface UserRepository extends JpaRepository<User, Long> {

	User findByUsernameAndSource(String s,Source source);
	List<User> findDistinctByDepartmentsIn(Set<Department> departments);
	List<User> findByRoles(Role role);
}
