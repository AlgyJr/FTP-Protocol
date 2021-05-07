package Client;

import Shared.Commands;
import Shared.Constants;
import Shared.PathResolver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandIntepreter {


    private String fs;
    private Path cwd;
    private ArrayList<String> pathNames;
    public boolean isOnServer;
    private final String ROOT_CLIENT = System.getProperty("user.dir") + "/FileSystem/ClientRoot";
    private Socket socket;

    public CommandIntepreter(Socket socket) {
        this.socket = socket;
        this.fs = "[" + Constants.CLIENT_SIDE.name() + "]~username@localhost:/";
        this.pathNames = new ArrayList<>();
        this.cwd = Path.of(System.getProperty("user.dir") + "/FileSystem/ClientRoot");
        this.isOnServer = true;
    }

    public void awaitCommand() {
        Scanner sc = new Scanner(System.in);
        while (this.isOnServer) {
            System.out.print(this.fs + PathResolver.getRelPathString(this.pathNames) + "$ ");
            String command = sc.nextLine();
            System.out.println(intepretCommand(command));
        }
    }

    public String intepretCommand(String command) {
        String [] commandNdOptions = command.split(" ");
        String option = "";
        String action = commandNdOptions[0];

        action = action.toUpperCase();

        if(commandNdOptions.length > 1)
            option = commandNdOptions[1];

        switch (action) {
            case Commands.CD: changeDirectory(option); return "";
            case Commands.LS: return listDirectory(option);
            case Commands.CS: return "Available";
            case Commands.SS: return "Aavailable";
            case Commands.PWD: return "Avaliable";
            case Commands.MKDIR: return makeDirectory(option);
            case Commands.GET: return downloadFile(command);
            case Commands.MVS: {this.isOnServer = true; return "";}
            case Commands.MVC: {this.isOnServer = false; return "";}
            default:
                return "Client: Command Not Found";
        }

    }

    //::>> COMMANDS
    private String listDirectory(String path) {
        ArrayList<String> pathNames = PathResolver.resolvePath(this.cwd, path, this.pathNames);
        if (pathNames == null)  {
            return ":::> Error: Invalid Path For Operation";
        }

        Path pathToWalk = PathResolver.generatePath(ROOT_CLIENT, pathNames);
        String dirTree = "";

        try(Stream<Path> walk = Files.walk(pathToWalk, PathResolver.DEFAULT_PATH_WALK_DEPTH)) {
            List<String> result = walk.map(file -> {
                Path rPath = file.relativize(Path.of(System.getProperty("user.dir")));
                String fPath = "|-";
                if (Files.isDirectory(file))
                    fPath += "";
                else
                    fPath += "-";

                for(int i = 0; i < rPath.getNameCount(); i++) {
                    if(Files.isDirectory(file))
                        fPath += "-";
                    else
                        fPath += "-";
                }

                fPath += file.getFileName().toString();
                return fPath;
            }).collect(Collectors.toList());

            for (String res: result) {
                dirTree += res + "0-0";
            }

        } catch (NoSuchFileException ex) {
            return ".";
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return dirTree;
    }

    private void changeDirectory(String path) {
        ArrayList<String> newPathNames = PathResolver.resolvePath(this.cwd, path, this.pathNames);
        if(newPathNames == null) {
            System.out.println(":::> Error: Invalid Path For Operation");
            return;
        }

        this.pathNames = new ArrayList<>(newPathNames);
        this.cwd = PathResolver.generatePath(ROOT_CLIENT, newPathNames);
    }

    private String makeDirectory(String folderName) {
        if(folderName.isEmpty()) return ":::Error: No Folder Name Specified";

        String folderPath = this.cwd.toString() + "/" + folderName;
        ArrayList<String> newPathNames = PathResolver.resolvePath(this.cwd, folderPath, this.pathNames);
        if(newPathNames != null) return "Folder Already Exists!" ;

        File folder = new File(folderPath);
        boolean hasCreated = folder.mkdir();
        if(hasCreated) {
            return "Folder Created Successfully!";
        }
        return "Could not create Folder!";
    }

    private String downloadFile(String command) {
        try {
            DataInputStream is = new DataInputStream(this.socket.getInputStream());
            PrintWriter pw = new PrintWriter(this.socket.getOutputStream());
            String result, fileName, message;

            //::>> Send Command
            pw.println(command);
            pw.flush();

            //::>> Recieve Status
            fileName = is.readUTF();
            if(fileName.equals(Constants.FILE_NOT_FOUND.name())) {
                is.close();
                pw.close();
                return "::>> Error: File Not Found";
            }

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
            is.close();
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "File Download Complete!";
    }


    //::>> Getter and Setters
    public String getFs() {
        return fs;
    }

    public ArrayList<String> getPathNames() {
        return pathNames;
    }
}
