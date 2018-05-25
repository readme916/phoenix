package com.shangdao.phoenix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import com.shangdao.phoenix.service.UserService;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.PublicWeixinAuthenticationFilter;
import com.shangdao.phoenix.util.WebAuthenticationFilter;
import com.shangdao.phoenix.util.WorkWeixinAuthenticationFilter;


@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds=186400)
public class Config extends WebSecurityConfigurerAdapter {
	
	@Value("${salt}")
	private String salt;
	
	@Value("${work.weixin.filter.pattern}")
	private String workPattern;
	
	@Value("${public.weixin.filter.pattern}")
	private String publicPattern;
		
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.headers().frameOptions().disable();
		http.addFilterAt(webAuthenticationFilter(),UsernamePasswordAuthenticationFilter.class);
		http.addFilterAfter(workAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
		http.addFilterAfter(publicAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
		http.csrf().disable()
		.formLogin()
		.loginPage("/login").permitAll()
		.and().authorizeRequests().antMatchers("/entity/user/create","/","/wxpublic/api","/wxwork/api/**").permitAll()
		.and().authorizeRequests().antMatchers("/browser/**","electron/**","wxpublic-desk/**","wxpublic-mobile/**","wxwork-desk/**","wxpublic-mobile/**").permitAll()
		.and().authorizeRequests().anyRequest().authenticated()   //拦截
		.and().rememberMe().tokenValiditySeconds(60 * 60 * 24 * 30);
	}
	
	@Bean
    public UserDetailsService customUserService() {
        return new UserService();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserService()).passwordEncoder(new PasswordEncoder(){

            @Override
            public String encode(CharSequence rawPassword) {
                return CommonUtils.MD5Encode((String)rawPassword,salt);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals(CommonUtils.MD5Encode((String)rawPassword, salt));
            }});
    }
    
    @Bean  
    public CacheManager cacheManager(@SuppressWarnings("rawtypes") RedisTemplate redisTemplate) {  
    	RedisCacheManager cacheManager =  new RedisCacheManager(redisTemplate); 
    	cacheManager.setDefaultExpiration(3600*24*10);
    	return cacheManager;
    }
    
	@Bean
	public WebAuthenticationFilter webAuthenticationFilter() throws Exception {
		WebAuthenticationFilter authenticationFilter = new WebAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(authenticationManagerBean());
		return authenticationFilter;
	}
    
	@Override
	@Bean // share AuthenticationManager for web and oauth
	public AuthenticationManager authenticationManagerBean() throws Exception {
	    return super.authenticationManagerBean();
	}
	
	@Bean
	public WorkWeixinAuthenticationFilter workAuthenticationFilter() {
		WorkWeixinAuthenticationFilter authenticationFilter = new WorkWeixinAuthenticationFilter(workPattern);
		authenticationFilter.setAuthenticationManager( new AuthenticationManager() {
			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				return authentication;
			}
		});
		return authenticationFilter;
	}
	
	@Bean
	public PublicWeixinAuthenticationFilter publicAuthenticationFilter() {
		PublicWeixinAuthenticationFilter authenticationFilter = new PublicWeixinAuthenticationFilter(publicPattern);
		authenticationFilter.setAuthenticationManager( new AuthenticationManager() {
			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				return authentication;
			}
		});
		return authenticationFilter;
	}
}
