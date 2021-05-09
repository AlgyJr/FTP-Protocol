package Shared;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class PathResolver {
    public static final int DEFAULT_PATH_WALK_DEPTH = 2;
    public static Path generatePath(String path, ArrayList<String> pathNames) {
        //if (path == null) return null;

        String absPath = path + "/";
        for (String pathName : pathNames)
            absPath += pathName + "/";

        return Path.of(absPath);
    }

    //::>> Checks if current path is valid. If it is, returns an ArrayList with the pathName, otherwise return null
    public static ArrayList<String> resolvePath(Path cPath, String newPath, ArrayList<String> pathNames) {
        ArrayList<String> tempPathsName = new ArrayList<>(pathNames);
        String localPath;

        if(newPath.isEmpty()) return  tempPathsName;
        localPath = newPath.replaceAll("\\'", "/");
        String[] pathsName = localPath.split("/");

        try {

            for (String pathName : pathsName) {
                if (pathName.equals(".") && pathsName[0].equals("."))
                    continue;
                else if(pathName.equals(".") && !pathsName[0].equals("."))
                    throw new IllegalArgumentException();

                if (pathName.equals(".."))
                    tempPathsName.remove(tempPathsName.size() - 1);
                else
                    tempPathsName.add(pathName);
            }

            if (Files.exists(generatePath(cPath.toString(), tempPathsName)))
                return tempPathsName;
            else
                throw new IllegalArgumentException();

        } catch (IndexOutOfBoundsException ex) {
            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String getRelPathString(ArrayList<String> pathNames) {
        String pString = "";
        for(String path: pathNames)
            pString += path + "/";
        return pString;
    }

}
