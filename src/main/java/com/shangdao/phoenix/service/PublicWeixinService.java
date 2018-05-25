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
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;
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
import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;
import com.shangdao.phoenix.util.OutsideRuntimeException;
import com.shangdao.phoenix.util.UserDetailsImpl;

@Service
public class PublicWeixinService {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private UserRepository userRepository;

	@Value("${public.weixin.appid}")
	private String appid;
	@Value("${public.weixin.secret}")
	private String secret;
	@Value("${public.weixin.redirect_uri}")
	private String redirect_uri;
	
	@Value("${public.weixin.token}")
	private String token;
	@Value("${public.weixin.encodingAESKey}")
	private String encodingAESKey;
	
	@Value("${public.weixin.enable}")
	private boolean publicEnable;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private EntityManagerRepository entityManagerRepository;
	
	
	public Map sendMessageToUser(String openId, String templateId ,String templateURL,String templateData) {
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		hashMap.put("touser", openId);
		hashMap.put("template_id", templateId);
		if(templateURL!=null){
			hashMap.put("url", templateURL);
		}
		hashMap.put("data", stringToMap(templateData));
		return _postTemplate("https://api.weixin.qq.com/cgi-bin/message/template/send", hashMap);
	}
	
	public Object getApi(HttpServletRequest req){
		if (!publicEnable) {
			throw new OutsideRuntimeException(6981, "微信公众号服务没启动");
		}
		try {
			WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(token, encodingAESKey, appid);
			String sVerifyMsgSig = req.getParameter("signature");
			String sVerifyTimeStamp = req.getParameter("timestamp");
			String sVerifyNonce = req.getParameter("nonce");
			if (req.getParameter("echostr") != null) {
				String sVerifyEchoStr = req.getParameter("echostr");
//				String sEchoStr = wxcpt.verifyUrl(sVerifyMsgSig, sVerifyTimeStamp, sVerifyNonce, sVerifyEchoStr);
				return sVerifyEchoStr;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public String postApi(HttpServletRequest req,String body){
		if (!publicEnable) {
			throw new OutsideRuntimeException(6981, "微信公众号服务没启动");
		}
		try {
			WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(token, encodingAESKey, appid);
			String sVerifyMsgSig = req.getParameter("msg_signature");
			String sVerifyTimeStamp = req.getParameter("timestamp");
			String sVerifyNonce = req.getParameter("nonce");
			String sMsg = wxcpt.decryptMsg(sVerifyMsgSig, sVerifyTimeStamp, sVerifyNonce, body);
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
				_handleEvent(root);
			}else if(msgTypeContent.equals("text")){
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return "success";
	}

	public String getPublicWeixinOAuthURL() {
		if (!publicEnable) {
			throw new OutsideRuntimeException(1659, "微信公众号功能没有启用");
		}

		try {
			String urlString = URLEncoder.encode(redirect_uri, "UTF-8");
			String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appid + "&redirect_uri="
					+ urlString + "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
			return "redirect:" + url;

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public Authentication publicWeixinAuthorize(String code) {

		Map info = _getOauth2Template("https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appid + "&secret="
				+ secret + "&code=" + code + "&grant_type=authorization_code");
		String openid = info.get("openid").toString();
		String accessToken = info.get("access_token").toString();
		Map userInfo = _getOauth2Template("https://api.weixin.qq.com/sns/userinfo?access_token=" + accessToken
				+ "&openid=" + openid + "&lang=zh_CN");
		User user = userRepository.findByUsernameAndSource(openid, Source.WXPUBLIC);
		if (user == null) {
			// 自动注册
			if (userInfo.get("errorcode") == null) {
				user = new User();
				user.setCreatedAt(new Date());
				user.setSource(Source.WXPUBLIC);
				EntityStructure structure = InitService.getStructure("user");
				Long entityManagerId = structure.getEntityManagerId();
				EntityManager findOne = entityManagerRepository.findOne(entityManagerId);
				user.setEntityManager(findOne);
				HashSet<Role> hashSet = new HashSet<Role>();
				Role findByCode = roleRepository.findByCode("GUEST");
				hashSet.add(findByCode);
				user.setRoles(hashSet);
				user.setUsername(userInfo.get("openid").toString());

			} else {
				throw new OutsideRuntimeException((Integer) userInfo.get("errcode"), userInfo.get("errmsg"));
			}
		}

		try {
			user.setDeletedAt(null);
			user.setName(CommonUtils
					.filterEmoji(new String(userInfo.get("nickname").toString().getBytes("ISO-8859-1"), "utf-8")));
			user.setAvatar(userInfo.get("headimgurl").toString());
			if (userInfo.get("sex").toString().equals("1")) {
				user.setGender(Gender.MALE);
			} else if (userInfo.get("sex").toString().equals("2")) {
				user.setGender(Gender.FEMALE);
			}
			user.setProvince(new String(userInfo.get("province").toString().getBytes("ISO-8859-1"), "utf-8"));
			user.setCity(new String(userInfo.get("city").toString().getBytes("ISO-8859-1"), "utf-8"));
			user.setCountry(new String(userInfo.get("country").toString().getBytes("ISO-8859-1"), "utf-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		userRepository.save(user);

		UserDetailsImpl userDetailsImpl = new UserDetailsImpl(user);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetailsImpl, null,
				userDetailsImpl.getAuthorities());

		return authentication;

	}
	
	
	private void _handleEvent(Element root) {
		NodeList eventNode = root.getElementsByTagName("Event");
		String eventContent = eventNode.item(0).getTextContent();
		NodeList fromUserNameNode = root.getElementsByTagName("FromUserName");
		String fromUserNameContent = fromUserNameNode.item(0).getTextContent();
		if (eventContent.equals("unsubscribe")) {
			User findByUsername = userRepository.findByUsernameAndSource(fromUserNameContent, Source.WXPUBLIC);
			if (findByUsername != null) {
				findByUsername.setDeletedAt(new Date());
				userRepository.save(findByUsername);
			}
		}
	}

	private String getCommonAccessToken(String appid) {

		String accessToken = cacheManager.getCache("publicCommonWeixinAccessToken").get(appid, String.class);
		if (accessToken == null) {
			RestTemplate restTemplate = new RestTemplate();
			String strInfo = restTemplate
					.getForObject("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appid
							+ "&secret=" + secret, String.class);
			Map map = stringToMap(strInfo);
			if (map.get("errcode") == null || map.get("errcode").toString().equals("0")) {
				cacheManager.getCache("publicCommonWeixinAccessToken").put(appid, map.get("access_token").toString());
				return map.get("access_token").toString();
			} else {
				throw new OutsideRuntimeException((Integer) map.get("errcode"), map.get("errmsg"));
			}

		} else {
			return accessToken;
		}
	}

	private Map _getOauth2Template(String url) {
		if (!publicEnable) {
			throw new OutsideRuntimeException(7612, "没有启动微信公众号功能");
		}
		RestTemplate restTemplate = new RestTemplate();
		String strInfo = restTemplate.getForObject(url, String.class);
		Map info = stringToMap(strInfo);
		if (info.get("errcode") == null || info.get("errcode").toString().equals("0")) {
			return info;
		} else {
			throw new OutsideRuntimeException((Integer) info.get("errcode"), info.get("errmsg"));
		}

	}

	private Map _getCommonTemplate(String url) {
		if (!publicEnable) {
			throw new OutsideRuntimeException(7612, "没有启动微信公众号功能");
		}
		String accessToken = getCommonAccessToken(appid);
		RestTemplate restTemplate = new RestTemplate();
		String strInfo;
		if (url.contains("?")) {
			strInfo = restTemplate.getForObject(url + "&access_token=" + accessToken, String.class);
		} else {
			strInfo = restTemplate.getForObject(url + "?access_token=" + accessToken, String.class);
		}
		Map info = stringToMap(strInfo);

		if (info.get("errcode") == null || info.get("errcode").toString().equals("0")) {
			return info;
		} else if (info.get("errcode").toString().equals("40014") || info.get("errcode").toString().equals("42001")
				|| info.get("errcode").toString().equals("42007")) {
			cacheManager.getCache("publicCommonWeixinAccessToken").evict(appid);
			return _getCommonTemplate(url);
		} else {
			throw new OutsideRuntimeException((Integer) info.get("errcode"), info.get("errmsg"));
		}
	}
	protected Map _postTemplate(String url, Map params) {
		if (!publicEnable) {
			throw new OutsideRuntimeException(7612, "没有启动微信公众号功能");
		}
		String accessToken = getCommonAccessToken(appid);
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
			throw new OutsideRuntimeException(4231, "post提交给微信错误");
		}

		if (info.get("errcode") == null || info.get("errcode").toString().equals("0")) {
			return info;
		} else if (info.get("errcode").toString().equals("40014") || info.get("errcode").toString().equals("42001")
				|| info.get("errcode").toString().equals("42007")) {
			cacheManager.getCache("publicCommonWeixinAccessToken").evict(appid);
			return _getCommonTemplate(url);
		} else {
			System.out.println(info);
			throw new OutsideRuntimeException((Integer) info.get("errcode"), info.get("errmsg"));
		}
	}
	private Map stringToMap(String str) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map m = mapper.readValue(str, Map.class);
			return m;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new OutsideRuntimeException(1220, "用户微信返回结果json解析失败");
		}
	}

}
