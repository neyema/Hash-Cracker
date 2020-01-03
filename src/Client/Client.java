package Client;

import SharedData.Message;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class Client {
    private final InetAddress addressBroadcast = InetAddress.getByName("255.255.255.255");
    private final int serverPort = 3117;
    private final int bufferSize = 1024;
    private DatagramSocket socket;
    private String hash;  //hash length is 40 chars
    private byte originalLength;
    private List<InetAddress> addresses;  //all servers are on same port, so saving just this address
    private int waitForServers = 1000*10; //the time that the client will wait for acks

    public Client(String hash, byte originalLength) throws UnknownHostException {
        try {
            this.socket = new DatagramSocket(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.hash = hash;
        this.originalLength = originalLength;
    }

    public String run() throws IOException, InterruptedException {
        Message msg = new Message(Message.ourTeamName,Message.discover, hash, originalLength, "", "");
        socket.setBroadcast(true);
        byte[] broadcastBytes = msg.getBytes();
        DatagramPacket broadcastPacket = new DatagramPacket(broadcastBytes, broadcastBytes.length, addressBroadcast, serverPort);
        socket.send(broadcastPacket);
        //now waiting for offers from servers
        double startListeningTime = System.currentTimeMillis();
        List<DatagramPacket> packets = new LinkedList<>();
        byte[] buf = new byte[bufferSize];  //size of buffer of datagram packet (contains header&data)
        DatagramPacket receivedPacket = new DatagramPacket(buf, bufferSize);

        Thread t = new Thread(() -> { while(true) {
            try {
                socket.receive(receivedPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            packets.add(receivedPacket); }
        });
        t.join(1000);
        t.interrupt();
        for (DatagramPacket datagramPacket: packets){
            byte[] data = datagramPacket.getData();
            if ((int)data[33] == Message.offer) {
                addresses.add(datagramPacket.getAddress());
            }
        }
        socket.setBroadcast(false);

        //send requests to servers
        for (InetAddress address : addresses) {
            String start = "";
            String end = "";
            Message requestMsg = new Message(Message.ourTeamName, Message.request, hash, originalLength, start, end);
            byte[] requestBytes = requestMsg.getBytes();
            DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, address,serverPort);
            socket.send(request);
        }
        //wait for one ack/all nack
        String answer = "Not Found";
        boolean acked = false;
        while(!acked && !addresses.isEmpty()) {
            byte[] bufAck = new byte[bufferSize];
            DatagramPacket serverAnswer = new DatagramPacket(bufAck, bufferSize);
            socket.receive(serverAnswer);
            Message serverAnswerMsg = new Message(serverAnswer.getData());
            if (serverAnswerMsg.getType() == Message.acknowledge){
                answer = serverAnswerMsg.getOriginalStringStart();
                acked = true;
            }
        }
        return answer;
    }

    //function return jobs
    //jobs look like: result[0] till result[1] is the first range
    //                result[2] till result[3] is the second range
    //                and so on...
    public String [] divideToDomains (int stringLength, int numOfServers){
        String [] domains = new String[numOfServers * 2];

        StringBuilder first = new StringBuilder(); //aaa
        StringBuilder last = new StringBuilder(); //zzz

        for(int i = 0; i < stringLength; i++){
            first.append("a"); //aaa
            last.append("z"); //zzz
        }

        int total = convertStringToInt(last.toString());
        int perServer = (int) Math.floor (((double)total) /  ((double)numOfServers));

        domains[0] = first.toString(); //aaa
        domains[domains.length -1 ] = last.toString(); //zzz
        int summer = 0;

        for(int i = 1; i <= domains.length -2; i += 2){
            summer += perServer;
            domains[i] = converxtIntToString(summer, stringLength); //end domain of server
            summer++;
            domains[i + 1] = converxtIntToString(summer, stringLength); //start domain of next server
        }

        return domains;
    }

    private int convertStringToInt(String toConvert) {
        char[] charArray = toConvert.toCharArray();
        int num = 0;
        for(char c : charArray){
            if(c < 'a' || c > 'z'){
                throw new RuntimeException();
            }
            num *= 26;
            num += c - 'a';
        }
        return num;
    }

    private String converxtIntToString(int toConvert, int length) {
        StringBuilder s = new StringBuilder(length);
        while (toConvert > 0 ){
            int c = toConvert % 26;
            s.insert(0, (char) (c + 'a'));
            toConvert /= 26;
            length --;
        }
        while (length > 0){
            s.insert(0, 'a');
            length--;
        }
        return s.toString();
    }
}
