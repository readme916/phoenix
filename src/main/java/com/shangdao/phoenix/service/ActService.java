package com.shangdao.phoenix.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.act.ActNotice;
import com.shangdao.phoenix.entity.act.ActRepository;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.entityManager.EntityManagerRepository;
import com.shangdao.phoenix.entity.example.Example;
import com.shangdao.phoenix.entity.message.Message;
import com.shangdao.phoenix.entity.message.MessageRepository;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.GetMethodService.GetMethodWrapper;
import com.shangdao.phoenix.service.PostMethodService.PostMethodWrapper;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.OutsideRuntimeException;

@Service
public class ActService implements InterfaceEntityService {

	// 保证bean创建顺序，没实际用处
	@Autowired
	private InitService initService;

	@Autowired
	private ActRepository actRepository;
	
	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EntityManagerRepository entityManagerRepository;
	

	// 最好不要修改oldInstance
	public void create(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance) {
		Act post = (Act) postBody;
		if(post.getEntityManager().getId()==0L){
			throw new OutsideRuntimeException(7612, "必须有entityManager.id属性");
		}
		EntityManager findOne = entityManagerRepository.findOne(post.getEntityManager().getId());
		_validActCode(post,findOne);
		_validTargetState(post,findOne);
		_validRoleCode(post,findOne);
	}

	

	public void update(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance) {
		Act post = (Act) postBody;
		Act old = (Act) oldInstance;
		if(post.getEntityManager()!=null){
			EntityManager findOne = entityManagerRepository.findOne(post.getEntityManager().getId());
			_validActCode(post,findOne);
			_validTargetState(post,findOne);
			_validRoleCode(post,findOne);
		}else{
			_validActCode(post,old.getEntityManager());
			_validTargetState(post,old.getEntityManager());
			_validRoleCode(post,old.getEntityManager());
		}
		
	}

	@Override
	@PostConstruct
	public void registerService() {
		initService.getStructure(Act.class).setEntityService(this);

	}
	
	private void _validTargetState(Act post, EntityManager entityManager) {
		if(post.getTargetState()!=null){
			if(!entityManager.isHasStateMachine()){
				throw new OutsideRuntimeException(8615, "动作对象必须是状态机实体");
			}
		}
		
	}
	
	private void _validActCode(Act post, EntityManager entityManager) {
		if(post.getCode()!=null){
			
			Act findByEntityManagerIdAndCode = actRepository.findByEntityManagerIdAndCode(entityManager.getId(), post.getCode());
			if(findByEntityManagerIdAndCode!=null){
				if(post.getId() == findByEntityManagerIdAndCode.getId()){
					return;
				}else{
					throw new OutsideRuntimeException(7671, "同一个实体的动作不能相同code");
				}
			}else{
				return;
			}
		}else{
			return;
		}
	}
	
	private void _validRoleCode(Act post, EntityManager entityManager) {
		// TODO Auto-generated method stub
		
		
		if(post.getActNotices()!=null){
			Set<ActNotice> actNotices = post.getActNotices();
			for (ActNotice actNotice : actNotices) {
				_checkCode(actNotice.getRoleCode(),entityManager);
			}
		}
	}

	private void _checkCode(String noticeImmediatelyFirstRoleCode, EntityManager entityManager) {
		
		String[] projectRoles = new String[]{"MEMBER","MANAGER","SUBSCRIBER","DEPARTMENT"};
		for (String role : projectRoles) {
			if(role.equalsIgnoreCase(noticeImmediatelyFirstRoleCode)){
				if(!entityManager.isHasProject()){
					throw new OutsideRuntimeException(1625, "动作对象必须是项目实体,才能通知到"+role);
				}
				return;
			}
		}
		if(noticeImmediatelyFirstRoleCode.equalsIgnoreCase("CREATOR")){
			return;
		}
		Role findByCode = roleRepository.findByCode(noticeImmediatelyFirstRoleCode);
		if(findByCode==null){
			throw new OutsideRuntimeException(6551, noticeImmediatelyFirstRoleCode+"不是有效的角色code");
		}
		
		
	}
}
