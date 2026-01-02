package com.LuminaWeb.config;

import java.nio.file.FileSystems;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.notas.dir:uploads/notas}")
    private String uploadNotasDir;

    @Value("${app.upload.pruebas.dir:uploads/pruebas}")
    private String uploadPruebasDir;

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

        String location = "file:" + Paths.get(uploadPruebasDir).toAbsolutePath().toString() + FileSystems.getDefault().getSeparator();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
        
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");              // templates/index.html
        registry.addViewController("/login").setViewName("Inicio_Sesion"); // <-- usa Inicio_Sesion.html
    }
}
