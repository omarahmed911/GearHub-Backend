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

@RestControllerAdvice(basePackages = "gearhub.website.gearhub.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        String path = request.getURI().getPath();

        // ❌ تجاهل endpoints المهمة (Spring Security / Swagger / error)
        if (path.startsWith("/error")
                || path.equals("/")
                || path.startsWith("/api/health")
                || path.startsWith("/api/auth")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger")
                || path.startsWith("/actuator")) {
            return body;
        }

        // ❌ لو already wrapped
        if (body instanceof Map<?, ?> map && map.containsKey("data")) {
            return body;
        }

        // ❌ لو List
        if (body instanceof List<?>) {
            return Map.of(
                    "data", body,
                    "success", true
            );
        }

        // ❌ لو String (مهم جدًا لحل مشكلتك)
        if (body instanceof String) {
            return Map.of(
                    "data", body,
                    "success", true
            );
        }

        // ❌ null response
        if (body == null) {
            return Map.of(
                    "data", Collections.emptyList(),
                    "success", true
            );
        }

        // ✅ أي Object عادي
        return Map.of(
                "data", body,
                "success", true
        );
    }
}