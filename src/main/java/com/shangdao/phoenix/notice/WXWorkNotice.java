package com.shangdao.phoenix.notice;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.User.Source;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.service.WorkWeixinLoginService;

//站内信通知
@Service
public class WXWorkNotice implements INotice {

	@Autowired
	private WorkWeixinLoginService workWeixinLoginService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.shangdao.phoenix.notice.INotice#canSend(com.shangdao.phoenix.entity.
	 * user.User)
	 */
	public boolean canSend(User user) {
		if (user.getSource().equals(Source.WXWORK) && user.getDeletedAt() == null) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.shangdao.phoenix.notice.INotice#sendMessage(com.shangdao.phoenix.
	 * entity.interfaces.ILogEntity,
	 * com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate,
	 * com.shangdao.phoenix.entity.user.User)
	 */
	public boolean sendMessage(ILogEntity entity, NoticeTemplate noticeTemplate, Act act, User toUser, User fromUser) {
		try {
			SpelNoticeContext spelContext = new SpelNoticeContext(entity, act, toUser, fromUser);
			ExpressionParser parser = new SpelExpressionParser();
			String content = parser.parseExpression(noticeTemplate.getWxworkTemplate(), new TemplateParserContext())
					.getValue(spelContext, String.class);
			Map sends = workWeixinLoginService.sendTextToUserByLoginAgent(toUser.getUsername(), content,
					noticeTemplate.getWxworkAgentId(), noticeTemplate.getWxworkAgentSecret());

			if (sends.get("errcode").toString().equals("0")) {
				if (sends.get("invaliduser") == null || sends.get("invaliduser").toString().equals("")) {
					return true;
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.shangdao.phoenix.notice.INotice#parseMessage(com.shangdao.phoenix.
	 * entity.interfaces.ILogEntity,
	 * com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate,
	 * com.shangdao.phoenix.entity.user.User)
	 */
	public String parseMessage(ILogEntity entity, NoticeTemplate noticeTemplate, Act act, User toUser, User fromUser) {
		SpelNoticeContext spelContext = new SpelNoticeContext(entity, act, toUser, fromUser);
		ExpressionParser parser = new SpelExpressionParser();
		return parser.parseExpression(noticeTemplate.getWxworkTemplate(), new TemplateParserContext())
				.getValue(spelContext, String.class);
	}

}
