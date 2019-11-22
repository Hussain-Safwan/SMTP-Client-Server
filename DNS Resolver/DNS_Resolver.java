import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;

class Query{

	//Class to send query and recieve response

	DatagramSocket socket;
	String domain;
	int serverNo;
	final int Port = 53; 
	String[] RootServers;


	public Query(DatagramSocket skt, String dom, int ser_no){
		socket = skt;
		domain = dom;
		serverNo = ser_no;
		init();

	}
    
    void init(){						
    	//Initializes 13 root servers
    	RootServers = new String[15];
        RootServers[0] = "198.41.0.4";
        RootServers[1] = "199.9.14.201";
        RootServers[2] = "192.33.4.12";
        RootServers[3] = "199.7.91.13[";
        RootServers[4] = "192.203.230.10";
        RootServers[5] = "192.5.5.241";
        RootServers[6] = "192.112.36.4";
        RootServers[7] = "198.97.190.53";
        RootServers[8] = "192.36.148.17";
        RootServers[9] = "192.58.128.30";
        RootServers[10] = "193.0.14.129";
        RootServers[11] = "199.7.83.42";
        RootServers[12] = "202.12.27.33";
    }

    void sendRequest() throws IOException {
    	//Sends request to a certain root server
        InetAddress ipAddress = InetAddress.getByName(RootServers[serverNo]);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        dataOutputStream.writeShort(0x1234);			
        dataOutputStream.writeShort(0x0000);			
        dataOutputStream.writeShort(0x0001);			
        dataOutputStream.writeShort(0x0000);			
        dataOutputStream.writeShort(0x0000);			
        dataOutputStream.writeShort(0x0000);			

        String[] domainParts = domain.split("\\.");	//Splits the given domian wrt '.'
        for (int i = 0; i < domainParts.length; i++) {
            byte[] domainBytes = domainParts[i].getBytes("UTF-8");
            dataOutputStream.writeByte(domainBytes.length);
            dataOutputStream.write(domainBytes);
        }

        dataOutputStream.writeByte(0x00);			

        dataOutputStream.writeShort(0x0001);			
        dataOutputStream.writeShort(0x0001);			

        byte[] dnsFrameByteArray = byteArrayOutputStream.toByteArray();
        DatagramPacket datagramPacket = new DatagramPacket(dnsFrameByteArray, dnsFrameByteArray.length,
                ipAddress, Port);
        socket.send(datagramPacket);	//Sends the request to obtained IP address
    }

    void sendRequest(String rootServer) throws IOException {
    	//Sends request to a predefined root server
        InetAddress ipAddress = InetAddress.getByName(rootServer);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        //Constructing the query
        dataOutputStream.writeShort(0x1234);			
        dataOutputStream.writeShort(0x0000);			
        dataOutputStream.writeShort(0x0001);			
        dataOutputStream.writeShort(0x0000);			
        dataOutputStream.writeShort(0x0000);			
        dataOutputStream.writeShort(0x0000);			

        String[] domainParts = domain.split("\\.");		//Splits given domian wrt '.'
        for (int i = 0; i < domainParts.length; i++) {
            byte[] domainBytes = domainParts[i].getBytes("UTF-8");
            dataOutputStream.writeByte(domainBytes.length);
            dataOutputStream.write(domainBytes);
        }

        dataOutputStream.writeByte(0x00);			

        dataOutputStream.writeShort(0x0001);			
        dataOutputStream.writeShort(0x0001);			

        byte[] dnsFrameByteArray = byteArrayOutputStream.toByteArray();
        DatagramPacket datagramPacket = new DatagramPacket(dnsFrameByteArray, dnsFrameByteArray.length, ipAddress, Port);
        socket.send(datagramPacket);	//Send the request to obtained IP address
    }

    DataInputStream getResponse() throws IOException {
    	//Gets response from the server
        byte[] byteArray = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(byteArray, byteArray.length);
        socket.receive(datagramPacket);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        return dataInputStream;
    }
}

class Answer{

