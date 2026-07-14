import org.lwjgl.opengl.GL20;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    public final int programID;

    public Shader(String vertexPath, String fragmentPath) {
        String vertexCode, fragmentCode;
        try {
            vertexCode = Files.readString(Paths.get(vertexPath));
            fragmentCode = Files.readString(Paths.get(fragmentPath));
        } catch (IOException e) {
            throw new RuntimeException("Impossible to read the shaders file", e);
        }
        int vertexID = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        glShaderSource(vertexID, vertexCode);
        glCompileShader(vertexID);
        checkCompileErrors(vertexID, "VERTEX");
        int fragmentID = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        glShaderSource(fragmentID, fragmentCode);
        glCompileShader(fragmentID);
        checkCompileErrors(fragmentID, "FRAGMENT");
        programID = glCreateProgram();
        glAttachShader(programID, vertexID);
        glAttachShader(programID, fragmentID);
        glLinkProgram(programID);
        checkCompileErrors(programID, "PROGRAM");
        glDeleteShader(vertexID);
        glDeleteShader(fragmentID);
    }

    public void bind() {
        GL20.glUseProgram(programID);
    }
    public void unbind() {
        GL20.glUseProgram(0);
    }
    public void cleanup() {
        unbind();
        if (programID != 0) {
            glDeleteProgram(programID);
        }
    }
    private void checkCompileErrors(int shader, String type) {
        int success;
        if (!type.equals("PROGRAM")) {
            success = glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
            if (success == GL20.GL_FALSE) {
                String log = glGetShaderInfoLog(shader, 1024);
                System.err.println("ERROR WHILE COMPILING SHADERS(" + type + "):\n" + log);
            }
        } else {
            success = glGetProgrami(shader, GL_LINK_STATUS);
            if (success == GL_FALSE) {
                String log = glGetProgramInfoLog(shader, 1024);
                System.err.println("ERROR WHILE LINKING SHADER PROGRAM:\n" + log);
            }
        }
    }
}
