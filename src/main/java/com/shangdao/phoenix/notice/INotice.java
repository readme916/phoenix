package com.shangdao.phoenix.notice;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.user.User;

public interface INotice {

	boolean canSend(User user);

	boolean sendMessage(ILogEntity entity, NoticeTemplate noticeTemplate, Act act ,User toUser , User fromUser);

	String parseMessage(ILogEntity entity, NoticeTemplate noticeTemplate, Act act ,User toUser , User fromUser);

}