package dev.osuna.kanban.web.error;

import dev.osuna.kanban.service.InvalidPositionException;
import dev.osuna.kanban.service.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Converts exceptions to RFC 9457 ProblemDetail responses (SPEC 7.5, DEC-007).
 * The 500 body never leaks internals; the full stack trace goes to the log only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /** Move position outside the target column (SPEC 7.4 step 3, 7.5): 400 with the exception message as detail. */
    @ExceptionHandler(InvalidPositionException.class)
    public ProblemDetail handleInvalidPosition(InvalidPositionException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.getMessage());
    }

    /**
     * Unknown path (SPEC 7.1: any other route → standard Spring 404 in ProblemDetail format).
     * The detail is Spring's own message; no per-route logic.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /** Request body content type the endpoint cannot read (SPEC 7.5 table: 415). */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage());
    }

    /**
     * Last resort: only genuinely unexpected errors become 500. Framework exceptions that
     * already carry a 4xx client status (e.g. 406, 431) keep it instead of being reported
     * as server errors (SPEC 7.1/7.5). The 500 body never leaks internals; the full stack
     * trace goes to the log only (SPEC 11, E-17).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = errorResponse.getStatusCode();
            HttpStatus httpStatus = HttpStatus.resolve(status.value());
            String title = httpStatus != null ? httpStatus.getReasonPhrase() : "Client Error";
            String detail = ex.getMessage() != null ? ex.getMessage() : title;
            log.debug("Framework client error {} handled as ProblemDetail", status.value(), ex);
            return problem(status, title, detail);
        }
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error");
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        return problem;
    }
}
