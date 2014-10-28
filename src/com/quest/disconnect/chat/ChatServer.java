/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.quest.disconnect.chat;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author connie
 */
public class ChatServer extends Chat {
    private ConcurrentHashMap clients=new ConcurrentHashMap();
   public ChatServer(int port) throws IOException{
       super(port);
   }
   
   @Override
   protected void messageReceived(int playerID, Object message) {
       executor.execute(new MessageProcessor(message, playerID));
   }
   
    @Override
   protected void clientIsConnected(int playerID) {
     ChatWindow.setInfo("New client connection accepted");  
   }
    
    @Override
   protected void clientIsDisconnected(int clientID) {
      clients.remove(clientID);
   }
    

   
  private class MessageProcessor implements Runnable{
    private Object message;
    private int clientID;
    public MessageProcessor(Object message, int clientID){
       this.message=message;
       this.clientID=clientID;
    }
    
   @Override
    public void run(){
      if(message instanceof Request){
         Request newRequest=(Request)message;  
         String msg=newRequest.getMessage();
         if(msg.equals("identity")){
            String identity=(String) newRequest.getRequestObject();
            clients.put(clientID, identity );
            ConcurrentHashMap verifiedIdentities=ChatWindow.getIdentities();
            String alias = (String) verifiedIdentities.get(identity);
            if(alias!=null){
              ChatWindow.newMessage("Connection accepted from "+alias,null,alias);    
            }
            else{
              ChatWindow.newMessage("Connection accepted from "+identity,null,alias);   
            }  
         }  
    }
    // this means a client is responding to a previous request
    else if(message instanceof Response){
         Response newResponse=(Response)message;
         String msg = newResponse.getMessage();
         String ip=(String) newResponse.getResponse();
         ConcurrentHashMap verifiedIdentities=ChatWindow.getIdentities();
         String alias = (String) verifiedIdentities.get(ip);
         if(alias!=null){
             ChatWindow.newMessage(msg,alias,null );   
         }
        else{
              ChatWindow.newMessage(msg,ip,null );     
         }
         
       }
     }
   }
}
