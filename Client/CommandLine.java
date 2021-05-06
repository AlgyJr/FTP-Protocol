package Client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class CommandLine {
    private ArrayList<String> cPath; //::>> Client Path
    private ArrayList<String> sPath; //::>> Server Path
    private boolean isOnline;
    private String cFileSystem; //::>> Current File System (Server) / (Client)
    private String command;

    public CommandLine() {
        this.cPath = new ArrayList<>(Arrays.asList("username@thread-name:/"));
        this.sPath = new ArrayList<>(Arrays.asList("username@client-root:/"));
        this.cFileSystem = "[" + Constants.SERVER_SIDE.name() + "]";
        this.isOnline = true;
        this.command = "";
    }

    public void awaitCommand() {
        Scanner sc = new Scanner(System.in);
        while (isOnline) {
            System.out.print(this.cFileSystem + formPath(this.sPath));
            this.command = sc.nextLine();
            System.out.println(intepretCommand());
        }
    }

    private String intepretCommand() {
        String [] commandNdOptions = this.command.split(" ");
        String option;
        String action = commandNdOptions[0];

        action = action.toUpperCase();

        if(commandNdOptions.length > 1)
            option = commandNdOptions[1];

        switch (action) {
            case Commands.CD: return "Available";
            case Commands.LS: return "Available";
            case Commands.CS: return "Available";
            case Commands.SS: return "Aavailable";
            case Commands.PWD: return "Avaliable";
            case Commands.EXIT: {
                this.exit();
                return "Closing connection...";

            }
            default:
                return "Command Not Found";
        }

    }

    private void exit() {
        this.isOnline = false;
    }

    //::>> Helper Methods
    //::>> From the arrayList of path elements it forms a string with the absolute path
    private String formPath(ArrayList<String> pathList) {
        String absPath = "";
        for(String path : pathList) {
            if(pathList.indexOf(path) == (pathList.size() - 1))
                absPath += path + "$ ";
            else
                absPath += path + "/";
        }
        return absPath;
    }


}