	//Class to analyse the response

	boolean ipFound = false, cnameFound=false, hostnameFound=false;
    String cnameIP = "";
    ArrayList<String> ipArrayList = new ArrayList<>();
    ArrayList<String> extraArrayList = new ArrayList<>();

	int[] getHeader(DataInputStream dataInputStream) throws IOException {
		//Analyses the Header section
        dataInputStream.readShort();	//Reads ID
        dataInputStream.readShort();	//Reads flags
        dataInputStream.readShort();	//Reads Questions
        short answers = dataInputStream.readShort();
        short authoritative = dataInputStream.readShort();
        short additional = dataInputStream.readShort();

        String hostName = getHostByName(dataInputStream);
        dataInputStream.readInt();

        int[] output = new int[3];
        output[0] = (int)answers;
        output[1] = (int)authoritative;
        output[2] = (int)additional;
        return output;
    }

    String getHostByName(DataInputStream dataInputStream) throws IOException {
        int partLen;
        StringBuffer stringBuffer = new StringBuffer();
        while ( (partLen = dataInputStream.readByte()) > 0 ) {
        	 byte[] hostName = new byte[partLen];
            for (int i=0; i<partLen; i++) {
            	hostName[i] = dataInputStream.readByte();
            }
            String aliasHostName = new String(hostName, "UTF-8");
            if (stringBuffer.length() > 0) {
            	stringBuffer.append(".");
            }
            stringBuffer.append(aliasHostName);
        }
        return stringBuffer.toString();
    }

   	String getDNSAnswers(DataInputStream dataInputStream) throws IOException {
   		//Analyses the Answer section
        dataInputStream.readShort();
        short hexCode = dataInputStream.readShort();
        dataInputStream.readShort();
        dataInputStream.readInt();
        short addressLength = dataInputStream.readShort();
        byte[] address = new byte[addressLength];
        dataInputStream.read(address, 0, addressLength);

        Converter converter = new Converter(address, addressLength);

        try {
            if (hexCode == 0x0001 || hexCode == 0x0028) {
                if (!ipFound) {	//Checks for IP returned address
                    ipArrayList.add(converter.convert());	///Adds to the list
                    ipFound = true;
                    return ipArrayList.get(ipArrayList.size() - 1);
                } else {
                    ipArrayList.add(converter.convert());
                }
            } else if (hexCode == 0x0005) {
                if (!cnameFound) {	//Checks for returned cname
                    cnameIP = converter.convert();
                    cnameFound = true;
                    return cnameIP;
                } else
                    return converter.convert();
            }
            else if(hexCode == 0x0006) {
                hostnameFound = true;
                return null;
            }
            else
                return null;
        }
        catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    void analyseAnswers(DataInputStream dataInputStream) throws IOException{
        int[] outputs = getHeader(dataInputStream);
        int answers = outputs[0], authoritative = outputs[1], additional = outputs[2];
        for (int i=0; i<answers; i++) {
        	getDNSAnswers(dataInputStream);
        }
        for (int i=0; i<authoritative; i++) {
        	getDNSAnswers(dataInputStream);
        }
        for (int i=0; i<additional; i++) {
        	getDNSAnswers(dataInputStream);
        }
    }

    boolean isAnswerFound(DataInputStream dataInputStream) throws IOException{
        int[] outputs = getHeader(dataInputStream);
        int answers = outputs[0], authoritative = outputs[1],additional = outputs[2];
        for (int i = 0; i < answers; i++) {
        	getDNSAnswers(dataInputStream);
        }
        if(hostnameFound){
            System.out.println("Alias hostname not found");
            return true;
        }
        for (int i = 0; i < authoritative; i++) {
        	getDNSAnswers(dataInputStream);
        }
        if(hostnameFound){
            System.out.println("Alias hostname not found");
            return true;
        }
        for (int i = 0; i < additional; i++) {
        	getDNSAnswers(dataInputStream);
        }
        if(hostnameFound){
            System.out.println("Alias hostname not found");
            return true;
        }
        return false;
    }
}

class Converter{

