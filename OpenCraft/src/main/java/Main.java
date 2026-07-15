import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.stb.STBImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.io.InputStream;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Shell32;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20.*;

public class Main {
    private long window;
    private double lastTime = glfwGetTime();
    public Player player = new Player(8.0f, 20f, 20.0f);
    public World world = new World();
    public static final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();



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
            throw new RuntimeException("An error occurred while loading the shaders. The Game will close.");
        }
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);//modern OPENGL
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(800, 600, "OpenCraft", 0, 0);
        if (window == org.lwjgl.system.MemoryUtil.NULL) {
            throw new RuntimeException(" Window's creation failed");
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
                // ANTERIOR FACE
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,
                0.5f, -0.5f,  0.5f,   0.992f , 0.005f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.838f , 0.995f,
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,

                // POSTERIOR FACE
                0.5f, -0.5f, -0.5f,   0.838f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.992f , 0.005f,
                -0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.838f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.838f , 0.005f,

                // UPWARDS FACE
                -0.5f,  0.5f, -0.5f,   0.838f , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.838f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.992f , 0.005f,
                -0.5f,  0.5f, -0.5f,   0.838f , 0.005f,

                //INFERIOR FACE
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.838f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.992f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.992f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.992f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.838f , 0.005f,

                // RIGHT FACE
                0.5f, -0.5f,  0.5f,   0.838f , 0.005f,
                0.5f, -0.5f, -0.5f,   0.992f , 0.005f,
                0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.992f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.838f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.838f , 0.005f,

                // LEFT FACE
                -0.5f, -0.5f, -0.5f,   0.838f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.992f , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.992f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.838f , 0.995f,
                -0.5f, -0.5f, -0.5f,   0.838f , 0.005f
        };
        float[] manoArray = {
                // ANTERIOR FACE
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,
                0.5f, -0.5f,  0.5f,   0.822f , 0.005f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.672f , 0.995f,
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,

                // POSTERIOR FACE
                0.5f, -0.5f, -0.5f,   0.672f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.827f, 0.005f,
                -0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.672f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.672f , 0.005f,

                // UPWARDS FACE
                -0.5f,  0.5f, -0.5f,   0.822f  , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.822f  , 0.995f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.822f , 0.005f,
                -0.5f,  0.5f, -0.5f,   0.672f , 0.005f,

                //INFERIOR FACE
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,
                -0.5f, -0.5f, -0.5f,   0.672f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.822f , 0.995f,
                0.5f, -0.5f, -0.5f,   0.822f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.822f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.672f , 0.005f,

                // RIGHT FACE
                0.5f, -0.5f,  0.5f,   0.672f , 0.005f,
                0.5f, -0.5f, -0.5f,   0.822f , 0.005f,
                0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                0.5f,  0.5f, -0.5f,   0.822f , 0.995f,
                0.5f,  0.5f,  0.5f,   0.672f , 0.995f,
                0.5f, -0.5f,  0.5f,   0.672f , 0.005f,

                // LEFT FACE
                -0.5f, -0.5f, -0.5f,   0.672f , 0.005f,
                -0.5f, -0.5f,  0.5f,   0.822f , 0.005f,
                -0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f,  0.5f,   0.822f , 0.995f,
                -0.5f,  0.5f, -0.5f,   0.672f , 0.995f,
                -0.5f, -0.5f, -0.5f,   0.672f , 0.005f
        };

        float[] chrosshairArray = new float[]{
               -0.03f, 0.0f,0.0f,
                0.03f,0.0f,0.0f,
                0.0f,0.03f,0.0f,
                0.0f,-0.03f,0.0f

        };
        float[] hotbarBackgroundVertices = new float[] {
                -0.45f, -0.90f,  0.0f,
                0.45f, -0.90f,  0.0f,
                0.45f, -0.75f,  0.0f,

                -0.45f, -0.90f,  0.0f,
                0.45f, -0.75f,  0.0f,
                -0.45f, -0.75f,  0.0f
        };
        float[] hotbarSlotsVertices = new float[] {
                // Slot 1
                -0.44f, -0.89f, 0.0f,  -0.36f, -0.89f, 0.0f,  -0.36f, -0.76f, 0.0f,
                -0.44f, -0.89f, 0.0f,  -0.36f, -0.76f, 0.0f,  -0.44f, -0.76f, 0.0f,
                // Slot 2
                -0.35f, -0.89f, 0.0f,  -0.27f, -0.89f, 0.0f,  -0.27f, -0.76f, 0.0f,
                -0.35f, -0.89f, 0.0f,  -0.27f, -0.76f, 0.0f,  -0.35f, -0.76f, 0.0f,
                // Slot 3
                -0.26f, -0.89f, 0.0f,  -0.18f, -0.89f, 0.0f,  -0.18f, -0.76f, 0.0f,
                -0.26f, -0.89f, 0.0f,  -0.18f, -0.76f, 0.0f,  -0.26f, -0.76f, 0.0f,
                // Slot 4
                -0.17f, -0.89f, 0.0f,  -0.09f, -0.89f, 0.0f,  -0.09f, -0.76f, 0.0f,
                -0.17f, -0.89f, 0.0f,  -0.09f, -0.76f, 0.0f,  -0.17f, -0.76f, 0.0f,
                // Slot 5
                -0.04f, -0.89f, 0.0f,   0.04f, -0.89f, 0.0f,   0.04f, -0.76f, 0.0f,
                -0.04f, -0.89f, 0.0f,   0.04f, -0.76f, 0.0f,  -0.04f, -0.76f, 0.0f,
                // Slot 6
                0.09f, -0.89f, 0.0f,   0.17f, -0.89f, 0.0f,   0.17f, -0.76f, 0.0f,
                0.09f, -0.89f, 0.0f,   0.17f, -0.76f, 0.0f,   0.09f, -0.76f, 0.0f,
                // Slot 7
                0.18f, -0.89f, 0.0f,   0.26f, -0.89f, 0.0f,   0.26f, -0.76f, 0.0f,
                0.18f, -0.89f, 0.0f,   0.26f, -0.76f, 0.0f,   0.18f, -0.76f, 0.0f,
                // Slot 8
                0.27f, -0.89f, 0.0f,   0.35f, -0.89f, 0.0f,   0.35f, -0.76f, 0.0f,
                0.27f, -0.89f, 0.0f,   0.35f, -0.76f, 0.0f,   0.27f, -0.76f, 0.0f,
                // Slot 9
                0.36f, -0.89f, 0.0f,   0.44f, -0.89f, 0.0f,   0.44f, -0.76f, 0.0f,
                0.36f, -0.89f, 0.0f,   0.44f, -0.76f, 0.0f,   0.36f, -0.76f, 0.0f
        };
        float[] inventoryWindowVertices = new float[] {
                -0.35f, -0.45f,  0.0f,
                0.35f, -0.45f,  0.0f,
                0.35f,  0.45f,  0.0f,
                -0.35f, -0.45f,  0.0f,
                0.35f,  0.45f,  0.0f,
                -0.35f,  0.45f,  0.0f
        };
        float[] inventorySlotsVertices = new float[] {
                //ROW 1
                -0.32f,  0.25f, 0.0f,  -0.26f,  0.25f, 0.0f,  -0.26f,  0.35f, 0.0f,  -0.32f,  0.25f, 0.0f,  -0.26f,  0.35f, 0.0f,  -0.32f,  0.35f, 0.0f,
                -0.25f,  0.25f, 0.0f,  -0.19f,  0.25f, 0.0f,  -0.19f,  0.35f, 0.0f,  -0.25f,  0.25f, 0.0f,  -0.19f,  0.35f, 0.0f,  -0.25f,  0.35f, 0.0f,
                -0.18f,  0.25f, 0.0f,  -0.12f,  0.25f, 0.0f,  -0.12f,  0.35f, 0.0f,  -0.18f,  0.25f, 0.0f,  -0.12f,  0.35f, 0.0f,  -0.18f,  0.35f, 0.0f,
                -0.11f,  0.25f, 0.0f,  -0.05f,  0.25f, 0.0f,  -0.05f,  0.35f, 0.0f,  -0.11f,  0.25f, 0.0f,  -0.05f,  0.35f, 0.0f,  -0.11f,  0.35f, 0.0f,
                -0.03f,  0.25f, 0.0f,   0.03f,  0.25f, 0.0f,   0.03f,  0.35f, 0.0f,  -0.03f,  0.25f, 0.0f,   0.03f,  0.35f, 0.0f,  -0.03f,  0.35f, 0.0f,
                0.05f,  0.25f, 0.0f,   0.11f,  0.25f, 0.0f,   0.11f,  0.35f, 0.0f,   0.05f,  0.25f, 0.0f,   0.11f,  0.35f, 0.0f,   0.05f,  0.35f, 0.0f,
                0.12f,  0.25f, 0.0f,   0.18f,  0.25f, 0.0f,   0.18f,  0.35f, 0.0f,   0.12f,  0.25f, 0.0f,   0.18f,  0.35f, 0.0f,   0.12f,  0.35f, 0.0f,
                0.19f,  0.25f, 0.0f,   0.25f,  0.25f, 0.0f,   0.25f,  0.35f, 0.0f,   0.19f,  0.25f, 0.0f,   0.25f,  0.35f, 0.0f,   0.19f,  0.35f, 0.0f,
                0.26f,  0.25f, 0.0f,   0.32f,  0.25f, 0.0f,   0.32f,  0.35f, 0.0f,   0.26f,  0.25f, 0.0f,   0.32f,  0.35f, 0.0f,   0.26f,  0.35f, 0.0f,
                //ROW 2
                -0.32f,  0.10f, 0.0f,  -0.26f,  0.10f, 0.0f,  -0.26f,  0.20f, 0.0f,  -0.32f,  0.10f, 0.0f,  -0.26f,  0.20f, 0.0f,  -0.32f,  0.20f, 0.0f,
                -0.25f,  0.10f, 0.0f,  -0.19f,  0.10f, 0.0f,  -0.19f,  0.20f, 0.0f,  -0.25f,  0.10f, 0.0f,  -0.19f,  0.20f, 0.0f,  -0.25f,  0.20f, 0.0f,
                -0.18f,  0.10f, 0.0f,  -0.12f,  0.10f, 0.0f,  -0.12f,  0.20f, 0.0f,  -0.18f,  0.10f, 0.0f,  -0.12f,  0.20f, 0.0f,  -0.18f,  0.20f, 0.0f,
                -0.11f,  0.10f, 0.0f,  -0.05f,  0.10f, 0.0f,  -0.05f,  0.20f, 0.0f,  -0.11f,  0.10f, 0.0f,  -0.05f,  0.20f, 0.0f,  -0.11f,  0.20f, 0.0f,
                -0.03f,  0.10f, 0.0f,   0.03f,  0.10f, 0.0f,   0.03f,  0.20f, 0.0f,  -0.03f,  0.10f, 0.0f,   0.03f,  0.20f, 0.0f,  -0.03f,  0.20f, 0.0f,
                0.05f,  0.10f, 0.0f,   0.11f,  0.10f, 0.0f,   0.11f,  0.20f, 0.0f,   0.05f,  0.10f, 0.0f,   0.11f,  0.20f, 0.0f,   0.05f,  0.20f, 0.0f,
                0.12f,  0.10f, 0.0f,   0.18f,  0.10f, 0.0f,   0.18f,  0.20f, 0.0f,   0.12f,  0.10f, 0.0f,   0.18f,  0.20f, 0.0f,   0.12f,  0.20f, 0.0f,
                0.19f,  0.10f, 0.0f,   0.25f,  0.10f, 0.0f,   0.25f,  0.20f, 0.0f,   0.19f,  0.10f, 0.0f,   0.25f,  0.20f, 0.0f,   0.19f,  0.20f, 0.0f,
                0.26f,  0.10f, 0.0f,   0.32f,  0.10f, 0.0f,   0.32f,  0.20f, 0.0f,   0.26f,  0.10f, 0.0f,   0.32f,  0.20f, 0.0f,   0.26f,  0.20f, 0.0f,
                //ROW 3
                -0.32f, -0.05f, 0.0f,  -0.26f, -0.05f, 0.0f,  -0.26f,  0.05f, 0.0f,  -0.32f, -0.05f, 0.0f,  -0.26f,  0.05f, 0.0f,  -0.32f,  0.05f, 0.0f,
                -0.25f, -0.05f, 0.0f,  -0.19f, -0.05f, 0.0f,  -0.19f,  0.05f, 0.0f,  -0.25f, -0.05f, 0.0f,  -0.19f,  0.05f, 0.0f,  -0.25f,  0.05f, 0.0f,
                -0.18f, -0.05f, 0.0f,  -0.12f, -0.05f, 0.0f,  -0.12f,  0.05f, 0.0f,  -0.18f, -0.05f, 0.0f,  -0.12f,  0.05f, 0.0f,  -0.18f,  0.05f, 0.0f,
                -0.11f, -0.05f, 0.0f,  -0.05f, -0.05f, 0.0f,  -0.05f,  0.05f, 0.0f,  -0.11f, -0.05f, 0.0f,  -0.05f,  0.05f, 0.0f,  -0.11f,  0.05f, 0.0f,
                -0.03f, -0.05f, 0.0f,   0.03f, -0.05f, 0.0f,   0.03f,  0.05f, 0.0f,  -0.03f, -0.05f, 0.0f,   0.03f,  0.05f, 0.0f,  -0.03f,  0.05f, 0.0f,
                0.05f, -0.05f, 0.0f,   0.11f, -0.05f, 0.0f,   0.11f,  0.05f, 0.0f,   0.05f, -0.05f, 0.0f,   0.11f,  0.05f, 0.0f,   0.05f,  0.05f, 0.0f,
                0.12f, -0.05f, 0.0f,   0.18f, -0.05f, 0.0f,   0.18f,  0.05f, 0.0f,   0.12f, -0.05f, 0.0f,   0.18f,  0.05f, 0.0f,   0.12f,  0.05f, 0.0f,
                0.19f, -0.05f, 0.0f,   0.25f, -0.05f, 0.0f,   0.25f,  0.05f, 0.0f,   0.19f, -0.05f, 0.0f,   0.25f,  0.05f, 0.0f,   0.19f,  0.05f, 0.0f,
                0.26f, -0.05f, 0.0f,   0.32f, -0.05f, 0.0f,   0.32f,  0.05f, 0.0f,   0.26f, -0.05f, 0.0f,   0.32f,  0.05f, 0.0f,   0.26f,  0.05f, 0.0f
        };



        //finds the position of the variables inside the shader
        int modelLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(shader.programID, "model");
        int viewLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(shader.programID, "view");
        int projLoc = org.lwjgl.opengl.GL20.glGetUniformLocation(shader.programID, "projection");
        int useTexLoc = glGetUniformLocation(shader.programID, "useTexture");
        int guiColorLoc = glGetUniformLocation(shader.programID, "guiColor");


        // init the  buffers
        FloatBuffer inventoryWindowSlotsBuffer = MemoryUtil.memAllocFloat(inventorySlotsVertices.length);
        inventoryWindowSlotsBuffer.put(inventorySlotsVertices).flip();
        FloatBuffer inventoryWindowBuffer = MemoryUtil.memAllocFloat(inventoryWindowVertices.length);
        inventoryWindowBuffer.put(inventoryWindowVertices).flip();
        FloatBuffer sleaveBuffer = MemoryUtil.memAllocFloat(manicaArray.length);
        sleaveBuffer.put(manicaArray).flip();
        FloatBuffer handBuffer = MemoryUtil.memAllocFloat(manoArray.length);
        FloatBuffer crosshairBuffer = MemoryUtil.memAllocFloat(chrosshairArray.length);
        FloatBuffer hotbarBackgroundVerticesBuffer = MemoryUtil.memAllocFloat(hotbarBackgroundVertices.length);
        FloatBuffer hotbarSlotsVerticesBuffer = MemoryUtil.memAllocFloat(hotbarSlotsVertices.length);
        crosshairBuffer.put(chrosshairArray).flip();
        handBuffer.put(manoArray).flip();
        hotbarBackgroundVerticesBuffer.put(hotbarBackgroundVertices).flip();
        hotbarSlotsVerticesBuffer.put(hotbarSlotsVertices).flip();

        byte stride = 5 * Float.BYTES;
        //inventory slots
        int inventorySlotsVao = glGenVertexArrays();
        glBindVertexArray(inventorySlotsVao);
        int inventorySlotsVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER,inventorySlotsVbo);
        glBufferData(GL_ARRAY_BUFFER,inventoryWindowSlotsBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,3 * Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER,0);
        glBindVertexArray(0);
        MemoryUtil.memFree(inventoryWindowSlotsBuffer);
        //inventory window
        int inventoryWindowVao = glGenVertexArrays();
        glBindVertexArray(inventoryWindowVao);
        int inventoryWindowVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER,inventoryWindowVbo);
        glBufferData(GL_ARRAY_BUFFER,inventoryWindowBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,3 * Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER,0);
        glBindVertexArray(0);
        MemoryUtil.memFree(inventoryWindowBuffer);
        //hotbar background
        int hotbarBackgroundVao = glGenVertexArrays();
        glBindVertexArray(hotbarBackgroundVao);
        int hotbarBackgroundVbo =glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER,hotbarBackgroundVbo);
        glBufferData(GL_ARRAY_BUFFER,hotbarBackgroundVerticesBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,3 * Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        MemoryUtil.memFree(hotbarBackgroundVerticesBuffer);
        //hotbar slots
        int hotbarSlotsVao = glGenVertexArrays();
        glBindVertexArray(hotbarSlotsVao);
        int hotbarSlotsVbo =glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, hotbarSlotsVbo);
        glBufferData(GL_ARRAY_BUFFER,hotbarSlotsVerticesBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,3 * Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        MemoryUtil.memFree(hotbarSlotsVerticesBuffer);
        // init the sleave  vao,vbo. Sends them to the gpu
        int sleaveVaoID = glGenVertexArrays();
         glBindVertexArray(sleaveVaoID);
        int sleaveVBOID =glGenBuffers();
        glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, sleaveVBOID);
        glBufferData(GL_ARRAY_BUFFER,sleaveBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        org.lwjgl.system.MemoryUtil.memFree(sleaveBuffer);

        // init the hand and vao,vbo. Sends them to the gpu
        int handVaoID = glGenVertexArrays();
        glBindVertexArray(handVaoID);
        int handVBOID =glGenBuffers();
        glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, handVBOID);
        glBufferData(GL_ARRAY_BUFFER,handBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3,GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        MemoryUtil.memFree(handBuffer);
        // init the corosshair and vao,vbo. Sends them to the gpu
        int crosshairVaoID = glGenVertexArrays();
        glBindVertexArray(crosshairVaoID);
        int crosshairVboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER,crosshairVboID);
        glBufferData(GL_ARRAY_BUFFER,crosshairBuffer,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,3 * Float.BYTES,0);
        glEnableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        MemoryUtil.memFree(crosshairBuffer);

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
            projectionMatrix.identity().perspective((float) Math.toRadians(60.0f), 800.0f / 600.0f, 0.2f, 150.0f);

            //sends the matrixes to the GPU trough a buffer
            modelMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            viewMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(viewLoc, false, matrixBuffer);
            projectionMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(projLoc, false, matrixBuffer);
            glUniform1i(useTexLoc,1);
            world.render();
            modelMatrix.identity()
                       .translate(player.getPosition().x,player.getPosition().y + 1.6f,player.getPosition().z)
                               .rotateY((float)Math.toRadians(-player.getYaw() - 90.0f))
                                       .rotateX((float)Math.toRadians(player.getPitch()))
                                               .translate(0.4f,-0.4f,-0.6f)
                                                     .scale(0.15f,0.15f,0.5f);
            modelMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            glBindVertexArray(sleaveVaoID);
            glDrawArrays(GL_TRIANGLES,0,36);
            glBindVertexArray(0);
            modelMatrix.translate(0.0f,0.0f,-0.6f)
                          .scale(0.85f,0.85f,0.3f);
            modelMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            glBindVertexArray(handVaoID);
            glDrawArrays(GL_TRIANGLES,0,36);
            glBindVertexArray(0);
            glUniform1i(useTexLoc,0);
            //crosshair
            modelMatrix.identity();
            viewMatrix.identity();
            projectionMatrix.identity().ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);

            modelMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            viewMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(viewLoc, false, matrixBuffer);
            projectionMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(projLoc, false, matrixBuffer);

            glDisable(GL_DEPTH_TEST);
            glLineWidth(2.0f);
            glBindVertexArray(crosshairVaoID);
            glDrawArrays(GL_LINES, 0, 4);
            glBindVertexArray(0);
            glEnable(GL_DEPTH_TEST);
            glEnableVertexAttribArray(1);
            //inventory window
            if(player.hasInventoryOpen){
                glDisable(GL_DEPTH_TEST);
                //draws the window
                glUniform4f(guiColorLoc,  0.35f, 0.35f, 0.35f, 1.0f);
                glBindVertexArray(inventoryWindowVao);
                glDrawArrays(GL_TRIANGLES, 0, 6);
                //draws the grid
                glUniform4f(guiColorLoc, 0.50f, 0.50f, 0.50f, 1.0f);
                glBindVertexArray(inventorySlotsVao);
                glDrawArrays(GL_TRIANGLES, 0, 162);
                glBindVertexArray(0);
                glEnable(GL_DEPTH_TEST);
            }
            //hotbar background
            modelMatrix.identity();
            viewMatrix.identity();
            projectionMatrix.identity().ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);

            modelMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            viewMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(viewLoc, false, matrixBuffer);
            projectionMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(projLoc, false, matrixBuffer);
            if(player.hasInventoryOpen) {
                glDisable(GL_DEPTH_TEST);
                glLineWidth(2.0f);
                glUniform4f(guiColorLoc, 0.25f, 0.25f, 0.25f, 1.0f);
                glBindVertexArray(hotbarBackgroundVao);
                glDrawArrays(GL_TRIANGLES, 0, 6);
                glBindVertexArray(0);
                glEnable(GL_DEPTH_TEST);
                glEnableVertexAttribArray(1);
            }

            //hotbar slots
            modelMatrix.identity();
            viewMatrix.identity();
            projectionMatrix.identity().ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);

            modelMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(modelLoc, false, matrixBuffer);
            viewMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(viewLoc, false, matrixBuffer);
            projectionMatrix.get(0, matrixBuffer);
            glUniformMatrix4fv(projLoc, false, matrixBuffer);

            glDisable(GL_DEPTH_TEST);
            glLineWidth(2.0f);
            glUniform4f(guiColorLoc, 0.55f, 0.55f, 0.55f, 1.0f);
            glBindVertexArray(hotbarSlotsVao);
            glDrawArrays(GL_TRIANGLES, 0, 54);
            glBindVertexArray(0);
            glEnable(GL_DEPTH_TEST);
            glEnableVertexAttribArray(1);


            shader.unbind();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        for (Chunk chunk : world.chunks.values()) {
            chunk.saveChunkDuringOtherThread();
        }
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                System.err.println("The saving process took too long.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        }

    /**Sets the application icon using the image inside the project. */
    private void setWindowIcon() {
        try (InputStream input = Main.class.getResourceAsStream("/image.png");
             MemoryStack stack = MemoryStack.stackPush()) {
            if (input == null) {
                throw new IllegalStateException("Resource image.png not found.");
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
                    throw new IllegalStateException("Impossible to load image.png: " + STBImage.stbi_failure_reason());
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
            throw new RuntimeException("Impossible to read image.png.", e);
        }
    }
}
