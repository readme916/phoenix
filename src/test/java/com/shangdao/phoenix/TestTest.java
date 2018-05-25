package com.shangdao.phoenix;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestTest {

	@Test
    public void exampleTest2() throws JsonParseException, JsonMappingException, IOException {
		
		String content = "{\"name\":\"Tom\", \"code\":\"123\"}";
		ObjectMapper mapper = new ObjectMapper();
		 Map<String,Object> m = mapper.readValue(content, Map.class);
		 Set<Entry<String,Object>> entrySet = m.entrySet();
		 String aliSmsTemplateContent = "名字:${name},代码：${code}";
		 for (Entry<String, Object> entry : entrySet) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			
			String replaceKey = "\\$\\{"+key+"\\}";
			aliSmsTemplateContent = aliSmsTemplateContent.replaceAll(replaceKey, value);
		}
		 System.out.println(aliSmsTemplateContent);
    }
}


