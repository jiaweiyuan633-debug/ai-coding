package com.aicoding.config;

import com.aicoding.core.auth.HeaderAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AI 服务 Web 配置：登录头拦截
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HeaderAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/error"
                );
    }
}
