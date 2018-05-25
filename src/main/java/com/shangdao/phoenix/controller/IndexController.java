package com.shangdao.phoenix.controller;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.jpa.HibernateEntityManager;
import org.mockito.internal.verification.Only;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.qq.weixin.work.aes.AesException;
import com.qq.weixin.work.aes.WXBizMsgCrypt;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.state.StateRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.GetMethodService;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.service.PostMethodService;
import com.shangdao.phoenix.service.PublicWeixinService;
import com.shangdao.phoenix.service.WorkWeixinContactService;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.EntityStructure;
import com.shangdao.phoenix.util.HTTPResponse;

@Controller
public class IndexController {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PostMethodService postMethodService;

	@Autowired
	private GetMethodService getMethodService;

	@Autowired
	private WorkWeixinContactService workWeixinService;

	@Autowired
	private PublicWeixinService publicWeixinService;

	@Value("${work.weixin.appid}")
	private String appid;
	@Value("${work.weixin.agent.login.id}")
	private String agentLoginId;
	@Value("${work.weixin.agent.login.redirect_uri}")
	private String agentLoginRedirectUri;

	@Value("${work.weixin.enable}")
	private boolean workEnable;
	@Value("${public.weixin.enable}")
	private boolean publicEnable;

	@ResponseBody
	@RequestMapping("/myinfo")
	public HTTPResponse myInfo() {
		return getMethodService.myInfo();
	}

	@RequestMapping("/")
	public String home(@RequestHeader HttpHeaders headers) {
		String ua = headers.getFirst("User-Agent");
		System.out.println(ua);
		if (ua.indexOf("wxwork") != -1) {
			if (ua.indexOf("Android") != -1 || ua.indexOf("iPhone") != -1) {
				return "/wxwork-mobile/index";
			} else {
				return "/wxwork-desk/index";
			}
		} else if (ua.indexOf("MicroMessenger") != -1) {
			if(ua.contains("WindowsWechat")){
				return "/wxpublic-desk/index";
			}else{
				return "/wxpublic-mobile/index";
			}
		} else if (ua.contains("Electron")) {
			return "/electron/index";
		} else {
			return "/browser/index";
		}
	}

	@RequestMapping("/login")
	public String worklogin(Model model, HttpServletRequest request, HttpServletResponse response,
			@RequestHeader HttpHeaders headers) {

		String ua = headers.getFirst("User-Agent");
		if (workEnable && ua.contains("wxwork")) {
			return workWeixinService.getWorkWeixinOAuthURL();
		} else if (publicEnable && ua.contains("MicroMessenger")) {
			return publicWeixinService.getPublicWeixinOAuthURL();
		} else {
			if (workEnable) {
				model.addAttribute("appid", appid);
				model.addAttribute("agentid", agentLoginId);
				model.addAttribute("redirect_uri", agentLoginRedirectUri);
			}
			if (ua.contains("Electron")) {
				return "/electron/login";
			} else {
				return "/browser/login";
			}
		}
	}
	// 一个以某个用户的身份，执行动作的例子
	// @ResponseBody
	// @RequestMapping("/change")
	// public Object change() {
	// User systemUser = InitService.getSystemUser().getUser();
	// return postMethodService.postDispatchByUser("example", "create", "all",
	// "{\"text\":\"wokao11111111111\",\"score\":88,\"name\":\"liyang11111111111\"}",
	// systemUser);
	// }

}
