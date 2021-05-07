package Client;

import Shared.Commands;
import Shared.PathResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 5000);
        Scanner sc = new Scanner(socket.getInputStream());
        PrintWriter pw = new PrintWriter(socket.getOutputStream());

        //::>> Get Server's cwd
        pw.println("pwd");
        pw.flush();
        String cwd = sc.nextLine();
        sc.nextLine(); //::>> Dispose of the result;

        String result, command;
        CommandIntepreter ci = new CommandIntepreter();


        Scanner input = new Scanner(System.in);
        while (true) {
            if(!ci.isOnServer) {
                //ci.awaitCommand();
                System.out.print(ci.getFs() + PathResolver.getRelPathString(ci.getPathNames()) + "$ ");
                command = input.nextLine();
                result = ci.intepretCommand(command).replaceAll("0-0", "\n");
                System.out.println(result);
            } else {
                System.out.print(cwd);
                command = input.nextLine();

                pw.println(command);
                pw.flush();

                cwd = sc.nextLine();
                result = sc.nextLine().replaceAll("0-0", "\n");
                System.out.println(result);

                if(command.toUpperCase().equals(Commands.MVC))
                    ci.isOnServer = false;
            }
        }

    }

}
