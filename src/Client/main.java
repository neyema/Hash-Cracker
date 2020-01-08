package Client;
import Server.Server;
import SharedData.Message;

import java. util. Scanner;

public class main {
    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System. in);
        System.out.println("Welcome to " + Message.ourTeamName +". Please enter the hash:");
        String hash = in.nextLine();

        boolean legal = true;
        for(int i=0; i<hash.length(); i++)
            if (!(Character.isLetterOrDigit(hash.charAt(i))))
                legal = false;
        while (!legal) {
            System.out.println("hash illegal");
            System.out.println("Please enter hash of only a-z chars and digits:");
            hash = in.nextLine();
        }
        while (hash.length()!=40) {
            System.out.println("hash illegal");
            System.out.println("Please enter hash of 40 chars:");
            hash = in.nextLine();
        }
        System.out.println("Please enter the input string length:");
        int length = in.nextInt();
        while(length >= 256){
            System.out.println("length illegal");
            System.out.println("Please enter length < 256:");
            length = in.nextInt();
        }
        Client c = new Client(hash, (byte)length);
        String answer = c.run();
        System.out.println("The input string is "+answer);
    }

    public static void test() throws Exception {
        Server server = new Server(); //create a server and running it
        Thread serverThread = new Thread(server::run);
        serverThread.start();
        Client c = new Client("9017347a610d1436c1aaf52764e6578e8fc1a083", (byte)5);
        String answer = c.run();
        server.turnOffServer();
        System.out.println("The input string is "+answer);
    }
}
