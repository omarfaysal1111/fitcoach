package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class CustomErrorController implements ErrorController {

    @RequestMapping
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        Throwable ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (ex instanceof MaxUploadSizeExceededException
                || (ex != null && ex.getCause() instanceof MaxUploadSizeExceededException)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error("File is too large. Maximum allowed size is 50 MB."));
        }

        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        HttpStatus status = statusCode != null
                ? HttpStatus.resolve(statusCode) : HttpStatus.INTERNAL_SERVER_ERROR;
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        String exClass = ex != null ? ex.getClass().getSimpleName() : "unknown";
        String exMsg   = ex != null ? ex.getMessage() : null;
        String body = "[DEBUG-FILTER] " + exClass + ": " + exMsg
                + " | httpMsg: " + (message != null ? message : "none");
        return ResponseEntity.status(status).body(ApiResponse.error(body));
    }
}
