package il.ac.kinnert.etsm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public final class Hashy{
	
	//has table for topics + print writers.
	private static Hashtable<String, Vector<PrintWriter>> htTopics = new Hashtable<>();
	//vector of all the online sockets.
	private static Vector<Socket> vSockets = new Vector<>();
	private static FileWriter fw = null;
	private static BufferedWriter bw = null;
	// path files
	private static final String LOG_PATH = "Log.txt";
	
	/**
	 * add a client to a topic that he registered to.
	 * synchronized for trade safety.
	 * @param topicName - the topic he registered to.
	 * @param pt - print writer of the same client
	 * @return - true if signed up, false if already there.
	 */
	public static synchronized boolean addToTopic(String topicName, PrintWriter pt){
		Vector<PrintWriter> temp = null;
		//get the vector of the topic
		temp = htTopics.get(topicName);
		if(temp == null){  				//if no such vector, make one
			temp = new Vector<>();
		}
		else if(temp.contains(pt)){    //if client is already signed up, return false
			return false;
		}
		//add the client to the vector and return the vector to the hash - return true.
		temp.add(pt); 
		htTopics.put(topicName, temp);
		return true;
	}
		
	/**
	 * opposite of add topic - remove from topic which client has left.
	 * synchronized for thread safety.
	 * @param topicName - the topic he left
	 * @param pt - print writer of the same client
	 * @return - return true of removed, false if failed.
	 */
	public static synchronized boolean removeFromTopic(String topicName, PrintWriter pt){
		Vector<PrintWriter> temp = null;
		if(!htTopics.containsKey(topicName)){	//if there is no such topic yet - false
			return false;	
		}
		//get the vector of the same topic from hash
		temp = htTopics.get(topicName);
		if(!temp.contains(pt)){				//if the client not registered for the topic - false
			return false;
		}
		//remove the client writer from the vector
		temp.remove(pt);
		if(!temp.isEmpty()){				//if the vector is empty - remove the topic from has, else update the vector
			htTopics.put(topicName, temp);
		}
		else{
			htTopics.remove(topicName);
		}
		return true;
	}
	
	/**
	 * removing some client who left from all the topics he signed for.
	 * was a dam hard function to do, lots of error from the java side, too many foreach loop + list errors. - will explain on demand.
	 * @param pt - print writer of the client
	 */
	public static void removeFromAllTopics(PrintWriter pt){
		//set of strings to hold all the topics
		Set<String> df = null;
		//get all the topics from the hash table.
		//synchronized for trade safety.
		synchronized(Hashy.class){
				df =  htTopics.keySet();  
		    }	
		//get all the topics from set to array because - java
		Object[] topics = df.toArray();
		int length = topics.length;
		for(int i = 0; i < length ; i++){
			removeFromTopic(topics[i].toString(),pt); //run on each topic and remove the client from it with the above function.
		}
	}
	
	/**
	 * get the entire vector of a specific topic - in order to forward msg to all registered clients. 
	 * synchronized for trade safety.
	 * @param topic - the topic name
	 * @return - return the vector that requested
	 */
	public static synchronized Vector<PrintWriter> getVector(String topic){
		return htTopics.get(topic);
	}
	
	/**
	 * add a new client socket to the socket list - in order to close all sockets on demand.
	 * @param s - the client socket to be added.
	 * @return - return if succeeded.
	 */
	public static boolean addSocket(Socket s) {
		return vSockets.add(s);
	}
	
	/**
	 * remove a socket from the list if the client has left.
	 * synchronized for trade safety.
	 * @param s - the socket of the client
	 * @return - return true if succeeded.
	 */
	public static synchronized boolean removeSocket(Socket s) {
		return vSockets.remove(s);
	}
	
	/**
	 * remove all the sockets because server is closing
	 * synchronized for trade safety.
	 */
	public static synchronized void removeAllSockets() {
		//iterate over every socket and close it
		for (Socket socket : vSockets) {
			try {
				socket.close();
			} catch (Exception e) {}
		}
		try {
			//iterate over every socket and remove it from the list.
			for(int i = 0; i < vSockets.size(); i++) {
				vSockets.remove(0);
			}
		}catch (Exception e) {}
	}
	
	/**
	 * Initializing the Log file we are going to write to.
	 */
	public static void initiateLogFile() {
		try {
			fw = new FileWriter(LOG_PATH,true);
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		bw = new BufferedWriter(fw);
	}
	
	/**
	 * write the log into the file
	 * synchronized for trade safety.
	 * @param s - the string to write to the log
	 */
	public static synchronized void writeLog(String s) {
		try {
			//specific string demanded for the log file.
			bw.write(LocalDate.now() + " " + LocalTime.now() + ": " + s + '\n');
			bw.flush();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
	/**
	 * clear the entire hash table, because server disconnected and should not remember old clients.
	 */
	public static void cleanTopics() {
		htTopics.clear();
	}
}