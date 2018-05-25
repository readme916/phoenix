package com.shangdao.phoenix.notice;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.user.User;
import com.shangdao.phoenix.entity.user.User.Source;
import com.shangdao.phoenix.service.InitService;
import com.shangdao.phoenix.service.PublicWeixinService;
import com.shangdao.phoenix.service.WorkWeixinLoginService;

//站内信通知
@Service
public class WXPublicNotice implements INotice {

	@Autowired
	private PublicWeixinService publicWeixinService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.shangdao.phoenix.notice.INotice#canSend(com.shangdao.phoenix.entity.
	 * user.User)
	 */
	public boolean canSend(User user) {
		if (user.getSource().equals(Source.WXPUBLIC) && user.getDeletedAt() == null) {
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
			String content = parser.parseExpression(noticeTemplate.getWxpublicTemplateData(), new TemplateParserContext())
					.getValue(spelContext, String.class);
			Map sends = publicWeixinService.sendMessageToUser(toUser.getUsername(), 
					noticeTemplate.getWxpublicTemplateId(), 
					noticeTemplate.getWxpublicTemplateUrl(), 
					content);

			if (sends.get("errcode").toString().equals("0")) {
				return true;
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
		String dataContent = parser.parseExpression(noticeTemplate.getWxpublicTemplateData(), new TemplateParserContext())
				.getValue(spelContext, String.class);
		ObjectMapper mapper = new ObjectMapper();
		try {
			 Map<String,Object> m = mapper.readValue(dataContent, Map.class);
			 Set<Entry<String,Object>> entrySet = m.entrySet();
			 String templateContent = noticeTemplate.getWxpublicTemplateContent();
			 for (Entry<String, Object> entry : entrySet) {
				String key = entry.getKey().toString();
				String value = entry.getValue().toString();
				
				String replaceKey = "\\{\\{"+key+"\\.DATA\\}\\}";
				templateContent = templateContent.replaceAll(replaceKey, value);
			}
			return templateContent;
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
