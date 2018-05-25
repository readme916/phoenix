package com.shangdao.phoenix.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.service.GetMethodService;
import com.shangdao.phoenix.service.PostMethodService;
import com.shangdao.phoenix.util.AbstractPostGetMethodService;
import com.shangdao.phoenix.util.CommonUtils;

@Controller
@RequestMapping("/entity")
public class EntityController {

	@Autowired
	private GetMethodService getMethodService;
	
	@Autowired
	private PostMethodService postMethodService;
	
	
	@RequestMapping(path="/{entityName}/{act}/{mode}",method=RequestMethod.GET)
	@ResponseBody
	public Object read(@PathVariable String entityName,@PathVariable String act,@PathVariable String mode,@RequestParam(required = false) Map<String, String> params ,  Pageable page ) {
		
		return getMethodService.getDispatch(entityName, act, mode, params , page);
	}
	
	@RequestMapping(path="/{entityName}/{act}",method=RequestMethod.GET)
	@ResponseBody
	public Object read(@PathVariable String entityName,@PathVariable String act,@RequestParam(required = false) Map<String, String> params , Pageable page) {
		
		return getMethodService.getDispatch(entityName, act, null, params , page);
	}
	
	@RequestMapping(path="/{entityName}/{act}/{mode}",method=RequestMethod.POST)
	@ResponseBody
	public Object write(@PathVariable String entityName,@PathVariable String act,@PathVariable(required=false) String mode, @RequestBody(required = true) String body) {
		return postMethodService.postDispatch(entityName, act, mode, body);
	}
	@RequestMapping(path="/{entityName}/{act}",method=RequestMethod.POST)
	@ResponseBody
	public Object write(@PathVariable String entityName,@PathVariable String act, @RequestBody(required = true)  String body) {
		return postMethodService.postDispatch(entityName, act, null, body);
	}
	
}
