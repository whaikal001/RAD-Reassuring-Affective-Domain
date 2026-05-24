package com.radai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.radai.interceptor.PreLLMScreeningInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private PreLLMScreeningInterceptor preLLMScreeningInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources from /static, /public, /resources, /META-INF/resources
        // but exclude /api/** paths (let them be handled by controllers)
        registry
            .addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/");
        
        registry
            .addResourceHandler("/public/**")
            .addResourceLocations("classpath:/public/");
        
        registry
            .addResourceHandler("/resources/**")
            .addResourceLocations("classpath:/resources/");
        
        // Only match root index.html if no other route matches
        registry
            .addResourceHandler("/index.html")
            .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Intercept chat flow endpoints to enforce pre-LLM screening for anonymous users
        registry.addInterceptor(preLLMScreeningInterceptor)
                .addPathPatterns("/api/chat/flow/**");
    }
}

