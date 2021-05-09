package Server;


import Shared.Constants;
import Shared.Commands;
import Shared.PathResolver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;




class CommandInterpreter {
    private Socket socket;
    private ServerSocket fileSocket;
    private Scanner sc;
    private PrintWriter pw;
    private String fs;
    private Path cwd;
    private boolean isOnline;
    private ArrayList<String> pathNames;
    private final String ROOT_SERVER = System.getProperty("user.dir") + "/FileSystem/ServerRoot";
    private int fileSharePort = 5001;

    public CommandInterpreter(Socket socket, ServerSocket fileSocket) throws IOException {
        this.socket = socket;
        this.fileSocket = fileSocket;
        this.pw = new PrintWriter(this.socket.getOutputStream());
        this.sc = new Scanner(this.socket.getInputStream());
        this.fs = "[" + Constants.SERVER_SIDE.name() + "]~username@hostname:/";
        this.pathNames = new ArrayList<>();
        this.cwd = Path.of(System.getProperty("user.dir") + "/FileSystem/ServerRoot");
        this.isOnline = true;
    }


    public void awaitCommand() {
        String command, result;
        while(this.isOnline) {
            command = sc.nextLine();
            result = intepretCommand(command);
            pw.println(this.fs + PathResolver.getRelPathString(this.pathNames) + "$ ");
            pw.println(result);
            pw.flush();
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
            case Commands.SS: return "Available";
            case Commands.PWD: return getCurrentWorkingDirectory();
            case Commands.MKDIR: return makeDirectory(option);
            case Commands.PUT: return downloadFile(option);
            case Commands.GET:
                try {
                    sendFile(option);
                    return "";
                } catch (IOException e) {
                    return "Could Not Upload File";
                }
            case Commands.MVC: return "";
            case Commands.EXIT: {
                this.exit();
                return "Closing connection...";

            }
            default:
                return "Command Not Found";
        }

    }

    //::>> COMMANDS
    private String listDirectory(String path) {
        ArrayList<String> pathNames = PathResolver.resolvePath(this.cwd, path, this.pathNames);
        if (pathNames == null)  {
            return ":::> Error: Invalid Path For Operation";
        }

        Path pathToWalk = PathResolver.generatePath(ROOT_SERVER, pathNames);
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
        this.cwd = PathResolver.generatePath(ROOT_SERVER, newPathNames);
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

    private String getCurrentWorkingDirectory() {
        return this.cwd.toString();
    }

    private void sendFile(String path) throws IOException {

            ArrayList<String> pathNames = PathResolver.resolvePath(this.cwd, path, this.pathNames);
            if (pathNames == null)  {
                this.pw.println(Constants.FILE_NOT_FOUND.name());
                this.pw.flush();
                return;
            }

            String filePath = PathResolver.generatePath(this.cwd.toString(), pathNames).toString();
            System.out.println(filePath);
            File fileObj = new File(filePath);

            FileInputStream fi = new FileInputStream(fileObj);
            long fileSize = fileObj.length();

            //::>> Send FileName and FileSize
            this.pw.println(fileObj.getName());
            this.pw.flush();
            this.pw.println(fileSize);
            this.pw.flush();



            Thread toWait = new Thread(new Runnable() {
                @Override
                public void run() {

                    DataOutputStream os = null;
                    try {
                        Socket s = fileSocket.accept();
                        os = new DataOutputStream(s.getOutputStream());
                        int chnkSize = 100;
                        byte[] byteArray = new byte[chnkSize];
                        int bytes;
                        while ((bytes = fi.read(byteArray, 0, chnkSize)) != -1) {
                            System.out.println(bytes);
                            os.write(byteArray);
                            os.flush();
                        }
                        os.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


            toWait.start();
//        try {
//            toWait.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


            /*DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());
            ArrayList<String> pathNames = PathResolver.resolvePath(this.cwd, path, this.pathNames);
            if (pathNames == null)  {
                os.writeUTF(Constants.FILE_NOT_FOUND.name());
                os.flush();
                return;
            }

            String filePath = PathResolver.generatePath(this.cwd.toString(), pathNames).toString();
            System.out.println(filePath);

            File fileObj = new File(filePath);

            FileInputStream fi = new FileInputStream(fileObj);
            long fileSize = fileObj.length();

            //::>> Send File Size and File Name
            os.writeUTF(fileObj.getName());
            os.flush();
            os.writeUTF(fileSize + "");
            os.flush();


            int chnkSize = 25;
            byte[] byteArray = new byte[chnkSize];
            int bytes;
            while ((bytes = fi.read(byteArray, 0, chnkSize)) != -1) {
                System.out.println(bytes);
                os.write(byteArray);
                os.flush();
            }
            os.close();*/


    }

    private String downloadFile(String command) {
        String result, fileName, message;
        //this.pw.println(command);
        //this.pw.flush();

        //::>> Receber FileName e FileSize
        fileName = this.sc.nextLine();
        System.out.println(fileName);
        if(fileName.equals(Constants.FILE_NOT_FOUND.name()))
            return "::>> Error: File Not Found";

        long fileSize = Long.parseLong(this.sc.nextLine());
        System.out.println(fileSize);


        Thread toWait = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket s = fileSocket.accept();
                    //Socket socket = new Socket("localhost",5050);
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



        return "\nFile Download Complete!";
    }

    private void exit() {
        this.isOnline = false;
    }

}
