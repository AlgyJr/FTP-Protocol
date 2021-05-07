package Playground;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

class ServerThread extends Thread {
  private Socket s;
  public ServerThread (Socket s) {
    this.s = s;
  }
  @Override
  public void run() {
    super.run();
    try {

      InputStream is = s.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String file;
      file = br.readLine();
      System.out.println(file);
      upload(s, file);
//      while(!(file = br.readLine()).equals("quit")) {
//        System.out.println(file);
//        upload(s, file);
//      }
      s.close();
    }catch (IOException oi) {
      oi.printStackTrace();
    }
  }

  private void upload(Socket s, String file) throws IOException {

    File fileObj = new File(file);
    DataOutputStream os = new DataOutputStream(s.getOutputStream());

    //OutputStream os = s.getOutputStream();
    FileInputStream fi = new FileInputStream(fileObj);
    long fileSize = fileObj.length();

    //::>> Send File Size and File Name
    os.writeUTF(fileObj.getName());
    os.flush();
    os.writeUTF(fileSize + "");
    os.flush();

    int chnkSize = 25;
    byte[] byteArray = new byte[chnkSize];
    int bytes;
    while ((bytes = fi.read(byteArray, 0, chnkSize)) != -1) {
      System.out.println(bytes);
      os.write(byteArray);
      os.flush();
    }
    os.close();
    System.out.println("File uploaded...");
  }
}

public class Server {
  public static void main(String[] args) throws IOException {

    ServerSocket ss = new ServerSocket(3005);
    while(true) {
      Socket s = ss.accept();
      System.out.println("Server running");
      System.out.println("IP & Local Port: " + s.getInetAddress() +" : "+ s.getLocalPort() );
      System.out.println("IP & Remote Port: "+ s.getInetAddress() + " : "+ s.getPort() );
      System.out.println("<PRINT THE LIST OF FILES>");
      new ServerThread(s).start();
    }
  }
}
