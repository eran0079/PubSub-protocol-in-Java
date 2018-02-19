package il.ac.kinnert.etsm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;


public class ServerThread {
	
	//file path
	public static final String FILE_PATH = "configuration.txt";
	
	public static void main(String[] args) {
		// vector to hold addresses,  Set to make it unique (no duplicates)
		Vector<InetAddress> myAddresses = new Vector<InetAddress>();
		Set<InetAddress> uniqueIPs = new HashSet<InetAddress>();
		// show the computers IP Address
		try{    
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while(interfaces.hasMoreElements())
				{
					// got the next interface
					NetworkInterface ni = interfaces.nextElement();
					// got its addresses
					Enumeration <InetAddress> adds = ni.getInetAddresses();
					while(adds.hasMoreElements())
					{
						//got an address
						InetAddress add = adds.nextElement();
						//add it to the list
						myAddresses.add(add);
					}
				}
				try{					
					//add any - listen to all IP's
					myAddresses.addElement(InetAddress.getByAddress(new byte[]{0,0,0,0}));
					//add local host
					myAddresses.addElement(InetAddress.getByAddress(new byte[]{127,0,0,1}));
				}
				catch(UnknownHostException e){
					System.out.println("Error adding InetAddresses element:" +e.getMessage());
							e.printStackTrace();
							return;				
				}						
		}catch(SocketException e)
		{			
			System.out.println("Error retrieving network interfaces: "+e.getMessage());
			e.printStackTrace();
			return;
		}
		
		//the port which the server going connect to
		int port = 0;
		//trying to read the port number from the configuration file.
		try {
			BufferedReader brFile = new BufferedReader(new FileReader(FILE_PATH));
			port = Integer.parseInt(brFile.readLine());
			brFile.close();
			
		} catch (IOException ioe) {
			System.out.println("Could not find file!!! Default file path in: " + System.getProperty("user.dir"));
			return;
		} catch (NumberFormatException nfe){
			System.out.println("File format corrupted!!! must contain port number only!");
			return;
		} catch (Exception e){
			e.printStackTrace();
		}
		//Initiate the log file.
		Hashy.initiateLogFile();
		//make all IP addresses unique and not duplicated.
		uniqueIPs.addAll(myAddresses);
		
		//boolean connected for a condition inside loops - not to connect/disconnect twise etc.
		boolean connected = false;
		ServerSocket listeningSocket = null;
		ConnectionThread listener = null;
		//Initializing the buffered reader from keyboard.
		BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
		//function to print the rules to use the commands of the server
		printRules();
		
		//the entire user interface: 
		while(true){	
				try {
					//read keyboard input
					String userAnswer = brIn.readLine().trim().toUpperCase();
					switch(userAnswer){
						case "CONNECT": {
							try { 
								if(!connected){ //if im not connected yet, then try and connect.
									//function to print all the IPs.
									printIPs(uniqueIPs);
									InetAddress ipAddr = chooseIPAddress(uniqueIPs);
									listeningSocket = new ServerSocket(port,10,ipAddr);
									listener = new ConnectionThread(listeningSocket);
									listener.start();
									System.out.println("Listening on: "+ listeningSocket.getLocalSocketAddress());
									Hashy.writeLog("Connected to: "+ listeningSocket.getLocalSocketAddress());
									connected = true;
								}else{
									System.out.println("Already connected to: " + listeningSocket.getLocalSocketAddress());
								}
							}catch(BindException be){
								System.out.println("Cannot connect. " + be.getMessage());
								Hashy.writeLog("BindException occurred: " + be.getMessage());
								printRules();
								continue;
							}catch (IOException ioe) {
								System.out.println("Failed to connect. "+ioe.getMessage());
								Hashy.writeLog("IOException occurred: " + ioe.getMessage());
								printRules();
								continue;
							}catch (Exception e){
								System.out.println("Failed to connect. " + e.getMessage());
								Hashy.writeLog("IOException occurred: " + e.getMessage());
							}
							break;
						}
						case "CLOSE":{
							if(connected){
								try { //if user decided to close the serve, close all the connected sockets.
									Hashy.removeAllSockets();
									listeningSocket.close();
								}
								catch (IOException e) { System.out.println("Failed to close socket.");}
								connected = false;	
								//closing the server
								System.out.println("Server closed.");
								Hashy.writeLog("Server closed.");
								//cleaning all topics used so far because, never asked to remember.
								Hashy.cleanTopics();
							}else{
								System.out.println("Server is already down.");
							}
							break;
						}
						case "QUIT":{
							try {
								if(connected){
									listeningSocket.close(); 
								} 
							}
							catch (IOException e) { System.out.println("Failed to close socket.");}
							finally {
								//remove all sockets and all topics, program shut down.
								Hashy.removeAllSockets();
								Hashy.cleanTopics();
							}
							System.out.println("Bye bye.");
							Hashy.writeLog("Server terminated.");
							return;
						}
						default:{
							System.out.println("Ilegal input!");
							printRules();
						}
					}		
				} catch (IOException ioe) {
					System.out.println("Failed to read input. "+ ioe.getMessage());
					Hashy.writeLog("IOException occured, couldnt read keyboard: " + ioe.getMessage());
				}
		}
	}
	
	/**
	 * function that print all the addresses in the pc.
	 * @param addresses - set of addresses to be printed.
	 */
	private static void printIPs(Set<InetAddress> addresses) {
		int counter = 0;
		//explain how to choose an IP address.
		System.out.println("Choose one of the following options by typing the option number:");
		for(InetAddress add: addresses){
			System.out.println("(" + ++counter + "): " + add.toString());
		}
		System.out.println("Or type your own IP address and presse enter:" );
	}
	
	/**
	 * function that helps the server to chose an IP
	 * @param adds - the set of all the addresses.
	 * @return - return the chosen IP address.
	 */
	private static InetAddress chooseIPAddress(Set<InetAddress> adds){
		//list to hold the index of the chosen IP
		List<InetAddress> list = new ArrayList<InetAddress>(adds);		 
		//connect to the keyboard
		BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
		InetAddress ia = null;	
		boolean succsses = true;
		do{
			try{	
				//read the keyboard input ip
				String answer = brIn.readLine().trim();
				//check if the input is manual IP or chosen index
				if(answer.contains(".") || answer.contains(":")) {
					ia = InetAddress.getByName(answer);
				}else {
					ia = list.get(Integer.parseInt(answer)-1);			
				}
				succsses = false;
			}catch(Exception e){
				System.out.println("Ilegal input! try again (number of the option or legal IP):");
			}
		}
		while (succsses);
		return ia;	
	}
	
	/**
	 * Print the rules of the server commands.
	 */
	private static void printRules(){
		System.out.println("Options Menu:\n"
				+ "\"Connect\" to connect to a socket, \"Close\" to close the server, \"Quit\" to close the program.");
	}
}
