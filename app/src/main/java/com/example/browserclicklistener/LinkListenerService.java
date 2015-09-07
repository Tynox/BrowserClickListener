package com.example.browserclicklistener;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by pocktynox on 2015/9/6.
 */
public class LinkListenerService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("LinkListenerService", "service on create");
        try {
            startListener();
            runForever();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("LinkListenerService", "start socket listener error");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("LinkListenerService", "stop socket listener error");
        }
    }

    private void startListener() throws IOException{
        serverSocket = new ServerSocket(55555);
    }

    private void runForever() {
        serverThread.start();
    }

    private void launchApp() {
        if (isAppRunningFront()) {
            return;
        }
        Log.d("LinkListenerService", "launch app");
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("source", "service");
        startActivity(intent);
    }

    @SuppressWarnings("deprecation")
    private boolean isAppRunningFront() {
        List<ActivityManager.RunningTaskInfo> infos = ((ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE)).getRunningTasks(1);
        return infos.size() > 0 && getPackageName().equals(infos.get(0).topActivity.getPackageName());
    }

    private ServerSocket serverSocket;
    private Thread serverThread = new Thread(new ServerRunnable());

    private class ServerRunnable implements Runnable {
        @Override
        public void run() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Log.d("LinkListenerService", "connection accept");
                    new ServerHandleThread(clientSocket).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("LinkListenerService", "connection accept error");
                }
            }
        }
    }

    private class ServerHandleThread extends Thread {
        protected Socket socket;

        public ServerHandleThread(Socket clientSocket) {
            socket = clientSocket;
            Log.d("LinkListenerService", "client info: " + socket.getInetAddress().getHostAddress() + ", port: " +socket.getPort());
        }

        @Override
        public void run() {
            receiveMessage();
            launchApp();
        }

        private void receiveMessage() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));

                        String info;
                        while ((info = bufferedReader.readLine()) != null && !info.isEmpty()) {
                            Log.d("LinkListenerService", "info: " + info);
                        }
                        sendMessage();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void sendMessage() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedWriter out = null;
                    try {
//                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//                        out.println("HTTP/1.1 200 OK");
//                        out.println("Connection:close");
//                        out.println("success");
//                        out.flush();
//                        out.close();
                        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 7" +
                                "\r\n\r\nsuccess");
                        out.flush();
                        Log.d("LinkListenerService", "send feedback");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
