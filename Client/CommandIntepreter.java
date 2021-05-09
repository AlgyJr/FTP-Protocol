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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class FileSharingThread extends Thread {

    private int fileSharingPort;

    public FileSharingThread(int fileSharingPort) {
        this.fileSharingPort = fileSharingPort;
    }

    public void run() {
        try {
            System.out.println("Client Started!");
            Socket fileSocket = new Socket("localhost", this.fileSharingPort);
            Scanner is = new Scanner(fileSocket.getInputStream());
            PrintWriter pw = new PrintWriter(fileSocket.getOutputStream());

            pw.println("Sending File...");
            pw.flush();

            System.out.println(is.nextLine()); //::>> Print "File Has Been Read"

            is.close();
            pw.close();
            fileSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


public class CommandIntepreter {

    private String fs;
    private Path cwd;
    private ArrayList<String> pathNames;
    public boolean isOnServer;
    private final String ROOT_CLIENT = System.getProperty("user.dir") + "/FileSystem/ClientRoot";
    private int sockerPort;
    private PrintWriter pw;
    private Scanner sc;
    private Socket socket;
    private InterfaceCounter counter;

    public CommandIntepreter(Socket socket, Scanner sc, PrintWriter pw, InterfaceCounter counter) {
        this.socket = socket;
        this.sc = sc;
        this.pw = pw;
        this.fs = "[" + Constants.CLIENT_SIDE.name() + "]~username@localhost:/";
        this.pathNames = new ArrayList<>();
        this.cwd = Path.of(System.getProperty("user.dir") + "/FileSystem/ClientRoot");
        this.isOnServer = true;
        this.sockerPort = 5001;
        this.counter = counter;
    }

    public void awaitCommand() throws FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        while (this.isOnServer) {
            System.out.print(this.fs + PathResolver.getRelPathString(this.pathNames) + "$ ");
            String command = sc.nextLine();
            System.out.println(intepretCommand(command));
        }
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
            case Commands.CS: return "Available";
            case Commands.SS: return "Aavailable";
            case Commands.PWD: return "Avaliable";
            case Commands.MKDIR: return makeDirectory(option);
            case Commands.GET: return downloadFile(command);
            case Commands.PUT: return sendFile(command);
            case Commands.MVS: {this.isOnServer = true; return "";}
            case Commands.MVC: {this.isOnServer = false; return "";}
            default:
                return "Client: Command Not Found";
        }

    }

    private String sendFile(String command) throws FileNotFoundException {

        this.pw.println(command);
        this.pw.flush();

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

        Thread toWait = new Thread(() -> {

            try {
                Socket socket = new Socket("localhost",5050);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                double readChunk = 0;
                double percentage;
                int chnkSize = 100;
                int bytes;;

                byte[] byteArray = new byte[chnkSize];

                while ((bytes = fi.read(byteArray, 0, chnkSize)) != -1) {

                    //::>> Print Progress
                    readChunk += byteArray.length;
                    percentage = (readChunk / fileSize) * 100;
                    System.out.print("\r" + percentage + "%");
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
            String fileName;
            this.pw.println(command);
            this.pw.flush();

            //::>> Receber FileName e FileSize
            fileName = this.sc.nextLine();
            if(fileName.equals(Constants.FILE_NOT_FOUND.name()))
                return "::>> Error: File Not Found";

            long fileSize = Long.parseLong(this.sc.nextLine());

            Thread toWait = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket socket = new Socket("localhost",5050);
                        DataInputStream is = new DataInputStream(socket.getInputStream());
                        FileOutputStream fo = new FileOutputStream( cwd.toString()  + "/" +  fileName);
                        double readChunk = 0;
                        double percentage;

                        byte[] bytes;
                        while (!((bytes = is.readNBytes(100)).length == 0)) {
                            readChunk += bytes.length;
                            percentage = (readChunk / fileSize) * 100;
                            System.out.print("\r" + percentage + "%");
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

        try {
            System.out.println();
            System.out.println();
            // Print of statistics
            System.out.println(this.counter.statisticResume());
        } catch (RemoteException rm) {
            rm.printStackTrace();
        }


            /*DataInputStream is = new DataInputStream(this.socket.getInputStream());
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

            long fileSize = Long.parseLong(is.readUTF());

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
            pw.close();*/



        return "\nFile Download Complete!";
    }


    //::>> Getter and Setters
    public String getFs() {
        return fs;
    }

    public ArrayList<String> getPathNames() {
        return pathNames;
    }
}
