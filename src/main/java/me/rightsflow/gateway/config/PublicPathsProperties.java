package me.rightsflow.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "rightsflow.gateway.security")
public class PublicPathsProperties {

    private List<String> publicPaths;

}