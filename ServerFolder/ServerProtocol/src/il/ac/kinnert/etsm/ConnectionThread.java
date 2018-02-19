package il.ac.kinnert.etsm;

import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionThread extends Thread {
	ServerSocket listeningSocket;
	/**
	 * build a new listening thread
	 * @param listeningSocket
	 * listen on endless loop
	 */
	public ConnectionThread(ServerSocket listeningSocket){
		super();
		this.listeningSocket = listeningSocket;
	}
	@Override
	public void run(){
		try{
			while(true){
				Socket clientConnection = listeningSocket.accept();
				
				// see if we were interrupted - then stop
				if (this.isInterrupted())
				{
					System.err.println("Stopped listening since we were interrupted.");
					return;
				}
				//add the socket to a socket list.
				Hashy.addSocket(clientConnection);

				
				ClientThread handleClient = new ClientThread(clientConnection);
				handleClient.start();
			}
		}catch(Exception e){}
	}
}