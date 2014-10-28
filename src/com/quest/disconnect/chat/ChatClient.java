/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.disconnect.chat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author connie
 */
public class ChatClient extends Client {
    private String host;
    public ChatClient(String host,int port) throws IOException{
        super(host, port);
        this.host=host;
       
    }

    @Override
    protected void messageReceived(Object message) {
       ChatWindow.newMessage((String)message,host,null);
    }
    
    @Override
    protected void ClientDisconnected(int clientID){
      ChatWindow.setInfo(host+" is offline or you have been disconnected"); 
      
    }
    
    @Override
    protected void ClientConnected(int clientID){
        try {
            ChatWindow.setInfo("You have been connected to: "+host);
            InetAddress address= InetAddress.getLocalHost();
            send(new Request("identity", address.getHostName()));
        } catch (UnknownHostException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
