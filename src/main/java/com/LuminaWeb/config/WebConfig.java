package com.LuminaWeb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/");
        registry
            .addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/");
        registry
            .addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/img/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");              // templates/index.html
        registry.addViewController("/login").setViewName("Inicio_Sesion"); // <-- usa Inicio_Sesion.html
    }
}
