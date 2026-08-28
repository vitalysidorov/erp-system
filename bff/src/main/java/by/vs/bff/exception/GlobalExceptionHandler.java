package by.vs.bff.exception;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ApiErrorResponse> handleSlotAlreadyBooked(SlotAlreadyBookedException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(), // 409
                "SLOT_ALREADY_OCCUPIED",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ErpUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleErpUnavailable(ErpUnavailableException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(), // 503
                "ERP_SYSTEM_ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(WebExchangeBindException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(), // 400
                "VALIDATION_FAILED",
                "Ошибка валидации входных данных",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Непредвиденная ошибка на стороне BFF: ", ex);
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                "UNKNOWN_BFF_ERROR",
                "Произошла внутренняя ошибка сервера. Мы уже работаем над исправлением.",
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RequestNotPermitted ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(), // Статус 429
                "TOO_MANY_REQUESTS",
                "Вы отправляете запросы слишком часто. Пожалуйста, подождите несколько секунд.",
                null
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Object> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(HttpStatus.FORBIDDEN.getReasonPhrase());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Неверный формат параметра: " + ex.getName());
    }
}
