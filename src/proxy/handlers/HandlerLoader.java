package proxy.handlers;

import proxy.Proxy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Sunset on 2015/10/31.
 */
public class HandlerLoader {
    private static ConcurrentHashMap<String,List<Handler>> beforeMap = new ConcurrentHashMap<String, List<Handler>>();
    private static ConcurrentHashMap<String,List<Handler>> afterMap = new ConcurrentHashMap<String, List<Handler>>();
    static {
        addHandlerToClass(Proxy.class);
    }
    public static EnumSet<ErrorType> invokeBefore(Class<? extends Runnable> cls,Map<String,Object> map){
        EnumSet<ErrorType> errors = EnumSet.noneOf(ErrorType.class);
        List<Handler> handlers = beforeMap.get(cls.getName());
        if(handlers == null){
            if(!addHandlerToClass(cls)){
                beforeMap.put(cls.getName(),new ArrayList<Handler>());
            }
            handlers = beforeMap.get(cls.getName());
        }
        for(Handler h:handlers){
            if(!h.handle(map,errors)){
                return errors;
            }
        }
        return errors;
    }
    public static EnumSet<ErrorType> invokeAfter(Class<? extends Runnable> cls,Map<String,Object> map){
        EnumSet<ErrorType> errors = EnumSet.noneOf(ErrorType.class);
        List<Handler> handlers = afterMap.get(cls.getName());
        if(handlers == null){
            if(!addHandlerToClass(cls)){
                afterMap.put(cls.getName(),new ArrayList<Handler>());
            }
            handlers = afterMap.get(cls.getName());
        }
        for(Handler h:handlers){
            if(!h.handle(map,errors)){
                return errors;
            }
        }
        return errors;
    }
    private static boolean addHandlerToClass(Class<? extends Runnable> cls){
        boolean result = false;
        //add before handlers
        if(cls.isAnnotationPresent(BeforeHandlers.class)){
            BeforeHandlers beforeHandlers = cls.getAnnotation(BeforeHandlers.class);
            String[] classes = beforeHandlers.classes();
            List<Class> list = new ArrayList<Class>();
            for(String c:classes){
                try{
                    list.add(Class.forName("proxy.handlers."+c));
                    System.out.println("proxy.handlers."+c);
                }catch (ClassNotFoundException cns){
                    System.out.println("Class not found");
                }
            }
            List<Handler> handlers = new ArrayList<Handler>();
            for(Class c:list){
                try{
                    Handler handler = (Handler)c.newInstance();
                    handlers.add(handler);
                }catch (Exception e){
                    System.out.println("Convented error");
                }
            }
            beforeMap.put(cls.getName(),handlers);
            result = true;
        }
        //add after handlers
        if(cls.isAnnotationPresent(AfterHandlers.class)){
            AfterHandlers afterHandlers = cls.getAnnotation(AfterHandlers.class);
            String[] classes = afterHandlers.classes();
            List<Class> list = new ArrayList<Class>();
            for(String c:classes){
                try{
                    list.add(Class.forName("proxy.handlers."+c));
                }catch (ClassNotFoundException cns){
                    System.out.println("Class not found");
                }
            }
            List<Handler> handlers = new ArrayList<Handler>();
            for(Class c:list){
                try{
                    Handler handler = (Handler)c.newInstance();
                    handlers.add(handler);
                }catch (Exception e){
                    System.out.println("Convented error");
                }
            }
            afterMap.put(cls.getName(),handlers);
            result = true;
        }
        return result;
    }
}
