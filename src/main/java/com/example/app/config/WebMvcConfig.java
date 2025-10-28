package com.example.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Configuration class for Spring MVC settings
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    // Override method to configure static resource handling
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure all static resources to be served from the /static/ directory
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    // Override method to configure view controllers for URL mapping
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map root URL to index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        // Map /register URL to register.html
        registry.addViewController("/register").setViewName("forward:/register.html");
        // Map /dashboard URL to dashboard.html
        registry.addViewController("/dashboard").setViewName("forward:/dashboard.html");
        // Map /topic URL to topic.html
        registry.addViewController("/topic").setViewName("forward:/topic.html");
    }
} 