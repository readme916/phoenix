package com.shangdao.phoenix;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.department.DepartmentRepository;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.InitService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Application.class })
public class Db {

	@Autowired
	private InitService entitySerivce;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DepartmentRepository departmentRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	@Test
	public void exampleTest() throws ParserConfigurationException, SAXException, IOException {
		
		List<Department> findAll = departmentRepository.findAll();
		List<User> findByDepartmentsIn = userRepository.findDistinctByDepartmentsIn(findAll.stream().collect(Collectors.toSet()));
		System.out.println(findByDepartmentsIn.size());
		Role findOne = roleRepository.findOne(1L);
		List<User> findDistinctByRoles = userRepository.findByRoles(findOne);
		System.out.println(findDistinctByRoles.size());
	}

}
