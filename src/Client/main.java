package Client;
import SharedData.Message;

import java.io.IOException;
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
}