	//Class to convert IP address to string 

	byte[] address;
	short addrLen;

	public Converter(byte[] Address, short AddrLen){
		address = Address;
		addrLen = AddrLen;
	}

	String convert() {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < addrLen; i++ ) {
            if (i != 0) stringBuffer.append(".");
            stringBuffer.append( String.format("%d", (address[i] & 0xFF)) );
        }
        String output = stringBuffer.toString();
        return output;
    }

}

public class DNS_Resolver{
    private static String domain = "";
    static Scanner scanner  = new Scanner(System.in);

public static void main(String[] args) throws IOException {
        System.out.print("Enter the domain  name to be resolved: ");
        domain = scanner.next();
        DatagramSocket socket = new DatagramSocket();

        Query query = new Query(socket, domain, 8);

        query.sendRequest();
        DataInputStream dataInputStream = query.getResponse();
        Answer ans = new Answer();
        ans.analyseAnswers(dataInputStream);
        if(ans.hostnameFound){
            System.out.println("Domain doesnt exist");
            return;
        }
        else if(ans.ipFound) {
        	//IP address found, now added to extra lsit to be queried again
            ans.ipFound = false;
            ans.extraArrayList.clear();
            for(int i=0;i<ans.ipArrayList.size();i++){
                ans.extraArrayList.add(ans.ipArrayList.get(i));	//Queries the returned IP address
            }
            ans.ipArrayList.clear();
            for(int j=0;j<ans.extraArrayList.size();j++) {
                query.sendRequest(ans.extraArrayList.get(j));
                dataInputStream = query.getResponse();
                ans.analyseAnswers(dataInputStream);
                if(ans.ipFound) break;
            }
        }
        else {
            if(ans.cnameFound){
                while(true){
                    if(ans.cnameFound){
                        ans.cnameFound=false;
                        query.sendRequest(ans.cnameIP);
                        dataInputStream = query.getResponse();
                        ans.analyseAnswers(dataInputStream);
                    }
                    else if(ans.ipFound) {
                        if (ans.ipArrayList.size() != 0) System.out.println( domain + " = " + ans.ipArrayList.get(0));
                        else System.out.println("DNS Error");
                        return ;
                    }
                    else break;

                }
            }
            else System.out.println("DNS Error");
            return ;
        }

        if(ans.hostnameFound){
            System.out.println("Domain doesn't exist");
            return;
        }
        if(ans.ipFound) {
            ans.ipFound = false;
            ans.extraArrayList.clear();
            for(int i=0;i<ans.ipArrayList.size();i++){
                ans.extraArrayList.add(ans.ipArrayList.get(i));
            }
            ans.ipArrayList.clear();
            for(int j=0;j<ans.extraArrayList.size();j++) {
                query.sendRequest(ans.extraArrayList.get(j));
                dataInputStream = query.getResponse();
                boolean ishostnameFoundTrue = ans.isAnswerFound(dataInputStream);
                if(ishostnameFoundTrue) {
                	return;
                }
                if(ans.ipFound){
                 break;
             }
            }
        }
        else{
            if(ans.cnameFound){
                while(true){
                    if(ans.cnameFound){
                        ans.cnameFound=false;
                        query.sendRequest(ans.cnameIP);
                        dataInputStream = query.getResponse();
                        boolean ishostnameFoundTrue = ans.isAnswerFound(dataInputStream);
                        if(ishostnameFoundTrue) return;
                    }
                    else if(ans.ipFound) {
                        if (ans.ipArrayList.size() != 0) System.out.println( domain + " = " + ans.ipArrayList.get(0));
                        else System.out.println("DNS Error Occurred");
                        return ;
                    }
                    else break;

                }
            }
            else System.out.println("DNS Error Occurred");
            return ;
        }
        if(ans.ipArrayList.size()!=0)
            System.out.println(domain+" = "+ans.ipArrayList.get(0));
        else
            System.out.println("DNS Error Occurred");
    }

}