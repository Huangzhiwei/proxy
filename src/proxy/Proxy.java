package proxy;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import proxy.handlers.ErrorType;
import proxy.handlers.HandlerLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;

/**
 * Created by Sunset on 2015/10/30.
 */
//@BeforeHandlers(classes = {"UrlFilterHandler","RedirectHandler"})
//@AfterHandlers
public class Proxy implements Runnable {
    private static int CACHESIZE = 16*1024;
    private boolean authoritied = true;
    private Socket socket;
    public Proxy(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){
        Scanner in = null;
        EnumSet<ErrorType> errors = EnumSet.noneOf(ErrorType.class);
        byte[] cache = new byte[CACHESIZE];
        StringBuilder sb = new StringBuilder();
        PrintWriter printWriter = null;
        OutputStream os = null;
        try{
            printWriter = new PrintWriter(socket.getOutputStream());
            os = socket.getOutputStream();
            in = new Scanner(socket.getInputStream());

            //decode the ip to http
            Map<String,Object> map = parseCode(in);
            //非空检测
            if(map == null){
                authoritied = false;
            }else{
                //Add handlers.
                errors.addAll(HandlerLoader.invokeBefore(this.getClass(),map));
            }
            //If pass handlers,then dispatch the request.
            if(authoritied&&errors.isEmpty()){
                CloseableHttpClient client = HttpClients.createDefault();
                HttpResponse response = null;
                HttpUriRequest req = getRequest(map);
                if(req instanceof HttpPost){
                    HttpPost post = (HttpPost)req;
                    response = client.execute(post);
                }else{
                    response = client.execute(req);
                }
                //设置返回数据头
                StringBuilder headers = new StringBuilder(response.getStatusLine().toString());
                headers.append("\r\n");
                Set<String> dirtySet = new HashSet<String>();
                dirtySet.add("Transfer-Encoding");
                for(Header h : response.getAllHeaders()){
                    if(!dirtySet.contains(h.getName())){
                        headers.append(h.getName());
                        headers.append(':');
                        headers.append(h.getValue());
                        headers.append("\r\n");
                    }
                }
                headers.append("\r\n");
                os.write(headers.toString().getBytes());
                os.flush();
                //deal with different status
                if(response.getStatusLine().getStatusCode() == 200){
                    HttpEntity entity = response.getEntity();
                    if(entity != null){
                        InputStream ins = entity.getContent();
                        while(true) {
                            int index = ins.read(cache);
                            if (index == -1) break;
                            os.write(cache, 0, index);
                        }
                    }
                }else{
//                    System.out.println(response.getStatusLine().getStatusCode());
                }
                client.close();
            }else{
                printWriter.println("403 forbid");
                printWriter.flush();
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Proxy raise a exception");
        }finally {
            try{
                if(socket != null)
                    socket.close();
                if(printWriter != null)
                    printWriter.close();
                if(in != null){
                    in.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private HttpUriRequest getRequest(Map<String,Object> map){
        Set unSet = new HashSet();
        unSet.addAll(Arrays.asList("Method", "Url", "Protocol", "Content-Length", "Params"));
        if("GET".equals(map.get("Method").toString())){
            HttpUriRequest request = new HttpGet(map.get("Url").toString());
            //添加请求头
            Set<String> keys = map.keySet();
            List<Header> list = new ArrayList<Header>();
            for(String s:keys){
                if(!unSet.contains(s) && map.get(s) != null){
                    list.add(new BasicHeader(s,map.get(s).toString()));
                }
            }
            Header[] headers = new Header[list.size()];
            for(int i = 0;i < list.size();++i){
                headers[i] = list.get(i);
            }
            request.setHeaders(headers);
            return request;
        }else{
            List <NameValuePair> params = new ArrayList<NameValuePair>();
            if(map.get("Params") != null){
                Map<String,String> kv = (Map)map.get("Params");
                for(String key:kv.keySet()){
                    params.add(new BasicNameValuePair(key,kv.get(key)));
                }
            }
            HttpPost request = new HttpPost(map.get("Url").toString());
            try{
                UrlEncodedFormEntity ent = new UrlEncodedFormEntity(params, "utf-8");
                request.setEntity(ent);
            }catch (Exception ue){
                ue.printStackTrace();
            }
            //添加请求头
            Set<String> keys = map.keySet();
            List<Header> list = new ArrayList<Header>();
            for(String s:keys){
                if(!unSet.contains(s) && map.get(s) != null){
                    list.add(new BasicHeader(s,map.get(s).toString()));
                }
            }
            Header[] headers = new Header[list.size()];
            for(int i = 0;i < list.size();++i){
                headers[i] = list.get(i);
            }
            request.setHeaders(headers);
            return request;
        }
    }

    private Map<String,Object> parseCode(Scanner in){
        boolean isData = false;
        Map<String,Object> map = null;
        StringBuilder sb = new StringBuilder();
        StringBuilder data = new StringBuilder();
        while(in.hasNextLine()){
            //read post data
            if(isData){
                data.append(in.nextLine());
            }else{
                //read header
                String tmp = in.nextLine();
                sb.append(tmp);
                sb.append("\r\n");
                if(tmp.length() == 0){
                    isData = true;
                }
            }
        }
        String[] raw = sb.toString().split("\r\n");
        if(raw.length >= 1){
            //Decode request line
            String[] tmps = raw[0].split(" ");
            if(tmps.length == 3){
                map = new HashMap<String, Object>();
                map.put("Method",tmps[0]);
                map.put("Url",tmps[1]);
                map.put("Protocol",tmps[2]);
                //Decode header
                for(int x = 1;x < raw.length;x++){
                    String tmp = raw[x];
                    String[] ts = tmp.split(":",2);
                    if(ts.length == 2){
                        map.put(ts[0].trim(),ts[1].trim());
                    }else{
                        System.out.println(tmp);
                    }
                }
                //Decode post data
                String datas = data.toString();
                if(datas.length() != 0){
                    String[] prams = datas.split("[&]");
                    if(prams.length > 0){
                        Map<String,String> params = new HashMap<String, String>();
                        for(String tmp:prams){
                            //Urldecode the post
                            try{
                                tmp = URLDecoder.decode(tmp,"UTF-8");
                            }catch (Exception e){
                                System.out.println("decode error");
                            }
                            System.out.println(tmp);
                            String[] dt = tmp.split("[=]");
                            if(dt.length > 0){
                                params.put(dt[0].trim(),dt[1].trim());
                            }
                        }
                        map.put("Params",params);
                    }
                }
            }
        }
        return map;
    }
}
