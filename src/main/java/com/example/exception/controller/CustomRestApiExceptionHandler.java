package com.example.exception.controller;

import com.example.exception.ErrorResponse;
import com.example.exception.IllegalUserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = RestController.class)
@Slf4j
public class CustomRestApiExceptionHandler extends ResponseEntityExceptionHandler {

    //400
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse illegalExHandle(IllegalArgumentException e){
        log.error("[BAD_REQUEST] ex",e);
        return new ErrorResponse("BAD_REQUEST",e.getMessage());
    }

    //권한 없는 유저가 요청(400)
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> illegalUserExHandle(IllegalUserException e) {
        log.error("[exceptionHandle] ex", e);
        ErrorResponse errorResponse = new ErrorResponse("USER-EX", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    //500
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse exHandle(Exception e){
        log.error("[INTERNAL_SERVER_ERROR] ex",e);
        return new ErrorResponse("INTERNAL_SERVER_ERROR",e.getMessage());
    }
}
