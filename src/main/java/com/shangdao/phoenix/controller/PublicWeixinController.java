package com.shangdao.phoenix.controller;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.qq.weixin.work.aes.WXBizMsgCrypt;
import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.department.DepartmentRepository;
import com.shangdao.phoenix.entity.role.Role;
import com.shangdao.phoenix.entity.role.RoleRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.User.Status;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.service.PublicWeixinService;
import com.shangdao.phoenix.service.WorkWeixinContactService;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.OutsideRuntimeException;
import com.shangdao.phoenix.util.HTTPHeader.Terminal;

@Controller
public class PublicWeixinController {
	
	@Autowired
	private PublicWeixinService publicWeixinService;

	@RequestMapping(path = "/wxpublic/api", method = RequestMethod.GET)
	@ResponseBody
	public Object getApi(HttpServletRequest req){
		return publicWeixinService.getApi(req);
	}
	@RequestMapping(path = "/wxpublic/api", method = RequestMethod.POST)
	@ResponseBody
	public Object postApi(HttpServletRequest req, @RequestBody String body){
		return publicWeixinService.postApi(req,body);
	}
	
}
