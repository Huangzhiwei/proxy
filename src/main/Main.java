package main;

import proxy.Proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Sunset on 2015/10/30.
 */
public class Main {
    private static int SO_TIMEOUT = 500;
    private static Executor exe = Executors.newCachedThreadPool();
    public static void main(String[] args){
        try{
            ServerSocket ss = new ServerSocket(8080);
            while(true){
                try{
                    Socket socket = ss.accept();
                    socket.setSoTimeout(SO_TIMEOUT);
                    exe.execute(new Proxy(socket));
                }catch (Exception e){
                    System.out.println("Main raise a accept");
                }
            }
        }catch (IOException e){
            System.out.println("Main raise a IOException");
        }
    }
}
