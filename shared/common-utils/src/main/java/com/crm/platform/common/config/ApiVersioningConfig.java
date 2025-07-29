package com.crm.platform.common.config;

import com.crm.platform.common.annotation.ApiVersion;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for API versioning support
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {
    
    private static final String VERSION_HEADER = "X-API-Version";
    private static final String VERSION_PARAM = "version";
    private static final String DEFAULT_VERSION = "1";
    private static final Pattern VERSION_PATTERN = Pattern.compile("v(\\d+(?:\\.\\d+)?)");
    
    /**
     * Custom RequestMappingHandlerMapping to handle API versioning
     */
    public static class VersionedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
        
        @Override
        protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
            RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
            
            if (info == null) {
                return null;
            }
            
            // Check for ApiVersion annotation on method first, then class
            ApiVersion methodVersion = method.getAnnotation(ApiVersion.class);
            ApiVersion classVersion = handlerType.getAnnotation(ApiVersion.class);
            
            ApiVersion version = methodVersion != null ? methodVersion : classVersion;
            
            if (version != null) {
                // Create versioned mapping
                RequestMappingInfo versionedInfo = RequestMappingInfo
                        .paths("/api/v" + version.value())
                        .methods()
                        .params()
                        .headers()
                        .consumes()
                        .produces()
                        .mappingName("")
                        .customCondition(new ApiVersionCondition(version.value()))
                        .build();
                
                return versionedInfo.combine(info);
            }
            
            return info;
        }
    }
    
    /**
     * Custom condition for API version matching
     */
    public static class ApiVersionCondition implements org.springframework.web.servlet.mvc.condition.RequestCondition<ApiVersionCondition> {
        
        private final String version;
        
        public ApiVersionCondition(String version) {
            this.version = version;
        }
        
        @Override
        public ApiVersionCondition combine(ApiVersionCondition other) {
            return new ApiVersionCondition(other.version);
        }
        
        @Override
        public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
            String requestVersion = extractVersion(request);
            
            if (requestVersion != null && requestVersion.equals(this.version)) {
                return this;
            }
            
            // Default version matching
            if (requestVersion == null && DEFAULT_VERSION.equals(this.version)) {
                return this;
            }
            
            return null;
        }
        
        @Override
        public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
            // Higher versions have priority
            return other.version.compareTo(this.version);
        }
        
        private String extractVersion(HttpServletRequest request) {
            // Try header first
            String headerVersion = request.getHeader(VERSION_HEADER);
            if (headerVersion != null) {
                return headerVersion;
            }
            
            // Try query parameter
            String paramVersion = request.getParameter(VERSION_PARAM);
            if (paramVersion != null) {
                return paramVersion;
            }
            
            // Try URL path
            String path = request.getRequestURI();
            Matcher matcher = VERSION_PATTERN.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Try Accept header
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("version=")) {
                Pattern acceptPattern = Pattern.compile("version=(\\d+(?:\\.\\d+)?)");
                Matcher acceptMatcher = acceptPattern.matcher(acceptHeader);
                if (acceptMatcher.find()) {
                    return acceptMatcher.group(1);
                }
            }
            
            return null;
        }
        
        public String getVersion() {
            return version;
        }
    }
}