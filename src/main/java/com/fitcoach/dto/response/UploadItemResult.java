package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadItemResult<T> {
    private String fileName;
    private boolean success;
    private T data;
    private String error;
}
