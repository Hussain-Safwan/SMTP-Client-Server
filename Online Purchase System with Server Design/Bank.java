import java.net.*; 
import java.io.*; 
import java.util.*;
/*
Arnab#Paul#1234#0023#20
Safwan#Hussain#0997#11001100#120
Sakib#Khan#5678#0045#50
*/
public class Bank 
{ 
	private Socket		 socket = null; 
	private ServerSocket server = null; 
	private DataInputStream dataIn	 = null;
	private DataOutputStream dataOut = null; 

	public void runBank(int port) 
	{ 
		try
		{ 
			server = new ServerSocket(port); 
			System.out.println("Server started at Port:" + port); 

			socket = server.accept(); 
			System.out.println("Client accepted"); 

			dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream())); 
			dataOut = new DataOutputStream (socket.getOutputStream());

			String [] Lines = new String[100];
			Lines = receiveLines(dataIn);
			if(checkFile(Lines, "database.txt")){
				System.out.println("Initials approved");
				try{
					dataOut.writeUTF("approved");
				}
				catch(IOException e){
					System.out.println(e);
				}
				String credit_number, amount;
				try{
					credit_number = dataIn.readUTF();
					amount = dataIn.readUTF();
					System.out.println("Checking for : "+credit_number);
					try{
						if(checkAmount(credit_number, amount, "database.txt")){
							updateFile(credit_number, "database.txt", amount);
							dataOut.writeUTF("approved");
							System.out.println("Balance verified");
						}
						else{
							System.out.println("Insufficient funds");
							dataOut.writeUTF("rejected");
						}
					}
					catch(IOException e){
						System.out.println(e);
					}
				}
				catch(IOException e){
					System.out.println(e);
				}
			}

			else{
				System.out.println("Initials rejected. Transaction aborted");
				try{
					dataOut.writeUTF("rejected");
				}
				catch(IOException e){
					System.out.println(e);
				}
			}

			System.out.println("Closing connection"); 

			socket.close(); 
			dataIn.close(); 
		} 
		catch(IOException i) 
		{ 
			System.out.println(i); 
		} 
	} 

	public static String[] receiveLines(DataInputStream dataIn){
		String line = ""; 
		String[] Lines = new String[100];
			for(int i=0; i<4; i++){ 
				try
				{ 
					line = dataIn.readUTF(); 
					Lines[i] = line;
					// System.out.println(line); 

				} 
				catch(IOException e) 
				{ 
					System.out.println(e); 
				} 
		}

	    return Lines;

	}

	public static boolean checkFile(String[] Lines, String fileName){
		BufferedReader bufferedReader;
		try{
			bufferedReader = new BufferedReader (new FileReader(fileName));
			String line = bufferedReader.readLine();
			while(line != null){
				// System.out.println(line);
				StringTokenizer strtok = new StringTokenizer(line, "#");
				String firstname = strtok.nextToken();
				String familyname = strtok.nextToken();
				String postCode = strtok.nextToken();
				String credit = strtok.nextToken();
				line = bufferedReader.readLine();
				// System.out.println("database: "+firstname+", "+familyname+", "+postCode+", "+credit);
				// System.out.println("input: "+Lines[0]+", "+Lines[1]+", "+Lines[2]+", "+Lines[3]);
				if( firstname.equals(Lines[0]) && familyname.equals(Lines[1]) && postCode.equals(Lines[2]) && credit.equals(Lines[3]) ){
					return true;
				}

			}
			bufferedReader.close();
		}
		catch(IOException e){
			System.out.println("Error : "+e);
		}

		return false;
	}

	public static boolean checkAmount(String credit, String Amount, String fileName){
		BufferedReader bufferedReader;
		try{
			bufferedReader = new BufferedReader(new FileReader(fileName));
			String line = bufferedReader.readLine();
			while(line != null){
				// System.out.println("Line: "+line);
				StringTokenizer strtok = new StringTokenizer(line, "#");
				String db_amount = "";
				String db_credit = "";
				for(int i=0; i<4; i++){
					db_credit = strtok.nextToken();
				}
				if(db_credit.equals(credit)){
					// System.out.println("Account found");
					strtok.nextToken();
					db_amount = strtok.nextToken();
					System.out.println("DB Amount: "+db_amount);
					int db_int_amount = Integer.parseInt(db_amount);
					int input_int_amount = Integer.parseInt(Amount);
					if (db_int_amount >= input_int_amount){
						// System.out.println("Amount verified");
						return true;
					}
					// System.out.println("Amount ran short :'-(");
				}
				line = bufferedReader.readLine();
			}
		}
		catch(IOException e){
			System.out.println(e);
		}
		return false;
	}

	public static void updateFile(String credit, String fileName, String Amount){
		// System.out.println("Amount to be deducted: "+Amount);
		BufferedReader bufferedReader;
		BufferedWriter bufferedWriter, tempWriter;
		try{
			bufferedReader = new BufferedReader (new FileReader(fileName));
			String [] temp = new String [100000];
			String line = bufferedReader.readLine();
			String intactLine = line;
			String editLine = "";
			int j=0;
			while(line != null){
				StringTokenizer strtok = new StringTokenizer(line, "#");
				String db_amount = "";
				String db_credit = "";
				for(int i=0; i<4; i++){
					db_credit = strtok.nextToken();
				}

				if(!db_credit.equals(credit)){
					temp[j] = intactLine;
					// System.out.println("Temp'd: "+temp[j]);
					j++;
				}
				else{
					editLine = intactLine;
					// System.out.println("Line to be edited: "+editLine);
				}
				line = bufferedReader.readLine();
				intactLine = line;
			}

				StringTokenizer strtok = new StringTokenizer(editLine, "#");
				String balance;
				String fname, famname, postcode, creditNo, _credit;
				fname = strtok.nextToken();
				famname = strtok.nextToken();
				postcode = strtok.nextToken();
				creditNo = strtok.nextToken();
				balance = strtok.nextToken();
				_credit = strtok.nextToken();

				int int_bal = Integer.parseInt(balance); System.out.println("Balance: " + int_bal);
				int int_credit = Integer.parseInt(_credit); System.out.println("Credit :"+int_credit);
				int int_amount = Integer.parseInt(Amount); System.out.println("Amount: " + int_amount);
				int new_balance = int_bal + int_amount;
				int new_credit = int_credit - int_amount;
				String Balance = Integer.toString(new_balance);
				String newCredit = Integer.toString(new_credit);

				String db_line = fname + "#" + famname + "#" + postcode + "#" + creditNo + "#" + Balance + "#" + newCredit;

				bufferedWriter = new BufferedWriter (new FileWriter(fileName));
				for(int k=0; k<j; k++){
					// System.out.println("Temp: "+temp[k]);
					bufferedWriter.write(temp[k]);
					bufferedWriter.write("\n");
				}
				bufferedWriter.close();

				bufferedWriter = new BufferedWriter( new FileWriter(fileName, true));
				bufferedWriter.write(db_line);
				bufferedWriter.close();		
		}
		catch(IOException e){
			System.out.println(e);
		}
	}

	public static int getPort(){
		int port = (int) (Math.random()*((65000-1025)+1))+1025;
		return port;
	}

	public static void main(String args[]) 
	{ 
		Bank bank = new Bank();
		String bank_port = args[0];
		int bankPort = Integer.parseInt(bank_port);
		boolean first = true;
		while(true){
			if(first){
				first = false;
				bank.runBank(bankPort);
			}
			
			else{
				bankPort = getPort();
				bank.runBank(bankPort);
			}

			}
		}
	} 
 
