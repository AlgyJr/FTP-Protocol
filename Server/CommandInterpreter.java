package Server;

import Server.Auth.Authentication;
import Shared.Constants;
import Shared.Commands;
import Shared.PathResolver;
import Server.rmi.Counter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
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
    private Counter c;
    private int chunkSize = 100;

    public CommandInterpreter(Socket socket, ServerSocket fileSocket, Counter c) throws IOException {
        this.socket = socket;
        this.fileSocket = fileSocket;
        this.pw = new PrintWriter(this.socket.getOutputStream());
        this.sc = new Scanner(this.socket.getInputStream());
        this.fs = "[" + Constants.SERVER_SIDE.name() + "]~username@hostname:/";
        this.pathNames = new ArrayList<>();
        this.cwd = Path.of(System.getProperty("user.dir") + "/FileSystem/ServerRoot");
        this.isOnline = true;
        this.c = c;
    }

    public boolean isAuthenticated(Authentication auth) {
        String username, password;
        boolean hasAuthenticated = false;
        byte tryChances = 3;

        do {
            pw.println("Utilizador: ");
            pw.flush();

            // Receive username
            username = sc.nextLine();

            pw.println("Palavra-passe: ");
            pw.flush();

            // Reveive password
            password = sc.nextLine();

            // Then check credentials
            hasAuthenticated = auth.authenticate(username, password);

            if (hasAuthenticated) {
                pw.println("true");
            } else {
                pw.println("false");
            }

            pw.flush();
            tryChances--;

        } while (!hasAuthenticated && tryChances > 0);

        return hasAuthenticated;
    }

    public void awaitCommand() {
        try {
            String command, result;
            while (this.isOnline) {
                command = sc.nextLine();
                result = intepretCommand(command);
                pw.println(this.fs + PathResolver.getRelPathString(this.pathNames) + "$ ");
                pw.println(result);
                pw.flush();
            }
        } catch (NoSuchElementException ex) {
            System.out.println(":::: CONNECTIO CLOSED UNEXPECTEDLY");
            System.out.println("IP: "+ this.socket.getInetAddress() + " Port: "+ this.socket.getPort() );
            System.out.println("::::::::::::::::::::\n\n");
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
            case Commands.PWD: return getCurrentWorkingDirectory();
            case Commands.MKDIR: return makeDirectory(option);
            case Commands.PUT: return receiveFile(option);
            case Commands.STAT: return printStat(command);
            case Commands.SETCHUNK: return setChunkSize(option);
            case Commands.GET: sendFile(option); return "";
            case Commands.MVC: return "";
            case Commands.EXIT: {this.exit(); return "::: CLOSED CONNECTION";}
            default:
                return "Command Not Found";
        }

    }

    //::>> COMMANDS
    private String listDirectory(String path) {
        ArrayList<String> pathNames = PathResolver.resolvePath(Path.of(ROOT_SERVER), path, this.pathNames);
        if (pathNames == null)  {
            return ":::> Error: Invalid Path For Operation";
        }

        Path pathToWalk = PathResolver.generatePath(Path.of(ROOT_SERVER).toString(), pathNames);
        String dirTree = "";

        try(Stream<Path> walk = Files.walk(pathToWalk, PathResolver.DEFAULT_PATH_WALK_DEPTH)) {
            List<String> result = walk.map(file -> {
                Path rPath = file.relativize(Path.of(System.getProperty("user.dir")));
                String fPath = "|-";
                if (Files.isDirectory(file))
                    fPath += "-";
                else
                    fPath += "---";

//                //for(int i = 0; i < rPath.getNameCount(); i++) {
//                    if(Files.isDirectory(file))
//                        fPath += "-";
//                    else
//                        fPath += "--";
//                //}

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
        ArrayList<String> newPathNames = PathResolver.resolvePath(Path.of(ROOT_SERVER), path, this.pathNames);
        if(newPathNames == null) {
            System.out.println(":::> Error: Invalid Path For Operation");
            return;
        }

        this.pathNames = new ArrayList<>(newPathNames);
        this.cwd = PathResolver.generatePath(Path.of(ROOT_SERVER).toString(), newPathNames);
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

    private String printStat(String command) {
        try {
            return this.c.statisticResume();
        } catch (RemoteException e) {
            return "::: Could not print statistics!";
        }
    }

    private void sendFile(String path) {

        try {
            ArrayList<String> pathNames = PathResolver.resolvePath(this.cwd, path, this.pathNames);
            if (pathNames == null) {
                this.pw.println(Constants.FILE_NOT_FOUND.name());
                this.pw.flush();
                return;
            }

            String filePath = PathResolver.generatePath(this.cwd.toString(), pathNames).toString();
            System.out.println(filePath);
            File fileObj = new File(filePath);

            FileInputStream fi = new FileInputStream(fileObj);
            long fileSize = fileObj.length();

            //::>> Send chunkSize, portForFileSharing, FileName and FileSize
            this.pw.println(fileObj.getName());
            this.pw.flush();
            this.pw.println(fileSize);
            this.pw.flush();
            this.pw.println(Server.SERVER_FILE_SHARING_PORT);
            this.pw.flush();
            this.pw.println(chunkSize);
            this.pw.flush();


            Thread toWait = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket s = fileSocket.accept();
                        DataOutputStream os = new DataOutputStream(s.getOutputStream());
                        int chnkSize = 100;
                        byte[] byteArray = new byte[chnkSize];

                        int bytes;
                        while ((bytes = fi.read(byteArray, 0, chnkSize)) != -1) {
                            os.write(byteArray);
                            os.flush();
                        }
                        os.close();
                        // Incrementação da quantidade de ficheiros descarregados
                        c.incrementQtdDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            toWait.start();

        } catch (IOException ex) {
            this.pw.println("::: COULD NOT DOWNLOAD FILE");
            this.pw.flush();
        }
    }

    private String receiveFile(String command) {
        String fileName;



        //::>> Receber FileName e FileSize
        fileName = this.sc.nextLine();
        if(fileName.equals(Constants.FILE_NOT_FOUND.name()))
            return "::: FILE NOT FOUND";

        this.pw.println(Server.SERVER_FILE_SHARING_PORT);
        this.pw.flush();
        this.pw.println(chunkSize);
        this.pw.flush();

        Thread toWait = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket fileSharingSocket;
                try {
                    fileSharingSocket = fileSocket.accept();
                    DataInputStream is = new DataInputStream(fileSharingSocket.getInputStream());
                    FileOutputStream fo = new FileOutputStream( cwd.toString()  + "/" +  fileName);

                    byte[] bytes;
                    while (!((bytes = is.readNBytes(chunkSize)).length == 0)) {
                        fo.write(bytes);
                    }
                    fo.close();
                    // Incrementação da quantidade de ficheiros carregados pelo cliente
                    c.incrementQtdUp();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    System.out.println(":::: COULD NOT OPEN CONNECTION");
                    System.out.println("IP: "+ socket.getInetAddress() + " Port: "+ socket.getPort() );
                    System.out.println("::::::::::::::::::::\n\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        toWait.start();
        return "File Saved!";
    }

    private String setChunkSize(String chunk) {
        int chunkSize = Integer.parseInt(chunk);
        chunkSize *= 1024;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

        try {
            this.chunkSize = chunkSize;
        } catch (NumberFormatException ex) {
            return ":: COULD NOT SET CHUNK SIZE";
        }
        return "CHUNK SIZE: " + df.format(chunkSize / 1024.0 / 1024.0) + "MB";
    }

    private void exit() {
        this.isOnline = false;
    }

    public void setFs(String fs) {
        this.fs = fs;
    }
}
