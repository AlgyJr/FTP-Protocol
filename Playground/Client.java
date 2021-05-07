package Playground;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class Client {
  public static void main(String[] args) throws IOException {
    Socket s = new Socket("localhost", 3005);

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

  /*
    ts --- 100%
    rc --- x

  x = (rc * 100%) / tc
   */

  private static void download(Socket s) throws IOException {

    DataInputStream is = new DataInputStream(s.getInputStream());
    String fileName = is.readUTF();

    FileOutputStream fo = new FileOutputStream(System.getProperty("user.dir") + "/FileSystem/ClientRoot/" +  fileName);
    long fileSize = Long.parseLong(is.readUTF());

    double readChunk = 0;
    double percentage;

    byte[] bytes;
    while (!((bytes = is.readNBytes(25)).length == 0)) {
      readChunk += bytes.length;
      percentage = (readChunk / fileSize) * 100;
      System.out.print("\r" + percentage + "%\r");
      fo.write(bytes);
    }
    fo.close();
  }

}
