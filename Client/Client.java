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

        String result, fullCommand;
        CommandIntepreter ci = new CommandIntepreter(socket,sc, pw);


        Scanner input = new Scanner(System.in);
        while (true) {
            if(!ci.isOnServer) {
                System.out.print(ci.getFs() + PathResolver.getRelPathString(ci.getPathNames()) + "$ ");
                fullCommand = input.nextLine();
                String [] commandNDoptions = fullCommand.split(" ");

                result = ci.intepretCommand(fullCommand).replaceAll("0-0", "\n");
                System.out.println(result);

                if(commandNDoptions[0].toUpperCase().equals(Commands.GET)) {
                    result = sc.nextLine();
                    result = sc.nextLine();
                    System.out.println(result);
                }
                if(commandNDoptions[0].toUpperCase().equals(Commands.PUT)) {
                    System.out.println(result);
                    cwd = sc.nextLine();
                    result = sc.nextLine();
                    System.out.println(result);
                }

            } else {
                System.out.print(cwd);
                fullCommand = input.nextLine();
                String [] commandNDoptions = fullCommand.split(" ");

                if(commandNDoptions[0].toUpperCase().equals(Commands.GET)) {
                    result = ci.intepretCommand(fullCommand).replaceAll("0-0", "\n");
                    System.out.println(result);
                    cwd = sc.nextLine();
                    sc.nextLine();
                    continue;
                }

                if(commandNDoptions[0].toUpperCase().equals(Commands.PUT)) {
                    ci.intepretCommand(fullCommand).replaceAll("0-0", "\n");
                    cwd = sc.nextLine();
                    result = sc.nextLine();
                    System.out.println(result);
                    continue;
                }

                pw.println(fullCommand);
                pw.flush();


                cwd = sc.nextLine();
                result = sc.nextLine().replaceAll("0-0", "\n");
                System.out.println(result);


                if(commandNDoptions[0].toUpperCase().equals(Commands.MVC))
                    ci.isOnServer = false;
            }
        }

    }

}
