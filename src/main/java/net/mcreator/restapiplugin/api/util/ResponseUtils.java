package net.mcreator.restapiplugin.api.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating standardized API responses.
 */
public class ResponseUtils {

    /**
     * Create a success response with the given data.
     * 
     * @param data The data to include in the response
     * @return A map representing the response
     */
    public static Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }

    /**
     * Create an error response with the given message.
     * 
     * @param message The error message
     * @return A map representing the response
     */
    public static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    /**
     * Create an error response with the given message and additional data.
     * 
     * @param message The error message
     * @param data    Additional data to include in the response
     * @return A map representing the response
     */
    public static Map<String, Object> createErrorResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("data", data);
        return response;
    }
}