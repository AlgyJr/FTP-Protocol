package Client;

import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String path = "~nozotrox/whatever>";

        while(true) {
            System.out.print(path + " ");
            sc.nextLine();
        }
    }

}
