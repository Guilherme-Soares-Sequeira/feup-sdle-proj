package org.C2.utils;

public class HttpResult<T> {
    private T correctResult;
    private String errorMessage;
    private Integer code;
    private boolean ok;

    private HttpResult() {
        this.correctResult = null;
        this.errorMessage = null;
        this.code = null;
        this.ok = false;
    }

    private HttpResult(Integer code, String errorMessage) {
        this.correctResult = null;
        this.errorMessage = errorMessage;
        this.code = code;
        this.ok = false;
    }

    private HttpResult(Integer code, T correctResult) {
        this.correctResult = correctResult;
        this.errorMessage = null;
        this.code = code;
        this.ok = true;
    }

    public static <T> HttpResult<T> err(Integer code, String errorMessage) {
        return new HttpResult<>(code, errorMessage);
    }

    public static <T> HttpResult<T> ok(Integer code, T correctResult) {
        return new HttpResult<>(code, correctResult);
    }

    public boolean isOk() {
        return ok;
    }

    private void setOk(boolean ok) {
        this.ok = ok;
    }

    public Integer code() {
        return this.code;
    }

    private void code(Integer code) {
        this.code = code;
    }

    private void correctResult(T correctResult) {
        this.correctResult = correctResult;
    }

    public String errorMessage() {
        if (this.ok) throw new RuntimeException("Tried to access errorMessage of an Ok HttpResult.");

        return this.errorMessage;
    }

    private void errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public T get() {
        return this.correctResult;
    }
}
