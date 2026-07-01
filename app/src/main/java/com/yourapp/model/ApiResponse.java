package com.yourapp.model;

/**
 * Generic API response wrapper.
 * All backend endpoints return this format:
 * { "success": true/false, "message": "...", "data": {...}, "error": null/string }
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
