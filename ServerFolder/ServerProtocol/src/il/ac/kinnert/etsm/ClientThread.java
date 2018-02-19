package il.ac.kinnert.etsm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Vector;

public class ClientThread extends Thread {
	//the client socket
	Socket clientConnection;
	public ClientThread(Socket clientConnection){
		super();
		this.clientConnection = clientConnection;
	}
	
	@Override
	public void run(){
		//save client socket as string for convenience.
		String clientIP = clientConnection.getRemoteSocketAddress().toString();
		//notify the server and obviously the log file.
		System.out.println("Received connection from: " + clientIP);			
		Hashy.writeLog(clientIP + ": Received connection from: " + clientIP);
		//steam out and in.
		BufferedReader brIn = null;
		PrintWriter pwOut = null;
		try {
			//initialize the streams.
			brIn = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
			pwOut = new PrintWriter(clientConnection.getOutputStream());
			//loop to handle the client endlessly.
			while (true)
			{
				//read the protocol recieved from client
				String recievedString = brIn.readLine().trim();
				if ( recievedString.length() == 0)
				{
					Hashy.writeLog("Protocol failure, received empty stream.");
					throw new Exception("Received empty stream.");					
				}
				//split the string by separated space.
				String[] stringParts = recievedString.split(" ");
				String sentence = "";
				//check if the number of protocol words is bigger than 2, then its a "send" msg with sentence.
				if (stringParts.length > 2){
					//connect all the parts of the message into 1 string.
					sentence = String.join(" ", Arrays.copyOfRange(stringParts,2,stringParts.length));
				}
				if (stringParts.length >= 2){
					stringParts[1] = stringParts[1].toLowerCase();
				}
				//read the protocol word.
				switch(stringParts[0]){
					case "REGISTER":
						//write to log
						Hashy.writeLog(clientIP + ": " + recievedString);
						//try to add some1 to topic, if it worked ok, else error.
						if (Hashy.addToTopic(stringParts[1], pwOut)){
							pwOut.println("OK");
							pwOut.flush();
							Hashy.writeLog(clientIP + ": OK");
						}
						else{//already registered.
							pwOut.println("ERROR");
							pwOut.flush();
							Hashy.writeLog(clientIP + ": ERROR");
						}
						break;
					case "LEAVE": 
						//write log
						Hashy.writeLog(clientIP + ": " + recievedString);
						//try to remove some1 from topic, if worked ok, else error.
						if (Hashy.removeFromTopic(stringParts[1], pwOut)){
							pwOut.println("OK");
							pwOut.flush();
							Hashy.writeLog(clientIP + ": OK");
						}
						else{
							//user not registered to the topic
							pwOut.println("ERROR");
							pwOut.flush();
							Hashy.writeLog(clientIP + ": ERROR");
						}
						break;
					case "SEND": 
						//write log
						Hashy.writeLog(clientIP + ": " + recievedString);
						pwOut.println("OK");
						pwOut.flush();	
						Hashy.writeLog(clientIP + ": OK");
						//get the vector of the topic in the protocol.
						Vector<PrintWriter> pwForward = Hashy.getVector(stringParts[1]);
						if(pwForward == null){
							//if no1 registered to the topic, continue.
							continue;
						}
						//iterate over all the registered clients to the topic and send a forward msg protocol.
						for (PrintWriter pw : pwForward) {
							if(pw != pwOut){
								pw.println("FORWARD " + stringParts[1] + " " + clientIP + " " + sentence);
								pw.flush();
								Hashy.writeLog(clientIP + ": " + "FORWARD " + stringParts[1] + " " + clientIP + " " + sentence);
							}
						}
						break;
					case "CLOSE":
						clientConnection.close();
						//client closed connection, remove him from the list.
						Hashy.removeSocket(clientConnection);
						System.out.println("Closed connection with: "+ clientIP);
						//remove client from all the topics.
						Hashy.removeFromAllTopics(pwOut);
						Hashy.writeLog(clientIP + ": CLOSE");
						return;
					default:
						//recieved wrong protocol string.
						pwOut.println("ERROR");
						pwOut.flush();
						Hashy.writeLog(clientIP + ": ERROR");
				}				
			}
		}
		catch (SocketException se)
		{
			System.out.println("The connection with client "+ clientIP + " has been terminated.");
			Hashy.writeLog(clientIP + ": The connection with client "+ clientIP + " has been terminated.");
		}
		catch (NullPointerException npe){
			System.out.println("Error: Client closed his socket.");
		}
		catch(Exception e) {
			System.out.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
		finally{
			Hashy.removeFromAllTopics(pwOut);
			Hashy.removeSocket(clientConnection);
		}
	}
}

