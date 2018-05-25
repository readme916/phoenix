package com.shangdao.phoenix.notice;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.interfaces.ILogEntity;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.user.User;

//站内信通知
@Service
public class SMSNotice implements INotice {
	
	   static final String product = "Dysmsapi";
	    //产品域名,开发者无需替换
	    static final String domain = "dysmsapi.aliyuncs.com";
	    
	    
	    @Value("${spring.oss.accessKeyId}")
		private String accessKeyId;

		@Value("${spring.oss.accessKeySecret}")
		private String accessKeySecret;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.shangdao.phoenix.notice.INotice#canSend(com.shangdao.phoenix.entity.
	 * user.User)
	 */
	public boolean canSend(User user) {
		if (user.getMobile()!=null && !user.getMobile().equals("") && user.getDeletedAt() == null) {
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
		
		 System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
	     System.setProperty("sun.net.client.defaultReadTimeout", "10000");
		
		try {
			SpelNoticeContext spelContext = new SpelNoticeContext(entity, act, toUser, fromUser);
			ExpressionParser parser = new SpelExpressionParser();
			
			//初始化acsClient,暂不支持region化
	        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
	        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
	        IAcsClient acsClient = new DefaultAcsClient(profile);

	        //组装请求对象-具体描述见控制台-文档部分内容
	        SendSmsRequest request = new SendSmsRequest();
	        //必填:待发送手机号
	        request.setPhoneNumbers(toUser.getMobile());
	        //必填:短信签名-可在短信控制台中找到
	        request.setSignName(noticeTemplate.getAliSmsSignName());
	        //必填:短信模板-可在短信控制台中找到
	        request.setTemplateCode(noticeTemplate.getAliSmsTemplateCode());
	        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
	        
			String content = parser.parseExpression(noticeTemplate.getAliSmsTemplateParam(), new TemplateParserContext())
					.getValue(spelContext, String.class);
	        
	        request.setTemplateParam(content);

//	        hint 此处可能会抛出异常，注意catch
	        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
	        if(sendSmsResponse.getCode() != null && sendSmsResponse.getCode().equals("OK")) {
	        	return true;
	        }
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (ClientException e) {
			// TODO Auto-generated catch block
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
		String content = parser.parseExpression(noticeTemplate.getAliSmsTemplateParam(), new TemplateParserContext())
				.getValue(spelContext, String.class);
		ObjectMapper mapper = new ObjectMapper();
		try {
			 Map<String,Object> m = mapper.readValue(content, Map.class);
			 Set<Entry<String,Object>> entrySet = m.entrySet();
			 String aliSmsTemplateContent = noticeTemplate.getAliSmsTemplateContent();
			 for (Entry<String, Object> entry : entrySet) {
				String key = entry.getKey().toString();
				String value = entry.getValue().toString();
				
				String replaceKey = "\\$\\{"+key+"\\}";
				aliSmsTemplateContent = aliSmsTemplateContent.replaceAll(replaceKey, value);
			}
			return aliSmsTemplateContent;
			
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
