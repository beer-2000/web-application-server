package webserver;

// TODO 상수 만들고 openHTML의 인자로 사용하자
public enum ResponseStatus {
    OK(200, "OK"),
    REDIRECT(302, "Found");

    private final int code;
    private final String message;

    ResponseStatus(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
