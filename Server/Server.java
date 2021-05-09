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

//class FileSharingThread extends Thread {
//
//    private Socket socket;
//
//    public FileSharingThread(Socket socket) {
//        this.socket = socket;
//        this.start();
//    }
//
//    public void run() {
//        try {
//            System.out.println("Server Started!");
//            Scanner is = new Scanner(this.socket.getInputStream());
//            PrintWriter pw = new PrintWriter(this.socket.getOutputStream());
//
//            System.out.println(is.nextLine()); //::>> Prints : "Sending File"
//            pw.println("File Has Been Read");
//            pw.flush();
//
//            is.close();
//            pw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}



public class Server {
    public static void main(String[] args) throws IOException {
        Counter c = new Counter();
        LocateRegistry.createRegistry(1099).rebind("statistics", c);
        ServerSocket ss = new ServerSocket(5000);
        ServerSocket ss2 = new ServerSocket(5050);
        System.out.println("The Server is on...");
        while(true) {
            new Connection(ss.accept(), ss2, c);
        }
    }
}
