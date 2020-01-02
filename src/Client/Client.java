package Client;

import SharedData.Message;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class Client {
    private InetAddress addressBroadcast = InetAddress.getByName("255.255.255.255");
    private final int serverPort = 3117;
    private DatagramSocket socket;
    private char[] hash;  //hash length is 40 chars
    private byte originalLength;
    private List<InetAddress> addresses;

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
        Message msg = new Message((byte)1, hash, originalLength, "", "");
        socket.setBroadcast(true);
        byte[] buffer = msg.getBytes();
        DatagramPacket broadcastPacket = new DatagramPacket(buffer, buffer.length, addressBroadcast, serverPort);
        socket.send(broadcastPacket);
        //now waiting for offers from servers
        double startListeningTime = System.currentTimeMillis();
        List<DatagramPacket> packets = new LinkedList<>();
        byte[] buf = new byte[256];  //size of buffer of datagram packet (contains header&data)
        DatagramPacket receivedPacket = new DatagramPacket(buf, 256);

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
            if ((int)data[33] == Message.offer)
                addresses.add(datagramPacket.getAddress());
        }
        socket.setBroadcast(false);
        //code: send requests to servers and wait until ack/nack

        String answer = "";
        return answer;
    }
}
