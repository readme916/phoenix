package com.shangdao.phoenix.controller;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.OutsideRuntimeException;


@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(java.lang.RuntimeException.class)
	@ResponseBody
	public HTTPResponse handle(java.lang.RuntimeException ex) {
		ex.printStackTrace();
		if (ex instanceof OutsideRuntimeException) {
			if(((OutsideRuntimeException)ex).getCode() == 1000 ){
				return new HTTPResponse(((OutsideRuntimeException)ex).getCode(), "格式错误",((OutsideRuntimeException)ex).getError());
			}else{
				return new HTTPResponse(((OutsideRuntimeException)ex).getCode(), "外部错误",((OutsideRuntimeException)ex).getError());
			}
		}else{
			return new HTTPResponse(500, "内部错误",Collections.EMPTY_SET);
		}
	}
}