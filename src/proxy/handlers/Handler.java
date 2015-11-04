package proxy.handlers;

import java.util.EnumSet;
import java.util.Map;

/**
 * Created by Sunset on 2015/10/31.
 */
public interface Handler {
    boolean handle(Map<String,Object> map,EnumSet<ErrorType> sets);
}
