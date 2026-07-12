import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private final int id;

    public Texture(String filepath) {
        // 1. Genera un ID unico per la texture sulla GPU
        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.id);

        // 2. Configura i parametri di ridimensionamento (Fondamentale per lo stile Pixel Art di Minecraft!)
        // GL_NEAREST impedisce alla GPU di sfocare i pixel quando ci si avvicina, mantenendoli nitidi
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // 3. Alloca i puntatori temporanei per leggere larghezza, altezza e canali del PNG
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Forza STB a caricare l'immagine dall'alto verso il basso (formato nativo di OpenGL)
            stbi_set_flip_vertically_on_load(true);

            // Carica l'immagine dal percorso
            ByteBuffer data = stbi_load(filepath, w, h, channels, 4);
            if (data == null) {
                throw new RuntimeException("Impossibile caricare la texture in: " + filepath + " -> " + stbi_failure_reason());
            }

            // 4. Spedisci i pixel alla memoria della scheda video
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);


            // Rilascia la memoria RAM del computer, ormai i dati sono al sicuro nella GPU
            stbi_image_free(data);
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, this.id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(this.id);
    }
}

