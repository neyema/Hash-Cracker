package Client;

import SharedData.Message;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import static java.lang.Thread.interrupted;

public class Client {
    private final InetAddress addressBroadcast = InetAddress.getByName("255.255.255.255");
    private final InetAddress address = InetAddress.getByName("192.168.1.194");
    private final int serverPort = 3117;
    private final int bufferSize = 1024;
    private DatagramSocket socket;
    private String hash;  //hash length is 40 chars
    private byte originalLength;
    private List<InetAddress> addresses;  //all servers are on same port, so saving just this address
    private int waitForServers = 1000 * 60 ; //the time that the client will wait for acks
    private int timeToReceive = 1000 * 30;  //30 sec
    private String answer = "Not Found"; //cant be local variable, needs to be field because used inside lambda of thread

    public Client(String hash, byte originalLength) throws UnknownHostException {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.hash = hash;
        this.originalLength = originalLength;
        this.addresses = new LinkedList<>();
    }

    public String run() throws Exception {
        Message msg = new Message(Message.ourTeamName, Message.discover, hash, originalLength, "     ", "     ");
        //socket.setBroadcast(true);
        socket.setBroadcast(true);
        byte[] broadcastBytes = msg.getBytes();
        DatagramPacket broadcastPacket = new DatagramPacket(broadcastBytes, broadcastBytes.length, addressBroadcast, serverPort);
        //DatagramPacket broadcastPacket = new DatagramPacket(broadcastBytes, broadcastBytes.length, InetAddress.getLocalHost(), serverPort);
        //broadcastPacket.setAddress(InetAddress.getByAddress(new byte[] {(byte)255, (byte)255, (byte)255, (byte)255}));
        socket.send(broadcastPacket);
        System.out.println("sent broadcast");
        //now waiting for offers from servers
        List<DatagramPacket> packets = new LinkedList<>();
        try {
            socket.setSoTimeout(timeToReceive);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Thread t = new Thread(() -> {
            while (!interrupted()) {
                byte[] buf = new byte[bufferSize];  //size of buffer of datagram packet (contains header&data)
                DatagramPacket receivedPacket = new DatagramPacket(buf, bufferSize);
                try {
                    socket.receive(receivedPacket);
                    System.out.println("received packet from: " + receivedPacket.getAddress().toString() +", port: "+receivedPacket.getPort());
                    packets.add(receivedPacket);
                } catch (SocketTimeoutException e){

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        t.join(timeToReceive);
        t.interrupt();
        for (DatagramPacket datagramPacket : packets) {
            byte[] data = datagramPacket.getData();
            if (data[32] == Message.offer) {
                addresses.add(datagramPacket.getAddress());
            }
        }
        //socket.setBroadcast(false);
        //send requests to servers
        //so when it tries to listen, it gets stuck
        //seems like a dirty and ugly way to fix so need to see why it happens
        if (addresses.size() == 0)
            return answer;
        String[] stringsToCheck = divideToDomains(originalLength & 0xff, addresses.size());
        for (int i = 0; i < addresses.size(); i++) {
            InetAddress address = addresses.get(i);
            String start = stringsToCheck[2*i];
            String end = stringsToCheck[2*i+1];
            Message requestMsg = new Message(Message.ourTeamName, Message.request, hash, originalLength, start, end);
            byte[] requestBytes = requestMsg.getBytes();
            DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, address, serverPort);
            socket.send(request);
            System.out.println("sent request to: " + request.getAddress().toString() +", port: "+request.getPort() );
        }
        socket.setSoTimeout(waitForServers);
        //wait for one ack/all nack
        Thread waitForServersAcks = new Thread(() ->
        {
            int nacks = 0;
            while (!interrupted() && nacks < addresses.size()) {
                byte[] bufAck = new byte[bufferSize];
                DatagramPacket serverAnswer = new DatagramPacket(bufAck, bufferSize);
                try {
                    socket.receive(serverAnswer);
                    Message serverAnswerMsg = new Message(serverAnswer.getData());
                    byte serverAnswerMsgType = serverAnswerMsg.getType();
                    if (serverAnswerMsgType == Message.acknowledge) {
                        answer = serverAnswerMsg.getOriginalStringStart();  //contains the original input string
                        break;
                    }
                    if (serverAnswerMsgType == Message.negative_acknowledge) {
                        nacks++;
                    }
                }
                catch (SocketTimeoutException e){
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        waitForServersAcks.start();
        waitForServersAcks.join(waitForServers);
        waitForServersAcks.interrupt();  //time is over
        return answer;
    }

    //function return jobs
    //jobs look like: result[0] till result[1] is the first range
    //                result[2] till result[3] is the second range
    //                and so on...
    private String[] divideToDomains(int stringLength, int numOfServers) {
        String[] domains = new String[numOfServers * 2];

        StringBuilder first = new StringBuilder(); //aaa
        StringBuilder last = new StringBuilder(); //zzz

        for (int i = 0; i < stringLength; i++) {
            first.append("a"); //aaa
            last.append("z"); //zzz
        }

        BigDecimal total = convertStringToInt(last.toString());
        BigDecimal perServer = total.divide(new BigDecimal(numOfServers), 0, RoundingMode.FLOOR);

        domains[0] = first.toString(); //aaa
        domains[domains.length - 1] = last.toString(); //zzz
        BigDecimal summer = new BigDecimal(0);

        for (int i = 1; i <= domains.length - 2; i += 2) {
            summer = summer.add(perServer);
            domains[i] = converxtIntToString(summer.toBigInteger(), stringLength); //end domain of server
            summer = summer.add(new BigDecimal(1));
            domains[i + 1] = converxtIntToString(summer.toBigInteger(), stringLength); //start domain of next server
        }
        return domains;
    }

    private BigDecimal convertStringToInt(String toConvert) {
        char[] charArray = toConvert.toCharArray();
        BigDecimal num = new BigDecimal(0);
        for (char c : charArray) {
            if (c < 'a' || c > 'z') {
                throw new RuntimeException();
            }
            num = num.multiply(new BigDecimal(26));
            num = num.add(new BigDecimal((c - 'a')));
        }
        return num;
    }

    private String converxtIntToString(BigInteger toConvert, int length) {
        StringBuilder s = new StringBuilder(length);
        while (toConvert.signum() == 1) {//if it's positive it means it's bigger than zero
            int c = toConvert.mod(new BigInteger("26")).intValue();
            s.insert(0, (char) (c + 'a'));
            toConvert = toConvert.divide(new BigInteger("26"));
            length--;
        }
        while (length > 0) {
            s.insert(0, 'a');
            length--;
        }
        return s.toString();
    }
}
