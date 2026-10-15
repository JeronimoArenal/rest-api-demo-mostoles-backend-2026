package com.example.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessException(
            DataAccessException e) {

        Map<String, String> error = Map.of("error", "Error al consultar los productos");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException e) {

        String mensaje = Objects.requireNonNullElse(e.getReason(), "Se ha producido un error");
        Map<String, String> error = Map.of("mensaje", mensaje);

        return ResponseEntity
                .status(e.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {

        Map<String, String> error = Map.of("mensaje", "El parametro de petició es obligatorio");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

}