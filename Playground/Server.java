package Playground;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
  public static void main(String[] args) throws IOException {

    ServerSocket ss = new ServerSocket(3000);
    Socket s = ss.accept();

    System.out.println("Server running");
    System.out.println("IP & Local Port: " + s.getInetAddress() +" : "+ s.getLocalPort() );
    System.out.println("IP & Remote Port: "+ s.getInetAddress() + " : "+ s.getPort() );

    File f = new File("D:\\ISCTEM\\ENG Informatica\\3 Ano\\6 Semestre\\Sistemas Distribuidos\\Novo docente\\Playground\\Info.txt");
    byte[] buffer = new byte[(int) f.length()];
    FileInputStream fi = new FileInputStream(f);
    BufferedInputStream bi = new BufferedInputStream(fi);
    bi.read(buffer, 0, buffer.length);

    OutputStream os = s.getOutputStream();
    os.write(buffer, 0, buffer.length);

  }
}
