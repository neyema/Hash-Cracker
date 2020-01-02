package Client;

import SharedData.Message;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class Client {
    private final InetAddress addressBroadcast = InetAddress.getByName("255.255.255.255");
    private final int serverPort = 3117;
    private final int bufferSize = 256;
    private final String teamName = "       <insert-team-name>       ";
    private DatagramSocket socket;
    private char[] hash;  //hash length is 40 chars
    private byte originalLength;
    private List<InetAddress> addresses;  //all servers are on same port, so saving just this address
    private int waitForServers = 1000*10; //the time that the client will wait for acks

    public Client(String hash, byte originalLength) throws UnknownHostException {
        try {
            this.socket = new DatagramSocket(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.hash = hash.toCharArray();
        this.originalLength = originalLength;
    }

    public String run() throws IOException, InterruptedException {
        Message msg = new Message(teamName,Message.discover, hash, originalLength, "", "");
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
            Message requestMsg = new Message(teamName, Message.request, hash, originalLength, start, end);
            byte[] requestBytes = requestMsg.getBytes();
            DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, address,serverPort);
            socket.send(request);
        }
        //wait for one ack/all nack
        boolean acked = false;
        while(true && !acked && !addresses.isEmpty()) {
            byte[] bufAck = new byte[bufferSize];
            DatagramPacket serverAnswer = new DatagramPacket(buf, bufferSize);
            socket.receive(serverAnswer);
            Message serverAnswerMsg = new Message(serverAnswer.getData());
            if (serverAnswerMsg.getType() == Message.acknowledge)
                acked = true;
        }
        String answer = "";
        return answer;
    }
}
