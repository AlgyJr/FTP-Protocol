package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

class Connection extends Thread {
    private Socket socket;
    private ServerSocket fileSocket;
    private PrintWriter pw;
    private Scanner sc;
    private CommandInterpreter ci;

    public Connection(Socket s, ServerSocket fileSocket) throws IOException {
        this.socket = s;
        this.fileSocket = fileSocket;
        this.ci = new CommandInterpreter(this.socket, this.fileSocket);
        this.start();
    }

    public void run() {
        this.ci.awaitCommand();
    }
}


public class Server {
    public static final int SERVER_PORT = 5000;
    public static final int SERVER_FILE_SHARING_PORT = 5050;

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(SERVER_PORT);
        ServerSocket ss2 = new ServerSocket(SERVER_FILE_SHARING_PORT);
        System.out.println("Server is running PORT:" + SERVER_PORT);
        System.out.println("Server Files Port Will Run On PORT: " + SERVER_FILE_SHARING_PORT);
        while(true) {
            new Connection(ss.accept(), ss2);
        }
    }
}
