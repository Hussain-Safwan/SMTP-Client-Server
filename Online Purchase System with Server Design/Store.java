import java.net.*; 
import java.io.*; 
import java.util.*;
import java.lang.*;




public class Store implements Runnable{ 
	private Socket socket		 = null; 
	private DataInputStream dataIn = null; 
	private DataOutputStream dataOut	 = null; 
	private static int Store_port;
	private static int Bank_port;
	private static String bank_IP ="";

	static String firstname = "";
	static String familyname = "";
	static String postCode = "";
	static String credit = "";
	static String item = "";
	static String quntity = "";
	static Map<String, Integer> map;
	static boolean isApproved = false;
	static boolean credentials = false;

	public Store(Socket skt){
		socket = skt;
	}

	public void runStore(String address, int port) 
	{ 
		try
		{ 
			map = new HashMap<String, Integer>();
			map.put("1", 150);
			map.put("2", 300);
			map.put("3", 240);
			map.put("4", 320);
			map.put("5", 190);
			socket = new Socket(address, port); 
			System.out.println("Connected"); 

			dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream())); 
			dataOut = new DataOutputStream (socket.getOutputStream());
		} 
		catch(UnknownHostException e) 
		{ 
			System.out.println(e); 
		} 
		catch(IOException e) 
		{ 
			System.out.println(e); 
		} 

		sendLines(dataOut);
		String bankStat = "nothing";
		try{
			bankStat = dataIn.readUTF();
		}
		catch(IOException e){
			System.out.println(e);
		}

		System.out.println("Bank Server says : " + bankStat);
		if(bankStat.equals("approved")){
			credentials = true;
			String tk = calculate();
			sendAmount(dataOut, tk, credit);
			// System.out.println("Sent amount: "+tk);
			try{
				bankStat = dataIn.readUTF();
				if(bankStat.equals("approved")){
					System.out.println("Transaction Approved");
					isApproved = true;
				}
				else{
					System.out.println("Transaction aborted: Insufficient Funds");
					isApproved = false;
				}
			}
			catch(IOException e){
				System.out.println(e);
			}
		}
		else{
			credentials = false;
		}
		try
		{ 
			dataIn.close(); 
			dataOut.close(); 
			socket.close(); 
		} 
		catch(IOException i) 
		{ 
			System.out.println(i); 
		} 
	} 

	public void run(){
		System.out.println("Running");
		BufferedReader br = null;
		PrintWriter pw = null;
		BufferedOutputStream bos = null;
		try{
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter (socket.getOutputStream(), true);
			bos = new BufferedOutputStream (socket.getOutputStream());

			String htmlData = br.readLine();
			System.out.println("htmlData: "+htmlData);
			StringTokenizer strtok = new StringTokenizer (htmlData);
			String method_up = strtok.nextToken();
			String method = method_up.toLowerCase();
			String reqFile_up = strtok.nextToken();
			String reqFile = reqFile_up.toLowerCase();
			if(htmlData.contains("POST")) {
                String str = null;
                while ((str = br.readLine()).length() != 0) {
                	// System.out.println(br.readLine());
                }

                StringBuilder payload = new StringBuilder();
                while (br.ready()) {
                    payload.append((char) br.read());
                }
                String userinfo = payload.toString();
                String infos[] = userinfo.split("=");
                for (int i = 1; i < infos.length; i++) {
                    String info[] = infos[i].split("&");
                    if (i == 1) firstname = info[0];
                    if (i == 2) familyname = info[0];
                    if (i == 3) postCode = info[0];
                    if (i == 4) credit = info[0];
                    if (i == 5) item = info[0];
                    if (i == 6) quntity = info[0];
                }
                System.out.println("Info : "+firstname+", "+familyname+", "+postCode+", "+credit+", "+item+", "+quntity);
                runStore(bank_IP, Bank_port);
                if(isApproved){
                	loadPage(pw, "success.html");
                	bos.flush();
                }
                else{
                	loadPage(pw, "aborted_funds.html");
                }
            }

            if(htmlData.contains("GET")){
            	System.out.println("Method : GET");
            	loadPage(pw, "Index.html");
            	bos.flush();
            }
 		}
		catch(IOException e){
			System.out.println(e);
		}
		finally{
			try{
				br.close();
				pw.close();
				bos.close();
			}
			catch(IOException e){
				System.out.println(e);
			}
		}
	}


		public void loadPage(PrintWriter printWriter, String fileAddress){
		BufferedReader bufferedReader;
		try{
			File file = new File(fileAddress);
			int fileLength = (int)file.length();
			System.out.println("File length: "+fileLength);
			bufferedReader = new BufferedReader (new FileReader(fileAddress));
			String line = bufferedReader.readLine();		
			printWriter.println("HTTP/1.1 200 OK");
			printWriter.println("Content-Type: text/html");
			printWriter.println("Content-length: " + fileLength);
			printWriter.println();
			printWriter.flush();
			while(line != null){
				// System.out.println(line);
				printWriter.println(line);
				line = bufferedReader.readLine();
			}
		}
		catch(IOException e){
			System.out.println("Error : "+e);
		}
	}


	// public void createServer() throws IOException{
	// 	try {
	// 	    serverSocket = new ServerSocket(storePort); 
	// 	} 
	// 	catch (IOException e) {
	// 	    System.err.println("Could not listen on the port");
	// 	    System.exit(1);
	// 	}

	// 	Socket clientSocket = null; 
	// 	try {
	// 	    clientSocket = serverSocket.accept();

	// 	    if(clientSocket != null) {           
	// 	        System.out.println("Connected");
	// 	    }
	// 	} 
	// 	catch (IOException e) {
	// 	    System.err.println("Accept failed.");
	// 	    System.exit(1);
	// 	}

	// 	PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

	// 	BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	// 	String msg;
	// 	StoreServer storeServer = new StoreServer();
	// 	if ((msg = br.readLine()) != null){
	// 		System.out.println(msg);
	// 		storeServer.loadPage(out);
	// 		System.out.println("Page loaded");
	// 	}

	// }

	public static String calculate(){
		int Quantiy = Integer.parseInt(quntity);
		int unit = map.get(item);
		int int_amount = Quantiy * unit;
		String amount = Integer.toString(int_amount);
		System.out.println("calculate: "+amount);
		return amount;
	}

	public static void sendLines(DataOutputStream dataOut){

		try{
			dataOut.writeUTF(firstname);
			dataOut.writeUTF(familyname);
			dataOut.writeUTF(postCode);
			dataOut.writeUTF(credit);
			System.out.println("Sent: "+firstname);
		}
		catch(IOException e){
			System.out.println(e);
		}
	}

	public static void sendAmount(DataOutputStream dataOut, String amount, String credit){
		try{
			dataOut.writeUTF(credit);
			dataOut.writeUTF(amount);
			System.out.println("Sent amount: "+amount+" for credit no: "+credit);
		}
		catch(IOException e){
			System.out.println(e);
		}
	}

	public static void main(String args[]){ 
		String store_port = args[0];
		String bank_host_ip = args[1];
		String bank_port = args[2];
		Bank_port = Integer.parseInt(bank_port);
		Store_port = Integer.parseInt(store_port);
		bank_IP = bank_host_ip;
		// store.runStore("127.0.0.1", bankPort); 
		// Connects to the bank

		try{
			ServerSocket serverSocket = new ServerSocket(Store_port);
			while(true){	
				Store store = new Store(serverSocket.accept()); // Connects to the browser
				Thread thread = new Thread(store);
				thread.start();
			}
		}
		catch(IOException e){
			System.out.println(e);
		}
	} 
} 
