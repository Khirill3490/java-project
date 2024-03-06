package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionsHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorIndexingResponse> notFoundException(DataNotFoundException e) {
        ErrorIndexingResponse response = new ErrorIndexingResponse();
        response.setResult(false);
        response.setError(e.getMessage());

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorIndexingResponse> notFoundException(BadRequestException e) {
        ErrorIndexingResponse response = new ErrorIndexingResponse();
        response.setResult(false);
        response.setError(e.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    private ResponseEntity<ErrorIndexingResponse> notFoundException(Exception e) {
        ErrorIndexingResponse response = new ErrorIndexingResponse();
        response.setResult(false);
        response.setError(e.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
