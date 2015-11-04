package proxy.handlers;

import java.util.*;

/**
 * Created by Sunset on 2015/10/31.
 */
public class UrlFilterHandler implements Handler {
    private static final Set<String> urls;
    private static UrlFilterHandler INSTANCE = new UrlFilterHandler();
    //load resourse
    static {
        urls = loadUrl();
    }
    public static UrlFilterHandler getInstance(){
        return INSTANCE;
    }
    @Override
    public boolean handle(Map<String, Object> map,EnumSet<ErrorType> set) {
        Object rawurl = map.get("Url");
        if(rawurl != null){
            String url = rawurl.toString();
            for(String t:urls){
                if(url.contains(t)){
                    set.add(ErrorType.ILLEGELURL);
                    return false;
                }
            }
        }
        return true;
    }
    private static Set<String> loadUrl(){
        Set<String> urls = new HashSet<String>();
        Scanner in = null;
        try{
            in = new Scanner(UrlFilterHandler.class.getClassLoader().getResourceAsStream("proxy/handlers/cfg/url_forbid.txt"));
            while(in.hasNextLine()){
                String tmp = in.nextLine();
                tmp = tmp.trim();
                if(!tmp.startsWith("#")){
                    urls.add(tmp);
                }
            }
            System.out.println("load file success.");
        }catch (Exception e){
            System.out.println("load file fail.");
        }finally {
            if(in != null){
                in.close();
            }
        }
        return urls;
    }
}
