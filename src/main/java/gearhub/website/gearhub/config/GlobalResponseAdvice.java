package gearhub.website.gearhub.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "gearhub.website.gearhub")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
                                      
        String path = request.getURI().getPath();
        
        // Exclude the health endpoint (/) and error endpoint so we don't wrap them twice
        if (path.equals("/") || path.startsWith("/error") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return body;
        }

        // Avoid wrapping if it's already wrapped implicitly or explicitly
        if (body instanceof Map && ((Map<?, ?>) body).containsKey("data")) {
            return body;
        }

        if (body instanceof List) {
            return Map.of("data", body);
        } else if (body != null) {
            return Map.of("data", List.of(body));
        } else {
            return Map.of("data", Collections.emptyList());
        }
    }
}

