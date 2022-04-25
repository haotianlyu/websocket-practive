package com.websocket.haotian.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ServerEndpoint("/websocket/{username}")
public class WebSocket {

    public static AtomicInteger onlineNumber = new AtomicInteger(0);

    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();

    private Session session;

    private String username;

    // OnOpen build session
    @OnOpen
    public void onOpen(@PathParam("username") String username, Session session) {
        onlineNumber.getAndIncrement();
        log.info("current id: " + session.getId() + ", username: " + username);
        this.username = username;
        this.session = session;
        log.info("new connection! onlineNumber: " + onlineNumber.intValue());
        try {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("messageType", 1);
            map1.put("username", username);
            sendMessageAll(JSON.toJSONString(map1), username);

            clients.put(username, this);
            Map<String, Object> map2 = new HashMap<>();
            map2.put("messageType", 3);
            Set<String> set = clients.keySet();
            map2.put("onlineUsers", set);
            sendMessageTo(JSON.toJSONString(map2), username);
        } catch (Exception e) {
            log.info(username + "error!");
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.info("Server side crashed " + error.getMessage());
    }

    @OnClose
    public void onClose() {
        onlineNumber.getAndDecrement();
        clients.remove(username);
        try {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("messageType", 2);
            map1.put("username", username);
            map1.put("onlineUsers", onlineNumber.intValue());
            sendMessageAll(JSON.toJSONString(map1), username);
        } catch(IOException e) {
            log.info(username + " error when notifying other users when logout");
        }
        log.info("There's connection close, current user online is " + onlineNumber.intValue());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            log.info("receive message: " + message + " from session id: " + session.getId());
            JSONObject jsonObject = JSON.parseObject(message);
            String textMessage = jsonObject.getString("message");
            String fromUsername = jsonObject.getString("username");
            String toUsername = jsonObject.getString("to");
            Map<String, Object> map1 = new HashMap<>();
            map1.put("messageType", 4);
            map1.put("textMessage", textMessage);
            map1.put("fromUsername", fromUsername);
            if(toUsername.equals("All")) {
                map1.put("toUsername", "All");
                sendMessageAll(JSON.toJSONString(map1), fromUsername);
            } else {
                map1.put("toUsername", toUsername);
                sendMessageTo(JSON.toJSONString(map1), toUsername);
            }
        } catch(IOException e) {
            log.info("Error when trying to send out message");
        }
    }

    public void sendMessageTo(String message, String toUsername) throws IOException {
        WebSocket ws = clients.get(toUsername);
        if(ws != null && ws.session.isOpen()) {
            ws.session.getAsyncRemote().sendText(message);
        }
    }

    public void sendMessageAll(String message, String FromUsername) throws IOException {
        for(WebSocket item: clients.values()) {
            item.session.getAsyncRemote().sendText(message);
        }
    }
}
