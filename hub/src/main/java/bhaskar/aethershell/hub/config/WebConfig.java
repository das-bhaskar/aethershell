package bhaskar.aethershell.hub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps 'localhost:8080/output/...' to the physical folder
        registry.addResourceHandler("/output/**")
                .addResourceLocations("file:src/main/resources/static/output/");
    }
}