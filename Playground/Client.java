package Playground;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Client {
  public static void main(String[] args) throws IOException {
    Socket s = new Socket("localhost", 3000);

    byte[] buffer = new byte[1000];

    InputStream is = s.getInputStream();

    FileOutputStream fo = new FileOutputStream("D:\\ISCTEM\\ENG Informatica\\3 Ano\\6 Semestre\\Sistemas Distribuidos\\Novo docente\\Playground\\Info_output.txt");
    BufferedOutputStream bo = new BufferedOutputStream(fo);

    int howmany = is.read(buffer, 0, buffer.length);
    int i = howmany;

    while(howmany > -1) {
      howmany = is.read(buffer, i, (buffer.length - i));
      System.out.println(howmany);
      if (howmany >= 0)
        i += howmany;
    }

    bo.write(buffer, 0, i);
    bo.flush();
  }
}
