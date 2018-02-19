package il.ac.kinneret.es;

import java.io.BufferedReader;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

//import javax.xml.bind.Marshaller.Listener;

public class ServerListener extends Thread {

	/*Date format to Hour:minuts:seconds */
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
	/*current time variable*/
	LocalDateTime currentTime;
	/* initialize stream object which read from server*/
	BufferedReader brNetwork = null;
	public ServerListener(BufferedReader brNetwork){
		super();
		this.brNetwork= brNetwork;
	}
	
	
	@Override
	public void run(){
		
		try{
			while (true)
			{
				try {
				/*result array get into the stream according to the num of spaces */
				String[] result = brNetwork.readLine().split(" ");
				if(result.length == 1)
				{
					/* if the server sent 1 word protocol message - it can be Error / OK */
					switch(result[0].toUpperCase())
					{
						case "OK":  System.out.println("Successfully Executed\n");
									break;
						case "ERROR":System.out.println("Error Occurred\n");
									break;
					}	
				}else{
					/* if the server sent more than 1 word protocol message - it can be only forward MSG */
					String rebuildMsg="";
					/*editing the msg from server according the rules - 
					 * adding scopes around the "tpic".
					 * take the senderIP
					 * adding the time was the message sent from the server
					 * finally writing the MSG 
					 * result[0] = "FORWARDING"
					 * result[1] = "some topic"
					 * result[2]=	"senderIP" 
					 * Arrays.copyOfRange merge the string from: String[] result , at  index, until index 
					 * means, all the MSG without the sender, or Protocol prototype*/
					rebuildMsg = String.join(" ", Arrays.copyOfRange(result, 3, result.length));
					String formatMSG = "("+ result[1]+") " +result[2]+" "+dtf.format(LocalDateTime.now())+" - "+rebuildMsg;
					System.out.println(formatMSG);
				}//else
				}catch (NullPointerException ex){
					/* network communication isn't work- while the server doing quit*/
					System.out.println("The server closed the connection with you");
					return;
				}catch(SocketException sc){
					if(!helperClass.disconnet)
					/*  while the server does unpredictable exit*/
					System.out.println("The server disapeared");
					return;
				}
			}
		}catch(Exception ex){
			System.err.println("Error listening for connections"+ ex.getMessage());
			return;
		}
	}

}
