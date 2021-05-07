package Server.Auth;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Authentication {
    private String username;
    private static String CREDENTIALS_FILE = "credentials.txt";

    public Authentication() { 
        this.username = "";
     }

    public boolean authenticate(String username, String password) {
        StringTokenizer s;
        String line, user, pass;
        boolean hasMatched = false;

        try {
            FileReader fr = new FileReader(CREDENTIALS_FILE);
            Scanner sc = new Scanner(fr);

            while(sc.hasNextLine()) {
                line = sc.nextLine();
                s = new StringTokenizer(line, ":");
                user = s.nextToken();
                pass = s.nextToken();

                // if matched break while and return true (success)
                if (username.equals(user) && password.equals(pass)) {
                    hasMatched = true;
                    this.username = username;
                    break;
                }
            }
            sc.close();
        } catch (FileNotFoundException fl) {
            System.err.println("File not found!");
        }

        return hasMatched;
    }

    public String getUsername() { return this.username; }
}