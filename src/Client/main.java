package Client;
import Server.Server;
import SharedData.Message;

import java.io.IOException;
import java.net.UnknownHostException;
import java. util. Scanner;

public class main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner in = new Scanner(System. in);
        System.out.println("Welcome to " + Message.ourTeamName +". Please enter the hash:");
        String hash = in.nextLine();
        while (hash.length()!=40) {
            System.out.println("hash illegal");
            System.out.println("Please enter hash of 40 chars:");
            hash = in.nextLine();
        }
        System.out.println("Please enter the input string length:");
        int length = in.nextInt();
        Client c = new Client(hash, (byte)length);
        String answer = c.run();
        System.out.println("The input string is "+answer);
    }

    public static void test() throws IOException, InterruptedException {
        Server server = new Server(); //create a server and running it
        Thread serverThread = new Thread(server::run);
        serverThread.start();
        Client c = new Client("9017347a610d1436c1aaf52764e6578e8fc1a083", (byte)5);
        String answer = c.run();
        server.turnOffServer();
        System.out.println("The input string is "+answer);
    }
}
