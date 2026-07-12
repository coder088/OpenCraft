import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.stb.STBImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.io.InputStream;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Shell32;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15C.glGenBuffers;

public class Main {
    private long window;
    private double lastTime = glfwGetTime();
    public Player player = new Player(8.0f, 20f, 20.0f);
    public World world = new World();
    public static void main(String[] args) {
        configureWindowsTaskbar();
        new Main().run();
    }

    private static void configureWindowsTaskbar() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString("OpenCraft.Game"));
        }
    }

    public void run() {
        //init window and opengl
        if (!glfwInit()) {
            throw new RuntimeException("Errore nel caricamento delle shaders. Il programma si chiuderà");
        }
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);//modern OPENGL
        // L'icona deve essere presente prima che Windows mostri la finestra nella barra.
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(800, 600, "OpenCraft", 0, 0);
        if (window == org.lwjgl.system.MemoryUtil.NULL) {
            throw new RuntimeException(" Creazione finestra fallita");
        }
        setWindowIcon();
        glfwMakeContextCurrent(window); //tells the GPU which window we are using
        glfwShowWindow(window);
        org.lwjgl.opengl.GL.createCapabilities();//initializes OPENGL inside the gpu
        glEnable(GL_DEPTH_TEST);
        Texture textureAtlas = new Texture("src/main/resources/atlas.png");
        org.lwjgl.glfw.GLFW.glfwSetKeyCallback(window, (windowHandle, key, scancode, action, mods) -> {
            // Check if P was pressend and if it was a single click
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_P && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {


                if (!player.isCursorDisabled) {
                    // if the cursor is visible, we hide it and block it
                    org.lwjgl.glfw.GLFW.glfwSetInputMode(windowHandle, org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED);
                    player.isCursorDisabled = true;
                    player.firstMouse = true; // resets mouse memory to prevent lag
                } else {
                    //if it was hidden, we show it
                    player.isCursorDisabled = false;
                    org.lwjgl.glfw.GLFW.glfwSetInputMode(windowHandle, org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
                }
            }
        });


        //allocates a native 16bit float buffer for 4x4 matrix
        java.nio.FloatBuffer matrixBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(16);
        Shader shader = new Shader("src/main/resources/vertex.glsl","src/main/resources/fragment.glsl");

        //initializes items from the JOML library
        Matrix4f modelMatrix = new Matrix4f();
        Matrix4f viewMatrix = new Matrix4f();
        Matrix4f projectionMatrix = new org.joml.Matrix4f();

        //player config
        float[] manicaArray = {
                // FACCIA FRONTALE
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,
                0.5f, -0.5f,  0.5f,   0.992f , 0.005f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.838f , 0.995f,
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,

                // FACCIA POSTERIORE
                0.5f, -0.5f, -0.5f,   0.838f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.992f , 0.005f,
                -0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.838f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.838f , 0.005f,

                // FACCIA SUPERIORE
                -0.5f,  0.5f, -0.5f,   0.838f , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.838f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.992f , 0.005f,
                -0.5f,  0.5f, -0.5f,   0.838f , 0.005f,

                // FACCIA INFERIORE
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.838f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.992f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.992f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.992f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,

                // FACCIA DESTRA
                0.5f, -0.5f,  0.5f,   0.838f , 0.005f,
                0.5f, -0.5f, -0.5f,   0.992f , 0.005f,
                0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.838f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.838f , 0.005f,

                // FACCIA SINISTRA
                -0.5f, -0.5f, -0.5f,   0.838f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.992f , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.838f , 0.995f,
                -0.5f, -0.5f, -0.5f,   0.838f , 0.005f
        };
        float[] manoArray = {
                // FACCIA FRONTALE
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,
                0.5f, -0.5f,  0.5f,   0.822f , 0.005f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.672f , 0.995f,
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,

                // FACCIA POSTERIORE
                0.5f, -0.5f, -0.5f,   0.672f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.827f, 0.005f,
                -0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.672f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.672f , 0.005f,

                // FACCIA SUPERIORE
                -0.5f,  0.5f, -0.5f,   0.822f  , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.822f  , 0.995f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.822f , 0.005f,
                -0.5f,  0.5f, -0.5f,   0.672f , 0.005f,

                // FACCIA INFERIORE
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.672f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.822f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.822f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.822f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,

                // FACCIA DESTRA
                0.5f, -0.5f,  0.5f,   0.672f , 0.005f,
                0.5f, -0.5f, -0.5f,   0.822f , 0.005f,
                0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.672f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.672f , 0.005f,

                // FACCIA SINISTRA
                -0.5f, -0.5f, -0.5f,   0.672f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.822f , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.672f , 0.995f,
                -0.5f, -0.5f, -0.5f,   0.672f , 0.005f
        };


        java.nio.FloatBuffer manicaBuffer = MemoryUtil.memAllocFloat(manicaArray.length);
        manicaBuffer.put(manicaArray).flip();
        java.nio.FloatBuffer manoBuffer = MemoryUtil.memAllocFloat(manoArray.length);
        manoBuffer.put(manoArray).flip();
        byte stride = 5 * Float.BYTES;
        int manicavaoID = glGenVertexArrays();
         glBindVertexArray(manicavaoID);
        int manicaVBOID =glGenBuffers();
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, manicaVBOID);
        glBufferData(GL_ARRAY_BUFFER,manicaBuffer,GL_STATIC_DRAW);
        org.lwjgl.opengl.GL20.glVertexAttribPointer(0, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 0);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);
        org.lwjgl.opengl.GL20.glVertexAttribPointer(1, 2, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        org.lwjgl.system.MemoryUtil.memFree(manicaBuffer);

        int manovaoID = glGenVertexArrays();
        glBindVertexArray(manovaoID);
        int manoVBOID =glGenBuffers();
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, manoVBOID);
        glBufferData(GL_ARRAY_BUFFER,manoBuffer,GL_STATIC_DRAW);
        org.lwjgl.opengl.GL20.glVertexAttribPointer(0, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 0);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);
        org.lwjgl.opengl.GL20.glVertexAttribPointer(1, 2, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        org.lwjgl.system.MemoryUtil.memFree(manoBuffer);


        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        while (!glfwWindowShouldClose(window)) {

            // clean the screen
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            //calculates Delta Time
            double currentTime = glfwGetTime();
            float deltaTime = (float)(currentTime - lastTime);
            lastTime = currentTime;

            //activates shader
            shader.bind();
            textureAtlas.bind();

            //Player update
            player.handleInput(window, deltaTime,world);
            world.update(player.getPosition());
            // creates 2 1 element arrays to hold both the mouse coords
            double[] mouseX = new double[1];
            double[] mouseY = new double[1];

            // asks opengl the current mouse position inside the window
            org.lwjgl.glfw.GLFW.glfwGetCursorPos(window, mouseX, mouseY);

            //Update the cam
            if(player.isCursorDisabled) {
                player.handleMouseInput(mouseX[0], mouseY[0]);
            }
            //configure matrixes
            modelMatrix.identity(); //the ground is NOT moving
            viewMatrix = player.getViewMatrix(); //gets the player cam
            projectionMatrix.identity().perspective((float) Math.toRadians(60.0f), 800.0f / 600.0f, 0.2f, 150.0f); // Alzato a 0.2f e aumentata la distanza a 150




            //finds the position of the variables inside the shader
            int modelLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(shader.programID, "model");
            int viewLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(shader.programID, "view");
            int projLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(shader.programID, "projection");

            //sends the matrixes to the GPU trough a buffer
            modelMatrix.get(0, matrixBuffer);
            org.lwjgl.opengl.GL20.glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            viewMatrix.get(0, matrixBuffer);
            org.lwjgl.opengl.GL20.glUniformMatrix4fv(viewLoc, false, matrixBuffer);
            projectionMatrix.get(0, matrixBuffer);
            org.lwjgl.opengl.GL20.glUniformMatrix4fv(projLoc, false, matrixBuffer);

            world.render();
            modelMatrix.identity()
                       .translate(player.getPosition().x,player.getPosition().y + 1.6f,player.getPosition().z)
                               .rotateY((float)Math.toRadians(-player.getYaw() - 90.0f))
                                       .rotateX((float)Math.toRadians(player.getPitch()))
                                               .translate(0.4f,-0.4f,-0.6f)
                                                     .scale(0.15f,0.15f,0.5f);
            modelMatrix.get(0, matrixBuffer);
            org.lwjgl.opengl.GL20.glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            glBindVertexArray(manicavaoID);
            glDrawArrays(GL_TRIANGLES,0,36);
            glBindVertexArray(0);
            modelMatrix.translate(0.0f,0.0f,-0.6f)
                          .scale(0.85f,0.85f,0.3f);
            modelMatrix.get(0, matrixBuffer);
            org.lwjgl.opengl.GL20.glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            glBindVertexArray(manovaoID);
            glDrawArrays(GL_TRIANGLES,0,36);
            glBindVertexArray(0);
            shader.unbind();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }



        }

    /** Imposta l'icona della finestra usando l'immagine inclusa nelle risorse dell'applicazione. */
    private void setWindowIcon() {
        try (InputStream input = Main.class.getResourceAsStream("/image.png");
             MemoryStack stack = MemoryStack.stackPush()) {
            if (input == null) {
                throw new IllegalStateException("Risorsa image.png non trovata.");
            }

            byte[] imageBytes = input.readAllBytes();
            ByteBuffer encodedImage = MemoryUtil.memAlloc(imageBytes.length);
            encodedImage.put(imageBytes).flip();

            try {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encodedImage, width, height, channels, 4);
                if (pixels == null) {
                    throw new IllegalStateException("Impossibile caricare image.png: " + STBImage.stbi_failure_reason());
                }

                try (GLFWImage.Buffer icon = GLFWImage.malloc(1)) {
                    icon.get(0).set(width.get(0), height.get(0), pixels);
                    glfwSetWindowIcon(window, icon);
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            } finally {
                MemoryUtil.memFree(encodedImage);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossibile leggere image.png.", e);
        }
    }
}
