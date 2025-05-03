package net.mcreator.restapiplugin.api.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for validating request data.
 */
public class ValidationUtils {

    /**
     * Validate that a workspace path exists and is a valid MCreator workspace.
     * 
     * @param workspacePath The path to validate
     * @return True if the path is valid, false otherwise
     */
    public static boolean isValidWorkspacePath(String workspacePath) {
        if (workspacePath == null || workspacePath.isEmpty()) {
            return false;
        }

        Path path = Paths.get(workspacePath);

        // Check if the path exists and is a directory
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return false;
        }

        // Check if the directory contains the .mcreator file
        Path mcreatorFile = path.resolve(".mcreator");
        return Files.exists(mcreatorFile) && Files.isRegularFile(mcreatorFile);
    }

    /**
     * Get the .mcreator file from a workspace path.
     * 
     * @param workspacePath The workspace path
     * @return The .mcreator file, or null if it doesn't exist
     */
    public static File getMCreatorFile(String workspacePath) {
        if (!isValidWorkspacePath(workspacePath)) {
            return null;
        }

        return Paths.get(workspacePath, ".mcreator").toFile();
    }

    /**
     * Check if a string is null or empty.
     * 
     * @param str The string to check
     * @return True if the string is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
}