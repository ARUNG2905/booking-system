package com.undoschool.bookingsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BookingExceptions {

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class BookingConflictException extends RuntimeException {
        public BookingConflictException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String resource, Object id) {
            super(resource + " not found with id: " + id);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateBookingException extends RuntimeException {
        public DuplicateBookingException() {
            super("You have already booked this offering");
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidTimezoneException extends RuntimeException {
        public InvalidTimezoneException(String timezone) {
            super("Invalid timezone: " + timezone + ". Use IANA format e.g. Asia/Kolkata");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidSessionTimeException extends RuntimeException {
        public InvalidSessionTimeException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email);
        }
    }
}
