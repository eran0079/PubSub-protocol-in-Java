package il.ac.kinneret.es;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


public class KeyboardListener {

		public static final String FILE_PATH = "clientConfiguration.txt";
		public static void main(String[] args) {
			
			while(true) {
			/* Thread listen to the server- notifies the Client 
			 * while the main Thread listens to Client(keyboard) and notifies the server */
			ServerListener listener;
			/* socket with the server */
			Socket serverSocket = null; 
			/* IP address object */
			InetAddress serverIP = null;
			int port = 0;
			/* like streamWriter -> into the server 
			 * while the client wish send messages to Server- e.g Protocol Commands */
			PrintWriter pwNetwork=null;
			/* reader from keyboard */
			BufferedReader brKeyboard = null;
			/* the object that get messages from server in listener thread */
			BufferedReader brNetwork =null;
			/* boolean flag for over between the modes of the program */
			boolean isConnect = false; 
			helperClass.disconnet = false;
			while(!isConnect) 
					 {
						 /* buffer read from keyboard(user) */
						 brKeyboard = new BufferedReader(new InputStreamReader(System.in));
						 /*print the main menu: connect / disconnect / quit*/
						 printUserConnectMenue();
						 String answer ="";
						 try
						 {
							 /*read the answer from user*/
							 answer=brKeyboard.readLine().toUpperCase();
							/*some catches marks-  read execution from user doesn't succeeded*/
						 }catch (IOException e1) {
							System.out.println("Error can't read from Keyboard: "+e1.getMessage());
						 }	
						/*multiple choice of the main menu*/
						switch(answer) 
						{
						 case "CONNECT" :
							 System.out.println("Trying to connect...\n");
							 isConnect=true;
							 try {
								 	BufferedReader brFile= new BufferedReader(new FileReader(FILE_PATH));
									serverIP = InetAddress.getByName(brFile.readLine());
									port = Integer.parseInt(brFile.readLine());
									brFile.close();
								 	/*open socket*/
								 	serverSocket = new Socket(serverIP, port);
								 	/*success checking*/
								 	 if(serverSocket != null)
										{
											System.out.println("Connected to the server.\n");
											brNetwork = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
										}
								 	 /*open writer to server by the opened socket*/
								 	pwNetwork = new PrintWriter(serverSocket.getOutputStream());
								 	/*some catches - if serverSocket or pwNetwork doesn't succeeded*/	
							 } catch (UnknownHostException unx)
							 {
								System.out.println("Error: Can't resolve host: " + unx.getMessage());
								isConnect=false;
								break;
							 } catch (NumberFormatException nfe)
							 {
								System.out.println("Error: Invalid port: " + nfe.getMessage());
								isConnect=false;
								break;
							 }catch (ConnectException ce)
							 {
								System.out.println("Error Connection: no process listening now! try again later  \n");
								isConnect=false;
								break;
							 }catch (IOException e) 
								{
									e.printStackTrace();
									isConnect=false;
								}
								catch (NullPointerException e) 
								{
									System.out.println("can't write to Server: " + e.getMessage()+"\n");
									isConnect=false;
									break;
									
								}
							 break;
							 
						 case "DISCONNECT":
							 isConnect = false;
							 System.out.println("Not logged-in yet\n");
							 break;
						 
						 case "QUIT": 
							 System.out.println("Good bye!\n");
							 return;
							 default: illegalUsage();
						}
							
			 	}//while(!isConnect)
			
			/*open the Thread that listening to SERVER*/
			listener = new ServerListener(brNetwork);
			listener.start();
			while (isConnect)
			{    
				/*second menu - what can user do in pub/sub Protocol*/
				printUserInterfaceConnected();
				try
				{
					/*read the user choice - join / quit / disconnect / message to delivery / */
					String userIn = brKeyboard.readLine();
					String[] sentence = userIn.split(" ");
					/******Client Protocol checking the user commands*******/
					/*user press blank command (only ENTER)*/
					if(sentence.length==0) System.out.println("Blank command! please retry");
					/*user insert 1 word command : check if it "quit"/ "connect" / "disconnect"*/
					if(sentence.length == 1)
					{
						switch (sentence[0].toUpperCase()) 
							{
								case "QUIT":
								{
									try
									{	/*sending CLOSE protocol command to Server*/
										pwNetwork.println("CLOSE");
										pwNetwork.flush();
										helperClass.disconnet=true;
										//close socket on exit from program - closing the thread by the exception will thrown
										serverSocket.close();
									}catch (IOException ioe) {
										System.out.println("Error: "+ioe.getMessage());
										break;
									}
									catch (NullPointerException npe) {
										System.out.println("Error: "+npe.getMessage());
										break;
									}
									System.out.println("Good Bye!");
									return;
								}
								case "CONNECT":
								{
									/*check if the listener not alive -
									 * unpredictable exit of the server- e.g the server executed close with some client,
									 * therefore we doing check if it exist, or everything must start over*/
									if(!listener.isAlive())
									{
										try 
										{
												/*while the user insert connect twice, while the server closed */
												serverSocket = new Socket(serverIP, port);
												/*open socket between client and server*/
												if(serverSocket != null)
												{
													System.out.println("Connected to the server.");
													brNetwork = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
												}		
												 /*open writer to server by the opened socket*/
											 	pwNetwork = new PrintWriter(serverSocket.getOutputStream());
											 	/*some catches - if serverSocket or pwNetwork doesn't succeeded*/	
										 } catch (UnknownHostException unx)
										 {
											System.out.println("Error: Can't resolve host: " + unx.getMessage());
											isConnect=false;
											break;
										 } catch (NumberFormatException nfe)
										 {
											System.out.println("Error: Invalid port: " + nfe.getMessage());
											isConnect=false;
											break;
										 }catch (ConnectException ce)
										 {
											System.out.println("Error: NO process listening to this socket \n");
											isConnect=false;
											break;
										 }catch (NullPointerException e) {
											e.getMessage();
											isConnect=false;
											break;
										}
										 catch (Exception ex)
										 {
											System.out.println("error: " + ex.getMessage());
											ex.printStackTrace();		
											break;
										 }
										/*the serverSocket is open to communicate
										 * we have to listen with the listener thread
										 */
										listener = new ServerListener(brNetwork);
										listener.start();
									}
									/*if the user try to connect in connecting state*/
									else System.out.println("Already connect");
									break;
										
								}	
								case "DISCONNECT":
								{
									System.out.println("Disconnecting from server...");
									/*send CLOSE to the server*/
									pwNetwork.println("CLOSE");
									pwNetwork.flush();
									/*closing the socket instead of the listener thread because it un-safety*/
									try
									{	
										/*safety closing instead of use the Thread.stop function 
										 * the thread will stop with the exception*/
										serverSocket.close();
									}catch(NullPointerException e){
										e.getMessage();
										e.printStackTrace();
										return;
									 }
									catch (SocketException e) {
										e.getMessage();
										e.printStackTrace();
										return;
									}
									catch (IOException e) {
										 System.out.println(e.getMessage());
										 e.printStackTrace();
										 return;
									}
									isConnect=false;
									/*static boolean variable get true if the client closed the socket with server
									 * and false while the server closed the socket with the client*/
									helperClass.disconnet = true;
									System.out.println("Disconnected!");
									Thread.sleep(200);
									break;
								}
								default:illegalUsage(); break;
						}
							
					}
					/*case that the client wrote protocol commands  - join to topic 
					 * or leave topic
					 */
					else if(sentence.length == 2){						
						switch (sentence[0].toUpperCase()) {
							case "JOIN":{
								if(sentence[1].toUpperCase().equals("JOIN")){
									specialTopic();
									break;
								}
								pwNetwork.println("REGISTER "+sentence[1]);
								pwNetwork.flush();
								break;
								}
					 		case "LEAVE":{
								if(sentence[1].toUpperCase().equals("LEAVE")){
									specialTopic();
									break;
								}
					 			pwNetwork.println("LEAVE "+sentence[1]);
								pwNetwork.flush();
								break;
					 		}
					 		/*
					 		 * * if it isn't join or leave - it "topic" + one word message 
					 		 * */
							default:{
								pwNetwork.println("SEND "+ userIn);
								pwNetwork.flush();
								break;
							}
						}					
					}
					/*
					 * every another case is message - first word is topic, 
					 * 
					 * */
					else if (sentence.length >= 3)
					{
					
						/*if the sentence is more than 2 words 
						 *if the first word is protocol word, we will print command no found
						 *else it send all the sentence to the server with SEND command*/
						switch(sentence[0].toUpperCase())
						{
							case "JOIN": specialTopic();break;
							case "LEAVE":specialTopic();break;
							case "QUIT":specialTopic();break;
							default: {
								pwNetwork.println("SEND "+ userIn);
								pwNetwork.flush();
								break;}
						}
						
					}
			} catch (Exception iox)
			{
				System.out.println("Error in network communication: " + iox.getMessage());				
			}
					
		}
	}
			
}

