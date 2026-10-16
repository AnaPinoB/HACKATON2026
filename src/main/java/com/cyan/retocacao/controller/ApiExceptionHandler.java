package com.cyan.retocacao.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ProblemDetail camposInvalidos(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        String campos = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField()).distinct().sorted().collect(java.util.stream.Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise los campos obligatorios, longitudes y valores: " + campos);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ProblemDetail validacionServicio() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de operacion invalidos");
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ProblemDetail bloqueo() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "El lote esta siendo actualizado. Reintente con el mismo codigo de operacion");
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail validacion(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail formatoInvalido() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Envie un JSON valido, una fecha AAAA-MM-DD y una cantidad numerica");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflicto() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "No se pudo guardar: codigo existente o datos incompatibles con la base de datos");
    }
}
