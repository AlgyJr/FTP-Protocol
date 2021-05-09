package Server;

import Server.Auth.Authentication;
import Server.rmi.Counter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

class Connection extends Thread {
    private Socket socket;
    private ServerSocket fileSocket;
    private PrintWriter pw;
    private Scanner sc;
    private CommandInterpreter ci;
    private Authentication auth;

    public Connection(Socket s, ServerSocket fileSocket, Counter c) throws IOException {
        this.socket = s;
        this.fileSocket = fileSocket;
        this.ci = new CommandInterpreter(this.socket, this.fileSocket, c);
        auth = new Authentication();
        this.start();
    }

    public void run() {
        // In case success on authentication
        if (this.ci.isAuthenticated(auth))
            this.ci.awaitCommand();
    }
}


public class Server {
    public static final int SERVER_PORT = 5000;
    public static final int SERVER_FILE_SHARING_PORT = 5050;

    public static void main(String[] args) throws IOException {
        Counter c = new Counter();
        LocateRegistry.createRegistry(1099).rebind("statistics", c);
        ServerSocket ss = new ServerSocket(5000);
        ServerSocket ss2 = new ServerSocket(5050);
        System.out.println("Server is running PORT:" + SERVER_PORT);
        System.out.println("Server Files Port Will Run On PORT: " + SERVER_FILE_SHARING_PORT);
        while(true) {
            new Connection(ss.accept(), ss2, c);
        }
    }
}
