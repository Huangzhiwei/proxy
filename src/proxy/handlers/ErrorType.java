package proxy.handlers;

import java.util.EnumSet;

/**
 * Created by Sunset on 2015/10/31.
 */
public enum ErrorType {
    ILLEGELURL("·Ç·¨µÄÍøÖ·");
    private String message;
    ErrorType(String message){
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
