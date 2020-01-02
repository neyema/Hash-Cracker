package Server;

import SharedData.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Server {
    private DatagramSocket socket;//this socket is for listening to messages
    private ExecutorService calculatingThreadPool = new ScheduledThreadPoolExecutor(4);

    public Server() {
        try {
            socket = new DatagramSocket(3117);//we listen on port 3117
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        byte[] buf = new byte[256];  //size of buffer of datagram packet (contains header&data)
        DatagramPacket receivedPacket = new DatagramPacket(buf, 256);

        while (true) {
            try {
                socket.receive(receivedPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            switch((int)receivedPacket.getData()[33]) {
                case Message.discover://if it's a discover, we send to the client an offer message
                    try {
                        sendOfferMessage(receivedPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case Message.request://if it's a request, we make a new thread that try to brute force
                    calculatingThreadPool.execute(new Thread(() -> bruteForce(receivedPacket)));
                    break;
                default:
            }
        }
    }

    //this function will send ack or nack if it found to the client either it found or not
    private void bruteForce(DatagramPacket requestPacket) {


    }

    //this function get the discover packet and send an offer message to the client
    private void sendOfferMessage(DatagramPacket discoverPacket) throws IOException {
        Message msg = new Message(discoverPacket.getData());
        msg.changeMessageType(Message.offer);
        byte[] buffer = msg.getBytes();
        DatagramPacket offerPacket = new DatagramPacket(buffer, buffer.length, discoverPacket.getAddress(), discoverPacket.getPort());
        socket.send(offerPacket);
    }

    public void sendAck(){

    }

    public void sendNack(){

    }
}
