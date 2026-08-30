package by.vs.erp.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/employees/999");
    }

    @Test
    @DisplayName("NotFoundException маппится в 404 Not Found")
    void handleNotFound_ReturnsNotFound() {
        NotFoundException exception = new NotFoundException("Сотрудник не найден");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Сотрудник не найден", response.getBody().getMessage());
        assertEquals("/api/v1/employees/999", response.getBody().getPath());
    }

    @Test
    @DisplayName("ConflictException маппится в 409 Conflict (например, дубликат email/телефона)")
    void handleConflict_ReturnsConflict() {
        ConflictException exception = new ConflictException("Сотрудник с таким email уже зарегистрирован");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Сотрудник с таким email уже зарегистрирован", response.getBody().getMessage());
    }

    @Test
    @DisplayName("IllegalArgumentException маппится в реальную 400 Bad Request, а не в 404")
    void handleBadRequest_ReturnsBadRequest_NotNotFound() {
        IllegalArgumentException exception = new IllegalArgumentException("Некорректный параметр запроса");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    @DisplayName("IllegalStateException (бизнес-конфликт, напр. 'заказ уже закрыт') по-прежнему маппится в 409")
    void handleBusinessException_ReturnsConflict() {
        IllegalStateException exception = new IllegalStateException("Этот заказ-наряд уже закрыт.");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessExceptions(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Общий Exception маппится в 500 Internal Server Error с обезличенным сообщением")
    void handleGenericException_ReturnsInternalServerError() {
        Exception exception = new RuntimeException("что-то пошло не так на уровне инфраструктуры");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalExceptions(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Произошла внутренняя ошибка сервера. Обратитесь к администратору.",
                response.getBody().getMessage());
    }
}