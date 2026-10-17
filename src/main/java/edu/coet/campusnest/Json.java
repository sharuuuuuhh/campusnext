package edu.coet.campusnest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private Json() {}

    public static Map<String, Object> read(jakarta.servlet.http.HttpServletRequest request) throws IOException {
        return MAPPER.readValue(request.getInputStream(), new TypeReference<>() {});
    }

    public static void send(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getOutputStream(), value);
    }

    public static Map<String, Object> error(String message) { return Map.of("error", message); }
}
