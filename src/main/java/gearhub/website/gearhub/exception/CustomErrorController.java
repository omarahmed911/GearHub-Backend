package gearhub.website.gearhub.exception;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Map<String, Object> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        Integer statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        String errorMessage = "Internal Server Error";
        if (message != null && !message.toString().isBlank()) {
            errorMessage = message.toString();
        } else if (exception != null) {
            errorMessage = ((Exception) exception).getMessage();
        }
        
        return Map.of("data", List.of(Map.of("status", statusCode, "error", errorMessage)));
    }
}
