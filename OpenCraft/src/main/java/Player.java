import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private Vector3f position;
    private Vector3f forward;
    private Vector3f up;
    private float speed;
    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private double lastMouseX = 400.0;
    private double lastMouseY = 300.0;
    public boolean firstMouse = true;
    private float mouseSensitivity = 0.1f;
    public boolean isCursorDisabled = false;
    private float hitboxHeight = 1.8f;
    private float hitboxWidth = 0.6f;
    public boolean isGrounded = false;
    private float velocityY = 0.0f;
    public float eyeHeight = 1.6f;
    public Vector3i playerRay;
    public float reach = 5.0f;
    private boolean leftMouseWasPressed = false;

    public Vector3f getPosition(){return position;}

    public Player(float startX, float startY, float startZ) {
        this.position = new Vector3f(startX, startY, startZ);
        this.forward = new Vector3f().normalize();
        this.up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.speed = 6.0f;
        this.yaw = -90.0f;
        this.pitch = 0.0f;

// load the initial forward vector to allow the camera to see
        Vector3f direction = new Vector3f();
        direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        this.forward = direction.normalize();

    }
    public void handleInput(long window, float deltaTime,World world) {
        float space = speed * deltaTime;
        float displacementX = 0.0f;
        float displacementZ = 0.0f;


        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            Vector3f displacement = new Vector3f(this.forward).mul(space);
            displacement.y = 0.0f;
            if (displacement.lengthSquared() > 0) {
                displacement.normalize().mul(space);
            }
                displacementX += displacement.x;
                displacementZ += displacement.z;

        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            Vector3f displacement = new Vector3f(this.forward).mul(space);
            displacement.y = 0.0f;
            if (displacement.lengthSquared() > 0) {
                displacement.normalize().mul(space);
            }
            displacementX -= displacement.x;
            displacementZ -= displacement.z;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            Vector3f lateralDir = new org.joml.Vector3f();
            this.forward.cross(this.up, lateralDir);
            lateralDir.y = 0.0f;
            if (lateralDir.lengthSquared() > 0) {
                lateralDir.normalize().mul(space);
            }
            displacementX -= lateralDir.x;
            displacementZ -= lateralDir.z;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            org.joml.Vector3f lateralDir = new org.joml.Vector3f();
            this.forward.cross(this.up, lateralDir);
            lateralDir.y = 0.0f;
            if (lateralDir.lengthSquared() > 0) {
                lateralDir.normalize().mul(space);
            }
            displacementX += lateralDir.x;
            displacementZ += lateralDir.z;
        }


        //finds the first solid block where the player is
        int startX;
        int endX ;
        int startY ;
        int endY ;
        int startZ;
        int endZ;
        float newX = this.position.x + displacementX;
        startX = (int)Math.floor(newX) -1;
        endX = (int)Math.floor(newX) +1;
         startY = (int) Math.floor(this.position.y) - 1;
         endY   = (int) Math.floor(this.position.y) + 2; // +2 since the player's height is 1.8 blocks
         startZ = (int) Math.floor(this.position.z) - 1;
         endZ   = (int) Math.floor(this.position.z) + 1;
        AABB hitboxVirtualeX = gethitboxAt(newX,this.position.y,this.position.z);
        boolean collisionX = false;
        for(int bx = startX; bx <= endX; bx++){
            for(int by = startY; by <= endY; by++){
                for(int bz = startZ; bz <= endZ; bz++){
                    if(world.getBlockAt(bx,by,bz) != 0){
                        AABB blockHitbox = new AABB(bx,by,bz, bx+1,by+1,bz+1);
                        if(hitboxVirtualeX.intersects(blockHitbox)){
                            collisionX = true;
                            break;
                        }
                    }
                }
            }
        }
        if(!collisionX){
            this.position.x = newX;
        }
        float newZ = this.position.z  + displacementZ;
        startX = (int)Math.floor(this.position.x) -1;
        endX = (int)Math.floor(this.position.x) +1;
        startY = (int) Math.floor(this.position.y) - 1;
        endY   = (int) Math.floor(this.position.y) + 2; // +2 since the player's height is 1.8 blocks
        startZ = (int) Math.floor(newZ) - 1;
        endZ   = (int) Math.floor(newZ) + 1;
        AABB hitboxVirtualeZ = gethitboxAt(this.position.x,this.position.y,newZ);
        boolean collisionZ = false;
        for(int bx = startX; bx <= endX; bx++){
            for(int by = startY; by <= endY; by++){
                for(int bz = startZ; bz <= endZ; bz++){
                    if(world.getBlockAt(bx,by,bz)!=0){
                        AABB blockHitbox = new AABB(bx,by,bz,bx+1,by+1,bz+1);
                        if(hitboxVirtualeZ.intersects(blockHitbox)){
                            collisionZ = true;
                            break;
                        }
                    }

                }
            }
        }
        if(!collisionZ){
            this.position.z = newZ;
        }


        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && isGrounded) {
            this.velocityY = 5.0f;
            this.isGrounded = false;
        }
        // Permanent gravity
        this.velocityY -= 9.8f * deltaTime;
        float displacementY = this.velocityY * deltaTime;

        float newY = this.position.y + displacementY;
        startX = (int)Math.floor(this.position.x) -1;
        endX = (int)Math.floor(this.position.x) +1;
        startY = (int) Math.floor(newY) - 1;
        endY   = (int) Math.floor(newY) + 2;
        startZ = (int) Math.floor(this.position.z) - 1;
        endZ   = (int) Math.floor(this.position.z) + 1;
        AABB VirtualHitboxY = gethitboxAt(this.position.x,newY - 0.005f,this.position.z);
        boolean collisionY = false;
        float tuchedBlockHeight = 0.0f;
        for(int bx = startX; bx <= endX; bx++){
            for(int by = startY; by <= endY; by++){
                for(int bz = startZ; bz <= endZ; bz++){
                    if(world.getBlockAt(bx,by,bz)!= 0){
                        AABB hitboxBlocco = new AABB(bx,by,bz,bx+1,by+1,bz+1);
                        if(VirtualHitboxY.intersects(hitboxBlocco)){
                            collisionY = true;
                            tuchedBlockHeight = by+1f;
                            break;
                        }
                    }
                }
            }
        }
        if(collisionY){
            if(displacementY <=0){
                this.position.y = tuchedBlockHeight + 0.001f;
                this.velocityY = 0.0f;
                this.isGrounded = true;
            }
        }
        else{
            this.position.y = newY;
            this.isGrounded = false;

        }
        boolean leftMouseIsPressedNow = (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS);
        if(leftMouseIsPressedNow && !this.leftMouseWasPressed){
            Vector3i temp;
            temp =rayCasting(world);
            if(temp != null) {
                world.setBlockAt(temp.x, temp.y, temp.z, (byte) 0);
            }
        }
        leftMouseWasPressed = leftMouseIsPressedNow;
    }

    public Matrix4f getViewMatrix() {
        Matrix4f viewMatrix = new Matrix4f();
        Vector3f eyePosition = new Vector3f(
                this.position.x,
                this.position.y + this.eyeHeight,
                this.position.z
        );
        Vector3f target = new Vector3f();
        eyePosition.add(this.forward, target);
        viewMatrix.identity().lookAt(
                eyePosition.x, eyePosition.y, eyePosition.z, // Camera position(eyes)
                target.x,         target.y,         target.z,         // target
                this.up.x,        this.up.y,        this.up.z         // Vector pointing upwards
        );

        return viewMatrix;
    }

    public void handleMouseInput(double xpos, double ypos) {
        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
        }
        //Finds the mouse's offset
        float xoffset = (float) (xpos - lastMouseX);
        float yoffset = (float) (lastMouseY - ypos);
        lastMouseX = xpos;
        lastMouseY = ypos;
        // applies sensitivity
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;
        //updates angles
        yaw += xoffset;
        pitch += yoffset;
        //checks if the player is going to go over a 90 deg angle upwards
        if (pitch > 89.0f)  pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
        Vector3f direction = new Vector3f();
        direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        this.forward = direction.normalize();
    }

    public float getYaw(){return this.yaw;}
    public float getPitch(){return this.pitch;}
    public float getHitboxHeight() {
        return hitboxHeight;
    }

    public float getHitboxWidth() {
        return hitboxWidth;
    }

    public AABB getHitbox(){
        float radius = getHitboxWidth()/2;
        float height = getHitboxHeight();
        return new AABB(
                this.position.x - radius,  this.position.y,           this.position.z - radius,
                this.position.x + radius, this.position.y + height, this.position.z + radius);
    }
    public AABB gethitboxAt(float x, float y, float z) {
        float radius = 0.3f; // total player width /2
        float height = 1.8f; // total player height
        float minX = x - radius;
        float minY = y;
        float minZ = z - radius;

        float maxX = x + radius;
        float maxY = y + height;
        float maxZ = z + radius;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public Vector3i rayCasting(World world) {
        Vector3f rayPos = new Vector3f(
                this.position.x,
                this.position.y + this.eyeHeight,
                this.position.z
        );
        float step = 0.02f;
        for (double x = 0; x < this.reach; x += step) {
            rayPos.x += this.forward.x * step;
            rayPos.y += this.forward.y * step;
            rayPos.z += this.forward.z * step;
            int bx = (int) Math.floor(rayPos.x);
            int by = (int) Math.floor(rayPos.y);
            int bz = (int) Math.floor(rayPos.z);
            if (world.getBlockAt(bx, by, bz) != 0) {
                return new Vector3i(bx, by, bz);
            }
        }
        return null;
    }




}


