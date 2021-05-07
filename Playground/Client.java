package Playground;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class Client {
  public static void main(String[] args) throws IOException {
    Socket s = new Socket("localhost", 3000);

    OutputStream os = s.getOutputStream();
    PrintWriter pw = new PrintWriter(os);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    System.out.println("Enter the file to be downloaded: ");
    String file;
    while((file = br.readLine()) !=  null) {
      pw.println(file);
      pw.flush();
      download(s);
    }

  }

  private static void download(Socket s) throws IOException {
    FileOutputStream fo = new FileOutputStream(new Date().getTime()+"_Received.txt");
    InputStream is = s.getInputStream();
    System.out.println(is);
    int bytes = 0;
    while ((bytes = is.read()) != -1) {
      fo.write(bytes);
    }
    fo.close();
  }
}
