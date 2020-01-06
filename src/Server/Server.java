package Server;

import SharedData.Message;
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Server {
    private DatagramSocket listeningSocket;
    private DatagramSocket sendingSocket;
    private ExecutorService calculatingThreadPool = new ScheduledThreadPoolExecutor(4);
    private Mutex mutex;
    private boolean serverRunning;
    private Mutex mutexForRunFunctionToWait;//this mutex is to avoid busy waiting
    private long bruteForceRuntime = 1000*30;  //10 sec for Yuval
    private int serverRunTime = 1000 * 60 * 10;  //10 min for Yuval

    public Server() {
        try {
            listeningSocket = new DatagramSocket(3117);//we listen on port 3117
            sendingSocket = new DatagramSocket(0);//we don't mind sending on any socket
        } catch (SocketException e) {
            e.printStackTrace();
        }
        mutex = new Mutex();
        mutexForRunFunctionToWait = new Mutex();
        serverRunning = true;
    }

    public void run() {
        try {
            mutexForRunFunctionToWait.acquire();//we acquire first to say that the server is running
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            listeningSocket.setSoTimeout(serverRunTime);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Thread t = new Thread(() -> {
            while (serverRunning) {
                byte[] buf = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(buf, 1024);
                try {
                    listeningSocket.receive(receivedPacket);
                }
                catch (SocketTimeoutException e) {
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                switch (receivedPacket.getData()[32]) {
                    case Message.discover://if it's a discover, we send to the client an offer message
                        sendOfferMessage(receivedPacket);
                        break;
                    case Message.request://if it's a request, we make a new thread that try to brute force
                        calculatingThreadPool.execute(new Thread(() -> bruteForce(receivedPacket)));
                        break;
                    default://if it's something else we do nothing.
                }
            }
        });
        t.start();
        try {
            mutexForRunFunctionToWait.acquire();//if we could acquire the mutex that means that the server need to shut down and we continue to next line to shut down it
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t.interrupt();//we shut down the server
    }

    //function will send ack or nack if it found the HashKey to the client either it found or didn't
    private void bruteForce(DatagramPacket requestPacket) {
        Message msg = new Message(requestPacket.getData());
        String result = tryDeHash(msg.getOriginalStringStart(), msg.getOriginalStringEnd(),msg.getHash());
        if (!result.equals("")) {//means that we found the HashKey
            sendAck(requestPacket, result);
        } else {
            sendNack(requestPacket);  //send nack also in case runtime is over, because client is waiting for nack
        }
    }

    //this function get the discover packet and send an offer message to the client
    private void sendOfferMessage(DatagramPacket discoverPacket) {
        Message msg = new Message(discoverPacket.getData());
        msg.setType(Message.offer);
        sendMessage(msg, discoverPacket);
    }

    //the originalStringStart need to have the foundHashKey
    private void sendAck(DatagramPacket requstPacket, String foundHashKey) {
        Message msg = new Message(requstPacket.getData());
        msg.setType(Message.acknowledge);
        msg.setOriginalStringStart(foundHashKey);
        sendMessage(msg, requstPacket);
    }

    private void sendNack(DatagramPacket requstPacket) {
        Message msg = new Message(requstPacket.getData());
        msg.setType(Message.negative_acknowledge);
        sendMessage(msg, requstPacket);
    }

    //function gets a range to find a hash key.
    //will return a string with the current key or will return empty string if not found
    private String tryDeHash(String startRange, String endRange, String originalHash){
        long startTime = System.currentTimeMillis();
        for(String currentString = startRange; currentString.compareTo(endRange) <= 0 && !currentString.equals(""); currentString = incrementString(currentString)){
            long currentTime = System.currentTimeMillis();
            if (currentTime-startTime > bruteForceRuntime)
                break;
            String hash = hash(currentString);
            if(originalHash.equals(hash)){
                return currentString;
            }
        }
        return "";
    }

    //function gets a string and hashing it to SHA-1
    private String hash(String toHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(toHash.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return hashText.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //function handles that 2 people won't send at the same time from the socket
    private void sendPacket(DatagramPacket packet) {
        try {
            mutex.acquire();
            sendingSocket.send(packet);
            mutex.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Message msg, DatagramPacket packet) {
        msg.setTeamName(Message.ourTeamName);
        byte[] buffer = msg.getBytes();
        DatagramPacket offerPacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
        sendPacket(offerPacket);
    }

    private String incrementString(String str){
        StringBuilder strBuilder = new StringBuilder(str);
        for (int i = str.length() - 1; i >= 0; i--) {
            if(str.charAt(i) != 'z'){
                strBuilder.setCharAt(i,(char)(str.charAt(i) + 1));
                return strBuilder.toString();
            }
            else{
                strBuilder.setCharAt(i,'a');
            }
        }
        return "";
    }

    public void turnOffServer() {
        mutex.release();
    }
}
