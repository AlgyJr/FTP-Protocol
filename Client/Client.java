package Client;

import Shared.Commands;
import Shared.InterfaceCounter;
import Shared.PathResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;



public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    //197.249.10.243
    public static void main(String[] args) throws IOException, MalformedURLException, RemoteException, NotBoundException, InterruptedException {
        Socket socket = null;
        try {
            socket = new Socket(SERVER_ADDRESS, 5000);


            Scanner sc = new Scanner(socket.getInputStream());
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            String [] commandNDoptions = {""};
            boolean isOnline = true;

            InterfaceCounter ic = (InterfaceCounter) Naming.lookup("rmi://" + SERVER_ADDRESS + ":1099/statistics");
            Scanner input = new Scanner(System.in);
            CommandIntepreter ci = new CommandIntepreter(socket,sc, pw, ic);

        String username, password;
        boolean hasAuthenticated = false;
        byte tryChances = 3;

        do {
            System.out.print(sc.nextLine());
            username = input.nextLine();

            ci.setUsername(username);
            pw.println(username);
            pw.flush();

            System.out.print(sc.nextLine());
            password = input.nextLine();

            pw.println(password);
            pw.flush();

            System.out.println();
            tryChances--;
        } while(!(hasAuthenticated = Boolean.parseBoolean(sc.nextLine())) && tryChances > 0);

        if (!hasAuthenticated && tryChances <= 0)
            System.exit(-1);


        ci.setMainPath();
        //::>> Get Server's cwd
        pw.println("pwd");
        pw.flush();
        String cwd = sc.nextLine();
        sc.nextLine(); //::>> Dispose of the result;

        String result, fullCommand;



        while (isOnline) {
            if(!ci.isOnServer) {
                System.out.print(ci.getFs() + PathResolver.getRelPathString(ci.getPathNames()) + "$ ");
                fullCommand = input.nextLine();
                commandNDoptions = fullCommand.split(" ");

                result = ci.intepretCommand(fullCommand).replaceAll("0-0", "\n");
                System.out.println(result);

                if (commandNDoptions[0].toUpperCase().equals(Commands.GET)) {
                    sc.nextLine();
                    result = sc.nextLine();
                    System.out.println(result);
                }
                if (commandNDoptions[0].toUpperCase().equals(Commands.PUT)) {
                    System.out.println(result);
                    cwd = sc.nextLine();
                    result = sc.nextLine();
                    System.out.println(result);
                }
                if(commandNDoptions[0].toUpperCase().equals(Commands.EXIT)) {
                    cwd = sc.nextLine();
                    result = sc.nextLine();
                    System.out.println(result);
                }

            } else {
                System.out.print(cwd);
                fullCommand = input.nextLine();
                commandNDoptions = fullCommand.split(" ");

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

            if(commandNDoptions[0].toUpperCase().equals(Commands.EXIT)) {
                isOnline = false;
            }
        }

        } catch (IOException e) {
            System.out.println("::: COULD NOT CONNECT TO SERVER :::");
            return;
        } catch (NoSuchElementException ex) {
            System.out.println("::: CONNECTION DROPPED :::");
            return;
        }

    }

}
