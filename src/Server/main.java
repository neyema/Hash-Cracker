package Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class main {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService serversThreadPool = new ScheduledThreadPoolExecutor(4);
        int numOfServers = 3;
        Thread[] serversThreads = new Thread[numOfServers];
        for (int i = 0; i < numOfServers; i++) {
            Server server = new Server();//creating a server
            serversThreads[i] = new Thread(server::run);//putting it in a thread
            serversThreadPool.execute(serversThreads[i]);//running that thread
        }
        for (int i = 0; i < numOfServers; i++) {
            serversThreads[i].join();//we wait for each server to stop its job(in other words, till some1 call Server.turnOffServer
        }
    }
}
