package com.shangdao.phoenix.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.example.Example;
import com.shangdao.phoenix.entity.message.Message;
import com.shangdao.phoenix.entity.message.MessageRepository;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.UserRepository;
import com.shangdao.phoenix.service.GetMethodService.GetMethodWrapper;
import com.shangdao.phoenix.service.PostMethodService.PostMethodWrapper;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.HTTPResponse;

@Service
public class MessageService implements InterfaceEntityService {

	// 保证bean创建顺序，没实际用处
	@Autowired
	private InitService initService;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private UserRepository userRepository;

	@Transactional
	public HTTPResponse detail(GetMethodWrapper getMethodWrapper, HTTPResponse response) {

		Long id = getMethodWrapper.getId();
		Message message = messageRepository.findOne(id);
		if (!message.isBeRead()) {
			message.setBeRead(true);
			User toUser = message.getToUser();
			if (toUser.getId() == CommonUtils.currentUser().getId()) {
				int notReadMessage = toUser.getNotReadMessage();
				notReadMessage = Math.max(0, notReadMessage - 1);
				toUser.setNotReadMessage(notReadMessage);
				userRepository.save(toUser);
			}
			messageRepository.save(message);
		}
		return response;
	}

	// 最好不要修改oldInstance
	public void create(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance) {
		Message post = (Message) postBody;
		post.setFromUser(CommonUtils.currentUser().getUser());
		User toUser = post.getToUser();
		User findOne = userRepository.findOne(toUser.getId());
		findOne.setNotReadMessage(findOne.getNotReadMessage() + 1);
		userRepository.save(findOne);
	}

	@Override
	@PostConstruct
	public void registerService() {
		initService.getStructure(Message.class).setEntityService(this);

	}
}
