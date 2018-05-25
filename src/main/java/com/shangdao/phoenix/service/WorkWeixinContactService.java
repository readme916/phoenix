package com.shangdao.phoenix.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.swing.plaf.synth.SynthSeparatorUI;
import javax.transaction.Transactional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.weixin.work.aes.WXBizMsgCrypt;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.department.DepartmentRepository;
import com.shangdao.phoenix.entity.entityManager.EntityManager;
import com.shangdao.phoenix.entity.entityManager.EntityManagerRepository;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.User.Gender;
import com.shangdao.phoenix.entity.user.User.Source;
import com.shangdao.phoenix.entity.user.User.Status;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.EntityStructure;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;
import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.OutsideRuntimeException;
import com.shangdao.phoenix.util.UserDetailsImpl;

@Service
public class WorkWeixinContactService extends WorkWeixinLoginService {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private UserRepository userRepository;

	@Value("${work.weixin.appid}")
	private String appid;
	@Value("${work.weixin.agent.contact.id}")
	private String agentContactId;
	@Value("${work.weixin.agent.contact.secret}")
	private String agentContactSecret;
	@Value("${work.weixin.agent.contact.token}")
	private String contactToken;
	@Value("${work.weixin.agent.contact.encodingAESKey}")
	private String contactEncodingAESKey;

	@Value("${work.weixin.enable}")
	private boolean workEnable;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private EntityManagerRepository entityManagerRepository;

	public Object getApi(HttpServletRequest req) {
		if (!workEnable) {
			throw new OutsideRuntimeException(6981, "企业微信服务没启动");
		}
		try {
			WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(contactToken, contactEncodingAESKey, appid);
			String sVerifyMsgSig = req.getParameter("msg_signature");
			String sVerifyTimeStamp = req.getParameter("timestamp");
			String sVerifyNonce = req.getParameter("nonce");

			if (req.getParameter("echostr") != null) {
				String sVerifyEchoStr = req.getParameter("echostr");
				String sEchoStr = wxcpt.VerifyURL(sVerifyMsgSig, sVerifyTimeStamp, sVerifyNonce, sVerifyEchoStr);
				return sEchoStr;
			}

		} catch (Exception e) {
			// TODO
			// 解密失败，失败原因请查看异常
			e.printStackTrace();
		}
		return null;
	}

	public Object postApi(HttpServletRequest req, String body) {
		if (!workEnable) {
			throw new OutsideRuntimeException(6981, "企业微信服务没启动");
		}
		try {
			WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(contactToken, contactEncodingAESKey, appid);
			String sVerifyMsgSig = req.getParameter("msg_signature");
			String sVerifyTimeStamp = req.getParameter("timestamp");
			String sVerifyNonce = req.getParameter("nonce");

			String sMsg = wxcpt.DecryptMsg(sVerifyMsgSig, sVerifyTimeStamp, sVerifyNonce, body);
			System.out.println("after decrypt msg: " + sMsg);
			// TODO: 解析出明文xml标签的内容进行处理
			// For example:
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			StringReader sr = new StringReader(sMsg);
			InputSource is = new InputSource(sr);
			Document document = db.parse(is);
			Element root = document.getDocumentElement();
			
			NodeList msgTypeNode = root.getElementsByTagName("MsgType");
			String msgTypeContent = msgTypeNode.item(0).getTextContent();
			
			
			if(msgTypeContent.equals("event")){
				return _handleEvent(root);
			}else if(msgTypeContent.equals("text")){
				
			}
			
			
		} catch (Exception e) {
			// TODO
			// 解密失败，失败原因请查看异常
			e.printStackTrace();
		}
		return null;
	}


