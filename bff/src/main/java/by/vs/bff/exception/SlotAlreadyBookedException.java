package by.vs.bff.exception;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String message) {
        super(message);
    }
}
