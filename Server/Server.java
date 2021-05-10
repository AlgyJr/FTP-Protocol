package Server;

import Server.Auth.Authentication;
import Server.rmi.Counter;
import Shared.Constants;

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

    public Connection(Socket s, ServerSocket fileSocket, Counter c)  {
        try {
            System.out.println(":::: NEW CONNECTION");
            System.out.println("IP & Local Port: " + s.getInetAddress() +" : "+ s.getLocalPort() );
            System.out.println("IP & Remote Port: "+ s.getInetAddress() + " : "+ s.getPort() );
            System.out.println("::::::::::::::::::::\n\n");
            this.socket = s;
            this.fileSocket = fileSocket;
            this.ci = new CommandInterpreter(this.socket, this.fileSocket, c);
            auth = new Authentication();
            this.start();
        } catch (IOException e) {
            System.out.println(":::: COULD NOT OPEN CONNECTION");
            System.out.println("IP: "+ s.getInetAddress() + " Port: "+ s.getPort() );
            System.out.println("::::::::::::::::::::\n\n");
        }

    }


    public void run() {
        // In case success on authentication
        if (this.ci.isAuthenticated(auth)) {
            this.ci.setFs("[" + Constants.SERVER_SIDE.name() + "]~"+ auth.getUsername() + "@hostname:/");
            this.ci.awaitCommand();
        }
    }
}


public class Server {
    public static final int SERVER_PORT = 5000;
    public static final int SERVER_FILE_SHARING_PORT = 5050;

    public static void main(String[] args) {
        try {
            Counter c = new Counter();
            LocateRegistry.createRegistry(1099).rebind("statistics", c);
            ServerSocket ss = new ServerSocket(SERVER_PORT);
            ServerSocket ss2 = new ServerSocket(SERVER_FILE_SHARING_PORT);
            System.out.println("Server is running PORT:" + SERVER_PORT);
            System.out.println("Server Files Port Will Run On PORT: " + SERVER_FILE_SHARING_PORT);
            while (true) {
                new Connection(ss.accept(), ss2, c);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("::::  SERVER CLOSED UNEXPECTEDLY");
            System.out.println("::::::::::::::::::::\n\n");
        }
    }
}
