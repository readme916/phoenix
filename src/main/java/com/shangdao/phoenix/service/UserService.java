package com.shangdao.phoenix.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.example.Example;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.User.Source;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.PostMethodService.PostMethodWrapper;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;
import com.shangdao.phoenix.util.OutsideRuntimeException;
import com.shangdao.phoenix.util.UserDetailsImpl;


@Service
public class UserService implements InterfaceEntityService , UserDetailsService{

	@Autowired
	private InitService initService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Value("${salt}")
	private String salt;
	
	@Value("${work.weixin.enable}")
	private boolean workWeixinEnable;
	
	@Autowired
	private WorkWeixinContactService weixinService;
	
	@Autowired
	private HttpServletRequest request;

	
	//最好不要修改oldInstance
	public void create(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance){
		User user = (User)postBody;
		
		String username = user.getUsername();
		String password = user.getPassword();
		String password2 = user.getPassword2();
		
		User existUser = userRepository.findByUsernameAndSource(username, Source.WEB);
		if(existUser!=null){
			throw new OutsideRuntimeException(6611, "用户名已存在");
		}
		
		if(!password.equals(password2)){
			throw new OutsideRuntimeException(6133,"两次密码不一样");
		}
		String md5password = CommonUtils.MD5Encode(password, salt);
		user.setPassword(md5password);
		
		Role guestRole = roleRepository.findByCode("GUEST");
		HashSet<Role> roles = new HashSet<Role>();
		roles.add(guestRole);
		user.setRoles(roles);
		
		System.out.println("根据old的实体和业务逻辑，改变postObject后,包括其他数据库操作");
	}
	
	public void update(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance){
		User post = (User)postBody;
		User old = (User)oldInstance;
		Set<Role> roles = post.getRoles();
		//如果没有提交部门，部门只增加不减少
		if(roles!=null){
			throw new OutsideRuntimeException(7121, "只能在企业微信的用户编辑-职务里提交角色名字，用“,”分隔多个角色");
		}
	}
	
	@Override
	public UserDetails loadUserByUsername(String s){
		
		User user = userRepository.findByUsernameAndSource(s, Source.WEB);
		if (user == null) {
			throw new UsernameNotFoundException("用户名不存在");
		}
		System.out.println("username:" + user.getUsername());
		return new UserDetailsImpl(user);
	}
	

	@Override
	@PostConstruct
	public void registerService() {
		initService.getStructure(User.class).setEntityService(this);
		
	}
}
