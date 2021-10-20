package cn.zxsmart.exception;

/**
 * @author zhengkun
 * @date 2021/7/9
 */
public class TimeoutException extends RuntimeException {

    private String message;

    public TimeoutException(String message) {
        this.message = message;
    }
}