	public void setupDepartments() {
		Map info = _getTemplate("https://qyapi.weixin.qq.com/cgi-bin/department/list", agentContactId,
				agentContactSecret);
		EntityStructure structure = InitService.getStructure("department");
		Long entityManagerId = structure.getEntityManagerId();
		EntityManager findOne = entityManagerRepository.findOne(entityManagerId);
		List<Map> lm = (List<Map>) info.get("department");
		HashMap<Long, Department> departmentsMap = new HashMap<Long, Department>();

		for (Map row : lm) {
			Department department = new Department();
			department.setName(row.get("name").toString());
			department.setCreatedAt(new Date());
			department.setWorkWeixinId(Long.valueOf((row.get("id").toString())));
			department.setEntityManager(findOne);
			department.setSortNumber(Long.valueOf(row.get("order").toString()));
			if (row.get("parentid") != null
					&& departmentsMap.containsKey(Long.valueOf(row.get("parentid").toString()))) {
				department.setParent(departmentsMap.get(Long.valueOf(row.get("parentid").toString())));
			}
			System.out.println(department.getId());
			Department save = departmentRepository.save(department);
			departmentsMap.put(department.getWorkWeixinId(), save);
		}
	}

	public void setupUsers() {
		Map info = _getTemplate("https://qyapi.weixin.qq.com/cgi-bin/user/list?department_id=1&fetch_child=1",
				agentContactId, agentContactSecret);
		EntityStructure structure = InitService.getStructure("user");
		Long entityManagerId = structure.getEntityManagerId();
		EntityManager findOne = entityManagerRepository.findOne(entityManagerId);
		List<Map> list = (List<Map>) info.get("userlist");
		for (Map row : list) {
			User user = new User();
			user.setUsername(row.get("userid").toString());
			user.setName(CommonUtils.filterEmoji(row.get("name").toString()));
			user.setCreatedAt(new Date());
			user.setSource(Source.WXWORK);
			user.setEntityManager(findOne);
			if (row.get("department") != null && !((List) (row.get("department"))).isEmpty()) {
				HashSet<Department> hashSet = new HashSet<Department>();
				for (Object d : (List) (row.get("department"))) {
					Long departmentId = Long.valueOf(d.toString());
					Department findOne2 = departmentRepository.findByWorkWeixinId(departmentId);
					if (findOne2 == null) {
						throw new OutsideRuntimeException(1512, "department_id为" + departmentId + "部门没有设置对");
					} else {
						hashSet.add(findOne2);
					}
				}
				user.setDepartments(hashSet);
			}
			user.setPosition(row.get("position").toString());
			if(row.get("position")!=null && !row.get("position").toString().equals("")){
				HashSet<Role> hashSet = new HashSet<Role>();
				String[] split;
				String position = row.get("position").toString();
				if (position.contains("，")) {
					split = position.split("，");
				} else {
					split = position.split(",");
				}

				for (String roleName : split) {
					Role findByName = roleRepository.findByName(roleName);
					if (findByName != null) {
						hashSet.add(findByName);
					}
				}
				user.setRoles(hashSet);
			}
			
			
			user.setMobile(row.get("mobile").toString());
			long gender = Long.valueOf(row.get("gender").toString());
			if (gender == 1L) {
				user.setGender(Gender.MALE);
			} else if (gender == 2L) {
				user.setGender(Gender.FEMALE);
			}

			user.setEmail(row.get("email").toString());
			long leader = Long.valueOf(row.get("isleader").toString());
			if (leader == 1L) {
				user.setLeader(true);
			} else {
				user.setLeader(false);
			}
			user.setAvatar(row.get("avatar").toString());
			user.setTelephone(row.get("telephone").toString());
			user.setEnglishName(row.get("english_name").toString());
			long statusNumber = Long.valueOf(row.get("status").toString());
			User.Status status = null;
			if (statusNumber == 1L) {
				status = User.Status.ACTIVE;
			} else if (statusNumber == 2L) {
				status = User.Status.FORBIDDEN;
			} else if (statusNumber == 4L) {
				status = User.Status.INACTIVE;
			}
			user.setStatus(status);
			userRepository.save(user);
		}
	}
	
	
	
	
	private HTTPResponse _handleEvent(Element root) {
		NodeList eventNode = root.getElementsByTagName("Event");
		String eventContent = eventNode.item(0).getTextContent();
		
		if (eventContent.equals("change_contact")) {
			NodeList changeTypeNode = root.getElementsByTagName("ChangeType");
			String changeTypeContent = changeTypeNode.item(0).getTextContent();
			
			if ("create_user".equals(changeTypeContent)) {
				String userId = root.getElementsByTagName("UserID").item(0).getTextContent();
				User findByUsername = userRepository.findByUsernameAndSource(userId, Source.WXWORK);
				if (findByUsername == null) {
					findByUsername = new User();
					findByUsername.setCreatedAt(new Date());
				}
				fillWorkWeixinToUser(root, findByUsername);
				findByUsername.setDeletedAt(null);
				findByUsername.setStatus(Status.INACTIVE);
				userRepository.save(findByUsername);
				
			} else if ("update_user".equals(changeTypeContent)) {
				String userId = root.getElementsByTagName("UserID").item(0).getTextContent();
				User findByUsername = userRepository.findByUsernameAndSource(userId, Source.WXWORK);
				if (findByUsername != null) {
					fillWorkWeixinToUser(root, findByUsername);
					userRepository.save(findByUsername);
				}
				
			} else if ("delete_user".equals(changeTypeContent)) {
				String userId = root.getElementsByTagName("UserID").item(0).getTextContent();
				User findByUsername = userRepository.findByUsernameAndSource(userId, Source.WXWORK);
				if (findByUsername != null) {
					findByUsername.setDeletedAt(new Date());
					findByUsername.setDepartments(null);
					HashSet<Role> roleSet = new HashSet<Role>();
					Role findByCode = roleRepository.findByCode("GUEST");
					roleSet.add(findByCode);
					findByUsername.setRoles(roleSet);
					userRepository.save(findByUsername);
				}
				
			} else if ("create_party".equals(changeTypeContent)) {
				String id = root.getElementsByTagName("Id").item(0).getTextContent();
				Department department = departmentRepository.findByWorkWeixinId(Long.valueOf(id));
				if (department == null) {
					department = new Department();
				} else {
					department.setDeletedAt(null);
					fillWorkWeixinToDepartment(root, department);
					departmentRepository.save(department);
				}
				
			} else if ("update_party".equals(changeTypeContent)) {
				String id = root.getElementsByTagName("Id").item(0).getTextContent();
				Department department = departmentRepository.findByWorkWeixinId(Long.valueOf(id));
				if (department != null) {
					fillWorkWeixinToDepartment(root, department);
					departmentRepository.save(department);
				}
			} else if ("delete_party".equals(changeTypeContent)) {
				String id = root.getElementsByTagName("Id").item(0).getTextContent();
				Department department = departmentRepository.findByWorkWeixinId(Long.valueOf(id));
				if (department != null) {
					department.setDeletedAt(new Date());
					departmentRepository.save(department);
				}
			} else if ("update_tag".equals(changeTypeContent)) {
				
			}
		}
		return new HTTPResponse();
		
		
	}

