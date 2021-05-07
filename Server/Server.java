package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

class Connection extends Thread {
    private Socket socket;
    private PrintWriter pw;
    private Scanner sc;
    private CommandInterpreter ci;

    public Connection(Socket s) throws IOException {
        this.socket = s;
        this.pw = new PrintWriter(this.socket.getOutputStream());
        this.sc = new Scanner(this.socket.getInputStream());
        this.ci = new CommandInterpreter(sc, pw);
        this.start();
    }

    public void run() {
        this.ci.awaitCommand();
    }
}

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(5000);
        while(true) {
            new Connection(ss.accept());
        }
    }
}
