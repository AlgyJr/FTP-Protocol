package Client;

import Shared.Commands;
import Shared.Constants;
import Shared.InterfaceCounter;
import Shared.PathResolver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
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
    private String ROOT_CLIENT = System.getProperty("user.dir") + "/FileSystem/ClientRoot";
    private PrintWriter pw;
    private Scanner sc;
    private Socket socket;
    private InterfaceCounter counter;
    private String username;
    private String hostForFileShare;

    public CommandIntepreter(Socket socket, Scanner sc, PrintWriter pw, InterfaceCounter counter) {
        this.socket = socket;
        this.sc = sc;
        this.pw = pw;
        this.fs = "[" + Constants.CLIENT_SIDE.name() + "]~@localhost:/";
        this.pathNames = new ArrayList<>();
        this.cwd = Path.of(System.getProperty("user.dir") + "/FileSystem/ClientRoot");
        this.isOnServer = true;
        this.counter = counter;
        this.hostForFileShare = "197.249.10.243";
    }

    public String intepretCommand(String command) throws FileNotFoundException {
        String [] commandNdOptions = command.split(" ");
        String option = "";
        String action = commandNdOptions[0];

        action = action.toUpperCase();

        if(commandNdOptions.length > 1)
            option = commandNdOptions[1];

        switch (action) {
            case Commands.CD: changeDirectory(option); return "";
            case Commands.LS: return listDirectory(option);
            case Commands.PWD: return getCurrentWorkingDirectory();
            case Commands.MKDIR: return makeDirectory(option);
            case Commands.GET: return downloadFile(command);
            case Commands.PUT: return uploadFile(command);
            case Commands.MVS: {this.isOnServer = true; return "";}
            case Commands.MVC: {this.isOnServer = false; return "";}
            case Commands.STAT: return printStat(command);
            case Commands.EXIT: return this.exit(command);
            default:
                return "Client: Command Not Found";
        }

    }



    //::>> COMMANDS
    private String listDirectory(String path) {
        ArrayList<String> pathNames = PathResolver.resolvePath(Path.of(ROOT_CLIENT), path, this.pathNames);
        if (pathNames == null)  {
            return ":::> Error: Invalid Path For Operation";
        }

        Path pathToWalk = PathResolver.generatePath(Path.of(ROOT_CLIENT).toString(), pathNames);
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
        ArrayList<String> newPathNames = PathResolver.resolvePath(Path.of(ROOT_CLIENT), path, this.pathNames);
        if(newPathNames == null) {
            System.out.println(":::> Error: Invalid Path For Operation");
            return;
        }

        this.pathNames = new ArrayList<>(newPathNames);
        this.cwd = PathResolver.generatePath(Path.of(ROOT_CLIENT).toString(), newPathNames);
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

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            String fileName;
            int chunkSize, portForFileSharing;
            this.pw.println(command);
            this.pw.flush();

            //::>> Receive chunkSize, portForFileSharing, FileName e FileSize

            fileName = this.sc.nextLine();
            if(fileName.equals(Constants.FILE_NOT_FOUND.name()))
                return "::>> Error: File Not Found";

            long fileSize = Long.parseLong(this.sc.nextLine());
            //::>> Receive Port Number to Send The File
            portForFileSharing = Integer.parseInt(this.sc.nextLine());
            chunkSize = Integer.parseInt(this.sc.nextLine());

            Thread toWait = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket socket = new Socket(hostForFileShare,portForFileSharing);
                        DataInputStream is = new DataInputStream(socket.getInputStream());
                        FileOutputStream fo = new FileOutputStream( cwd.toString()  + "/" +  fileName);
                        double readChunk = 0;
                        double percentage;

                        byte[] bytes;
                        while (!((bytes = is.readNBytes(chunkSize)).length == 0)) {
                            readChunk += bytes.length;
                            percentage = (readChunk / fileSize) * 100;
                            System.out.print("\r" + df.format(percentage) + "%");
                            fo.write(bytes);
                        }
                        fo.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        try {
            toWait.start();
            toWait.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "\nFile Download Complete!";
    }

    private String uploadFile(String command) throws FileNotFoundException {

        this.pw.println(command);
        this.pw.flush();

        int portForFileShare, chnkSize;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        ArrayList<String> pathNames = PathResolver.resolvePath(this.cwd, command.split(" ")[1], this.pathNames);
        if (pathNames == null)  {
            this.pw.println(Constants.FILE_NOT_FOUND.name());
            this.pw.flush();
            return Constants.FILE_NOT_FOUND.name();
        }



        String filePath = PathResolver.generatePath(this.cwd.toString(), pathNames).toString();
        File fileObj = new File(filePath);

        FileInputStream fi = new FileInputStream(fileObj);
        long fileSize = fileObj.length();

        //::>> Send FileName and FileSize
        this.pw.println(fileObj.getName());
        this.pw.flush();

        //::>> Receive Port Number to Send The File
        portForFileShare = Integer.parseInt(this.sc.nextLine());
        chnkSize = Integer.parseInt(this.sc.nextLine());



        Thread toWait = new Thread(() -> {

            try {
                Socket socket = new Socket(hostForFileShare,portForFileShare);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                double readChunk = 0;
                double percentage;
                int bytes;;

                byte[] byteArray = new byte[chnkSize];

                while ((bytes = fi.read(byteArray, 0, chnkSize)) != -1) {

                    //::>> Print Progress
                    readChunk += byteArray.length;
                    percentage = (readChunk / fileSize) * 100;
                    System.out.print("\r" + df.format(percentage) + "%");
                    os.write(byteArray);
                    os.flush();
                }
                System.out.println();
                os.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            toWait.start();
            toWait.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "File Uploaded!";
    }

    private String exit(String command) {
        this.pw.println(command);
        this.pw.flush();
        return "";
    }

    private String getCurrentWorkingDirectory() {
        return this.cwd.toString();
    }

    private String printStat(String command) {
        try {
            return this.counter.statisticResume();
        } catch (RemoteException e) {
            return "::: Could not print statistics!";
        }
    }

    //::>> Getter and Setters
    public String getFs() {
        return fs;
    }

    public ArrayList<String> getPathNames() {
        return pathNames;
    }

    public void setMainPath() {
        ArrayList<String> newPathNames = PathResolver.resolvePath(this.cwd, this.username, this.pathNames);
        if(newPathNames == null)
            makeDirectory(this.username);
        this.cwd = Path.of(this.cwd.toString() + "/" + this.username);
        ROOT_CLIENT = ROOT_CLIENT.toString() + "/" + this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
