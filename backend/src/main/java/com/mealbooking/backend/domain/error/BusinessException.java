package com.mealbooking.backend.domain.error;

public class BusinessException extends RuntimeException {
    public enum Rejection { INVALID_INPUT, NOT_FOUND, FORBIDDEN, CONFLICT }

    private final Rejection rejection;

    private BusinessException(Rejection rejection, String message) {
        super(message);
        this.rejection = rejection;
    }

    public Rejection rejection() { return rejection; }

    public BusinessException withMessage(String message) {
        return new BusinessException(rejection, message);
    }

    public static BusinessException badRequest(String message) { return new BusinessException(Rejection.INVALID_INPUT, message); }
    public static BusinessException notFound(String message) { return new BusinessException(Rejection.NOT_FOUND, message); }
    public static BusinessException forbidden(String message) { return new BusinessException(Rejection.FORBIDDEN, message); }
    public static BusinessException conflict(String message) { return new BusinessException(Rejection.CONFLICT, message); }
}
