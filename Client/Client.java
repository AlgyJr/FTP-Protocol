package Client;

import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        CommandLine cl = new CommandLine();
        cl.awaitCommand();
    }

}
