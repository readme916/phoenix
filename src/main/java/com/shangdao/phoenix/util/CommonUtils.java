package com.shangdao.phoenix.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate;
import com.shangdao.phoenix.entity.noticeTemplate.NoticeTemplate.NoticeChannel;
import com.shangdao.phoenix.notice.INotice;
import com.shangdao.phoenix.notice.MessageNotice;
import com.shangdao.phoenix.notice.SMSNotice;
import com.shangdao.phoenix.notice.WXPublicNotice;
import com.shangdao.phoenix.notice.WXWorkNotice;
import com.shangdao.phoenix.service.InitService;;

@Component
public class CommonUtils implements ApplicationContextAware{

	private static ApplicationContext applicationContext = null;
	
	@Override  
    public void setApplicationContext(ApplicationContext applicationContext){  
        if(this.applicationContext == null){  
            this.applicationContext  = applicationContext;  
        }  
    }  
  
    //获取applicationContext  
    public static ApplicationContext getApplicationContext() {  
        return applicationContext;  
    }
	
	
	public static void print(Object o){
		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out.println(mapper.writeValueAsString(o));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static UserDetailsImpl currentUser(){
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return InitService.getAnonymousUser();
		}
		if (authentication.getPrincipal() instanceof UserDetailsImpl) {
			return (UserDetailsImpl) authentication.getPrincipal();
		} else {
			return InitService.getAnonymousUser();
		}
		
	}
	public static String[] getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
	
	public static Set<String> getNotNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> notEmptyNames = new HashSet<String>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue != null) notEmptyNames.add(pd.getName());
        }
        return notEmptyNames;
    }

    public static void copyPropertiesIgnoreNull(Object src, Object target){
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }
    
    
    public static void validate(Object object) {

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<Object>> validates = validator.validate(object);
		
		ArrayList<ValidateError> errors = new ArrayList<ValidateError>();
		if (!validates.isEmpty()) {
			for (ConstraintViolation<Object> constraint : validates) {
				ValidateError validateError = new ValidateError();
				validateError.setMessage(constraint.getMessage());
				validateError.setProperty(constraint.getPropertyPath().toString());
				errors.add(validateError);
			}
			throw new OutsideRuntimeException(1000,errors);
			
		}
	}
    public static String MD5Encode(String password,String salt) {
        password = password + salt;
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        char[] charArray = password.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }

            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
    
    public static String filterEmoji(String source) {
        if (source == null) {
            return source;
        }
        Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
                Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        Matcher emojiMatcher = emoji.matcher(source);
        if (emojiMatcher.find()) {
            source = emojiMatcher.replaceAll("");
            return source;
        }
        return source;
    }

    
    public static INotice getNotice(NoticeTemplate noticeTemplate){
    	ApplicationContext ac = getApplicationContext(); 
    	if(noticeTemplate.getNoticeChannel().equals(NoticeChannel.MESSAGE)){
    		return ac.getBean(MessageNotice.class);
    	}if(noticeTemplate.getNoticeChannel().equals(NoticeChannel.WXWORK)){
    		return ac.getBean(WXWorkNotice.class);
    	}if(noticeTemplate.getNoticeChannel().equals(NoticeChannel.WXPUBLIC)){
    		return ac.getBean(WXPublicNotice.class);
    	}if(noticeTemplate.getNoticeChannel().equals(NoticeChannel.SMS)){
    		return ac.getBean(SMSNotice.class);
    	}
    	else{
    		throw new OutsideRuntimeException(7333, "不支持"+noticeTemplate.getNoticeChannel()+"这种通知类型");
    	}
    }
}
