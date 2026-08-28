package by.vs.bff.exception;

// выбрасывается, если ERP недоступна по таймауту или выдает 5xx ошибки
public class ErpUnavailableException extends RuntimeException {
    public ErpUnavailableException(String message) {
        super(message);
    }
}