	/**
	 * first menu - connect , disconnect or quit before connecting to server
	 */
	private static void printUserConnectMenue() 
	{	
		System.out.println("\nPlease write your chosen command:"
							+"\n1. connect"
							+"\n2. disconnect"
							+"\n3. quit");
	}
	
	/**
	 * illegal input msg.
	 */
	private static void illegalUsage()
	{
		System.out.println("Command not found");
	}
	
	/**
	 * special topics illegal.
	 */
	private static void specialTopic(){
		System.out.println("Cannot use the commands as topics.");
	}
	
	/**
	 * while the client is connected to server.
	 * this menu views the client options of commands
	 */
	private static void  printUserInterfaceConnected() 
	{
		System.out.println("\nFollow the instructions below:"
							+"\n*** Purpose ***************************** Command ******************** Example *******************"
							+"\n1) Join to topic command:\t\tjoin\ttopic \t\t e.g. join cats"
							+"\n2) Leave topic command:\t\t\tleave\ttopic \t\t e.g. leave cats"
							+"\n3) Send a message command:\t\ttopic \"your sentence\" \t e.g. cats I like black cats"
							+"\n4) Disconnect from the server:\t\tdisconnect \t\t e.g. disconnect"
							+"\n5) Exit command:\t\t\tquit\n");
	}

}
