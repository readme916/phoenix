package com.shangdao.phoenix.service;

import java.io.IOException;
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

import javax.swing.plaf.synth.SynthSeparatorUI;
import javax.transaction.Transactional;

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
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.shangdao.phoenix.util.OutsideRuntimeException;
import com.shangdao.phoenix.util.UserDetailsImpl;

@Service
public class WorkWeixinLoginService {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private UserRepository userRepository;

	@Value("${work.weixin.appid}")
	private String appid;
	@Value("${work.weixin.agent.login.id}")
	private String agentLoginId;
	@Value("${work.weixin.agent.login.secret}")
	private String agentLoginSecret;
	@Value("${work.weixin.agent.login.redirect_uri}")
	private String agentLoginRedirectUri;
	
	@Value("${work.weixin.enable}")
	private boolean workEnable;

	public Authentication workWeixinAuthorize(String code) {

		Map info = _getTemplate("https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?code=" + code , agentLoginId , agentLoginSecret);
		String username = info.get("UserId").toString();
		User user = userRepository.findByUsernameAndSource(username, Source.WXWORK);
		if (user == null || user.getDeletedAt() != null) {
			throw new OutsideRuntimeException(1339, "用户不存在");
		}
		UserDetailsImpl userDetailsImpl = new UserDetailsImpl(user);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetailsImpl, null,
				userDetailsImpl.getAuthorities());
		return authentication;
	}
	public String getWorkWeixinOAuthURL() {
		if (!workEnable) {
			throw new OutsideRuntimeException(1659, "微信企业号功能没有启用");
		}

		try {
			String encodeUrl = URLEncoder.encode(agentLoginRedirectUri, "UTF-8");
			String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appid + "&redirect_uri="
					+ encodeUrl + "&response_type=code&scope=snsapi_base&agentid=" + agentLoginId
					+ "&state=STATE#wechat_redirect";
			return "redirect:" + url;

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public Map sendTextToUserByLoginAgent(String userId, String content, String agentid, String secret ) {
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		hashMap.put("touser", userId);
		hashMap.put("msgtype", "text");
		hashMap.put("agentid", agentLoginId);
		HashMap<String, String> hashMap2 = new HashMap<String, String>();
		hashMap2.put("content", content);
		hashMap.put("text", hashMap2);
		return _postTemplate("https://qyapi.weixin.qq.com/cgi-bin/message/send", hashMap, agentid,
				secret);
	}
	
	protected String getAccessToken(String agentid , String secret) {

		String accessToken = cacheManager.getCache("workWeixinAccessToken").get(agentid, String.class);
		if (accessToken == null) {
			RestTemplate restTemplate = new RestTemplate();
			Map map = restTemplate.getForObject(
					"https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=" + appid + "&corpsecret=" + secret,
					Map.class);
			if (map.get("errcode").toString().equals("0")) {
				cacheManager.getCache("workWeixinAccessToken").put(agentid, map.get("access_token").toString());
				return map.get("access_token").toString();
			} else {
				throw new OutsideRuntimeException((Integer) map.get("errcode"), map.get("errmsg"));
			}

		} else {
			return accessToken;
		}
	}

	protected Map _getTemplate(String url , String agentid, String secret) {
		if (!workEnable) {
			throw new OutsideRuntimeException(7612, "没有启动企业微信功能");
		}
		String accessToken = getAccessToken(agentid,secret);
		RestTemplate restTemplate = new RestTemplate();
		Map info;
		if (url.contains("?")) {
			info = restTemplate.getForObject(url + "&access_token=" + accessToken, Map.class);
		} else {
			info = restTemplate.getForObject(url + "?access_token=" + accessToken, Map.class);
		}
		if (info.get("errcode").toString().equals("0")) {
			return info;
		} else if (info.get("errcode").toString().equals("40014") || info.get("errcode").toString().equals("42001")) {
			cacheManager.getCache("workWeixinAccessToken").evict(agentid);
			return _getTemplate(url,agentid,secret);
		} else {

			throw new OutsideRuntimeException((Integer) info.get("errcode"), info.get("errmsg"));
		}
	}

	protected Map _postTemplate(String url, Map params , String agentid, String secret) {
		if (!workEnable) {
			throw new OutsideRuntimeException(7612, "没有启动企业微信功能");
		}
		String accessToken = getAccessToken(agentid, secret);
		RestTemplate restTemplate = new RestTemplate();
		String urlWithToken;
		if (url.contains("?")) {
			urlWithToken = url + "&access_token=" + accessToken;
		} else {
			urlWithToken = url + "?access_token=" + accessToken;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		// 将提交的数据转换为String
		// 最好通过bean注入的方式获取ObjectMapper
		ObjectMapper mapper = new ObjectMapper();
		String value;
		Map info;
		try {
			value = mapper.writeValueAsString(params);

			HttpEntity<String> requestEntity = new HttpEntity<String>(value, headers);
			// 执行HTTP请求
			info = restTemplate.postForObject(urlWithToken, requestEntity, Map.class);

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OutsideRuntimeException(7231, "post提交给微信错误");
		}

		if (info.get("errcode").toString().equals("0")) {
			return info;
		} else if (info.get("errcode").toString().equals("40014") || info.get("errcode").toString().equals("42001")) {
			cacheManager.getCache("workWeixinAccessToken").evict(agentid);
			return _postTemplate(url, params, agentid, secret);
		} else {

			throw new OutsideRuntimeException((Integer) info.get("errcode"), info.get("errmsg"));
		}
	}
	
	protected Map sendTextToUserByLoginAgent(String userId, String content) {
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		hashMap.put("touser", userId);
		hashMap.put("msgtype", "text");
		hashMap.put("agentid", agentLoginId);
		HashMap<String, String> hashMap2 = new HashMap<String, String>();
		hashMap2.put("content", content);
		hashMap.put("text", hashMap2);
		return _postTemplate("https://qyapi.weixin.qq.com/cgi-bin/message/send", hashMap, agentLoginId,
				agentLoginSecret);
	}
}
