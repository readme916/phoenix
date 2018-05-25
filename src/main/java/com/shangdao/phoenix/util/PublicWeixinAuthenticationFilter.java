package com.shangdao.phoenix.util;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.service.PublicWeixinService;
import com.shangdao.phoenix.service.WorkWeixinContactService;

public class PublicWeixinAuthenticationFilter extends WorkWeixinAuthenticationFilter {
	@Autowired
	private PublicWeixinService publicWeixinService;
	
	public PublicWeixinAuthenticationFilter(String point) {
		super(point);
		// TODO Auto-generated constructor stub
	}
	
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		try {
			if (request.getParameter("code") == null) {
				throw new RuntimeException("用户禁止授权");
			}
			Authentication authRequest = publicWeixinService.publicWeixinAuthorize(request.getParameter("code"));
			return authRequest;
			

		} catch (OutsideRuntimeException failReturnObject) {

			HTTPResponse httpResponse = new HTTPResponse(((OutsideRuntimeException)failReturnObject).getCode(), "外部错误",((OutsideRuntimeException)failReturnObject).getError());
			String json = mapper.writeValueAsString(httpResponse);
			response.setContentType("application/json;charset=utf-8");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(json);
			return null;
		}

	}
}
