import org.lwjgl.opengl.GL20;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Shader {
    public final int programID;

    public Shader(String vertexPath, String fragmentPath) {
        // 1. Leggi i file degli shader come stringhe
        String vertexCode, fragmentCode;
        try {
            vertexCode = Files.readString(Paths.get(vertexPath));
            fragmentCode = Files.readString(Paths.get(fragmentPath));
        } catch (IOException e) {
            throw new RuntimeException("Impossibile leggere i file shader!", e);
        }

        //Compila il Vertex Shader
        int vertexID = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexID, vertexCode);
        GL20.glCompileShader(vertexID);
        checkCompileErrors(vertexID, "VERTEX");
        // Compila il Fragment Shader
        int fragmentID = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentID, fragmentCode);
        GL20.glCompileShader(fragmentID);
        checkCompileErrors(fragmentID, "FRAGMENT");

        // Unisci gli shader in un unico "Programma Shader" funzionante
        programID = GL20.glCreateProgram();
        GL20.glAttachShader(programID, vertexID);
        GL20.glAttachShader(programID, fragmentID);
        GL20.glLinkProgram(programID);
        checkCompileErrors(programID, "PROGRAM");

        // Una volta uniti, i singoli shader possono essere eliminati per liberare memoria
        GL20.glDeleteShader(vertexID);
        GL20.glDeleteShader(fragmentID);
    }
    // Attiva questo shader per l'uso nel ciclo di rendering
    public void bind() {
        GL20.glUseProgram(programID);
    }

    // Disattiva lo shader
    public void unbind() {
        GL20.glUseProgram(0);
    }
    // Pulisce la memoria della GPU quando il gioco chiude
    public void cleanup() {
        unbind();
        if (programID != 0) {
            GL20.glDeleteProgram(programID);
        }
    }
    // Funzione di utilità per verificare se la GPU ha riscontrato errori di compilazione
    private void checkCompileErrors(int shader, String type) {
        int success;
        if (!type.equals("PROGRAM")) {
            success = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
            if (success == GL20.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader, 1024);
                System.err.println("ERRORE COMPILAZIONE SHADER (" + type + "):\n" + log);
            }
        } else {
            success = GL20.glGetProgrami(shader, GL20.GL_LINK_STATUS);
            if (success == GL20.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(shader, 1024);
                System.err.println("ERRORE LINKING PROGRAMMA SHADER:\n" + log);
            }
        }
    }
}