	private void fillWorkWeixinToUser(Element root, User findByUsername) {

		EntityStructure structure = InitService.getStructure("user");
		Long entityManagerId = structure.getEntityManagerId();
		EntityManager findOne = entityManagerRepository.findOne(entityManagerId);

		NodeList userIdNode = root.getElementsByTagName("UserID");
		String userId = userIdNode.item(0).getTextContent();
		findByUsername.setUsername(userId);
		findByUsername.setEntityManager(findOne);
		findByUsername.setSource(Source.WXWORK);

		NodeList nameNode = root.getElementsByTagName("Name");
		if (nameNode.getLength() != 0) {
			String name = nameNode.item(0).getTextContent();
			findByUsername.setName(name);
		}

		NodeList departmentNode = root.getElementsByTagName("Department");
		if (departmentNode.getLength() == 1) {
			String departmentIdString = departmentNode.item(0).getTextContent();
			HashSet<Department> hashSet = new HashSet<Department>();
			for (Object d : departmentIdString.split(",")) {
				Long departmentId = Long.valueOf(d.toString());
				hashSet.add(departmentRepository.findOne(departmentId));
			}
			findByUsername.setDepartments(hashSet);
		}
		NodeList positionNode = root.getElementsByTagName("Position");
		if (positionNode.getLength() != 0) {
			String position = positionNode.item(0).getTextContent();
			findByUsername.setPosition(position);
			HashSet<Role> hashSet = new HashSet<Role>();
			String[] split;
			if (position.contains("，")) {
				split = position.split("，");
			} else {
				split = position.split(",");
			}

			for (String roleName : split) {
				Role findByName = roleRepository.findByName(roleName);
				if (findByName != null) {
					hashSet.add(findByName);
				}
			}
			findByUsername.setRoles(hashSet);
			String content = "";
			if (!hashSet.isEmpty()) {
				List<String> collect = hashSet.stream().map(e -> e.getName()).collect(Collectors.toList());
				content = "你的职务被设置为:" + String.join(",", collect);
			} else {
				content = "你的职务被设置为:"+position+"（无效），请联系管理员，查看是否正确填写职务名称";
			}
			sendTextToUserByLoginAgent(userId, content);
		}

		NodeList mobileNode = root.getElementsByTagName("Mobile");
		if (mobileNode.getLength() != 0) {
			String mobile = mobileNode.item(0).getTextContent();
			findByUsername.setMobile(mobile);
		}
		NodeList genderNode = root.getElementsByTagName("Gender");
		if (genderNode.getLength() != 0) {
			String gender = genderNode.item(0).getTextContent();
			if (gender.equals("1")) {
				findByUsername.setGender(Gender.MALE);
			} else if (gender.equals("2")) {
				findByUsername.setGender(Gender.FEMALE);
			}
		}

		NodeList emailNode = root.getElementsByTagName("Email");
		if (emailNode.getLength() != 0) {
			String email = emailNode.item(0).getTextContent();
			findByUsername.setEmail(email);
		}

		NodeList statusNode = root.getElementsByTagName("Status");
		if (statusNode.getLength() != 0) {
			String statusString = statusNode.item(0).getTextContent();
			Status status = null;
			if (statusString.equals("1")) {
				status = Status.ACTIVE;
			} else if (statusString.equals("2")) {
				status = Status.FORBIDDEN;
			} else if (statusString.equals("4")) {
				status = Status.INACTIVE;
			}
			findByUsername.setStatus(status);
		}

		NodeList avatarNode = root.getElementsByTagName("Avatar");
		if (avatarNode.getLength() != 0) {
			String avatar = avatarNode.item(0).getTextContent();
			findByUsername.setAvatar(avatar);
		}

		NodeList englishNameNode = root.getElementsByTagName("EnglishName");
		if (englishNameNode.getLength() != 0) {
			String englishName = englishNameNode.item(0).getTextContent();
			findByUsername.setEnglishName(englishName);
		}

		NodeList isLeaderNode = root.getElementsByTagName("IsLeader");
		if (isLeaderNode.getLength() != 0) {
			String isLeader = isLeaderNode.item(0).getTextContent();
			if (isLeader.equals("0")) {
				findByUsername.setLeader(false);
			} else {
				findByUsername.setLeader(true);
			}
		}

		NodeList telephoneNode = root.getElementsByTagName("Telephone");
		if (telephoneNode.getLength() != 0) {
			String telephone = telephoneNode.item(0).getTextContent();
			findByUsername.setTelephone(telephone);
		}
	}

	private void fillWorkWeixinToDepartment(Element root, Department department) {
		EntityStructure structure = InitService.getStructure("department");
		Long entityManagerId = structure.getEntityManagerId();
		EntityManager findOne = entityManagerRepository.findOne(entityManagerId);

		NodeList idNode = root.getElementsByTagName("Id");
		String id = idNode.item(0).getTextContent();
		department.setWorkWeixinId(Long.valueOf(id));
		department.setEntityManager(findOne);

		NodeList nameNode = root.getElementsByTagName("Name");
		if (nameNode.getLength() != 0) {
			String name = nameNode.item(0).getTextContent();
			department.setName(name);
		}

		NodeList orderNode = root.getElementsByTagName("Order");
		if (orderNode.getLength() != 0) {
			String order = orderNode.item(0).getTextContent();
			department.setSortNumber(Long.valueOf(order));
		}
		NodeList parentIdNode = root.getElementsByTagName("ParentId");
		if (parentIdNode.getLength() != 0) {
			String parentId = parentIdNode.item(0).getTextContent();
			Department parent = departmentRepository.findOne(Long.valueOf(parentId));
			department.setParent(parent);
		}

	}

}
