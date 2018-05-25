
package com.shangdao.phoenix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.shangdao.phoenix.service.FileUploadService;
import com.shangdao.phoenix.util.HTTPResponse;

@Controller
public class FileUploadController {

	@Autowired
	private FileUploadService fileUploadService;

	@RequestMapping(value = "/fileUpload", method = RequestMethod.POST)
	@ResponseBody
	public HTTPResponse handleUploadProcess(@RequestParam("file") MultipartFile file) {
		return fileUploadService.upload(file);
	}
	
}
