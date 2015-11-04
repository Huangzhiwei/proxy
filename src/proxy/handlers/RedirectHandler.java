package proxy.handlers;

import java.util.EnumSet;
import java.util.Map;

/**
 * Created by Sunset on 2015/10/31.
 */
public class RedirectHandler implements Handler{
    @Override
    public boolean handle(Map<String, Object> map, EnumSet<ErrorType> sets) {
        map.put("Url","http://www.baidu.com");
        return true;
    }
}
