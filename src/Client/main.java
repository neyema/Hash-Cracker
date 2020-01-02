package Client;
import java.io.IOException;
import java.net.SocketException;
import java. util. Scanner;

public class main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner in = new Scanner(System. in);
        System.out.println("Welcome to <our-team-name>. Please enter the hash:");
        String hash = in.nextLine();
        System.out.println("Please enter the input string length:");
        int length = in.nextInt();
        Client c = new Client(hash, (byte)length);
        String answer = c.run();
        System.out.println("The input string is "+answer);
    }
}
