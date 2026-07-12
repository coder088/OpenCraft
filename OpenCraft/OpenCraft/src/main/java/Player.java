import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private Vector3f position;
    private Vector3f forward;
    private Vector3f up;
    private float speed;
    private float yaw = -90.0f; // -90 gradi fa in modo che guardi dritto in avanti (verso l'asse Z negativo)
    private float pitch = 0.0f;  // Sguardo livellato all'orizzonte
    private double lastMouseX = 400.0; // Valori iniziali approssimativi (centro di una finestra 800x600)
    private double lastMouseY = 300.0;
    public boolean firstMouse = true; // Evita uno scatto violento al primissimo frame
    private float mouseSensitivity = 0.1f; // Regola la sensibilità del mouse
    public boolean isCursorDisabled = false;
    private float hitboxHeight = 1.8f;
    private float hitboxWidth = 0.6f;
    public boolean isGrounded = false;
    private float velocityY = 0.0f;
    public float eyeHeight = 1.6f;

    public Vector3f getPosition(){return position;}

    public Player(float startX, float startY, float startZ) {
        this.position = new org.joml.Vector3f(startX, startY, startZ);
        this.forward = new org.joml.Vector3f().normalize();
        this.up = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
        this.speed = 4.0f;
        // Dentro il costruttore di Player.java
        this.yaw = -90.0f;
        this.pitch = 0.0f;

// Calcoliamo il vettore forward iniziale per non partire con la telecamera cieca
        org.joml.Vector3f direction = new org.joml.Vector3f();
        direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        this.forward = direction.normalize();

    }
    public void handleInput(long window, float deltaTime,World world) {
        float spazio = speed * deltaTime;
        float spostamentoX = 0.0f;
        float spostamentoZ = 0.0f;


        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            Vector3f spostamento = new Vector3f(this.forward).mul(spazio);
            spostamento.y = 0.0f;
            if (spostamento.lengthSquared() > 0) {
                spostamento.normalize().mul(spazio);
            }
                spostamentoX += spostamento.x;
                spostamentoZ += spostamento.z;

        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            Vector3f spostamento = new Vector3f(this.forward).mul(spazio);
            spostamento.y = 0.0f;
            if (spostamento.lengthSquared() > 0) {
                spostamento.normalize().mul(spazio);
            }
            spostamentoX -= spostamento.x;
            spostamentoZ -= spostamento.z;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            org.joml.Vector3f dirLaterale = new org.joml.Vector3f();
            this.forward.cross(this.up, dirLaterale);
            dirLaterale.y = 0.0f;
            if (dirLaterale.lengthSquared() > 0) {
                dirLaterale.normalize().mul(spazio);
            }
            spostamentoX -= dirLaterale.x;
            spostamentoZ -= dirLaterale.z;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            org.joml.Vector3f dirLaterale = new org.joml.Vector3f();
            this.forward.cross(this.up, dirLaterale);
            dirLaterale.y = 0.0f;
            if (dirLaterale.lengthSquared() > 0) {
                dirLaterale.normalize().mul(spazio);
            }
            spostamentoX += dirLaterale.x;
            spostamentoZ += dirLaterale.z;
        }


        // Trova il blocco intero in cui si trovano i piedi del giocatore
        int startX;
        int endX ;
        int startY ;
        int endY ;
        int startZ;
        int endZ;
        float nuovaX = this.position.x + spostamentoX;
        startX = (int)Math.floor(nuovaX) -1;
        endX = (int)Math.floor(nuovaX) +1;
         startY = (int) Math.floor(this.position.y) - 1;
         endY   = (int) Math.floor(this.position.y) + 2; // Arriviamo a +2 perché il giocatore è alto 1.8 blocchi
         startZ = (int) Math.floor(this.position.z) - 1;
         endZ   = (int) Math.floor(this.position.z) + 1;
        AABB hitboxVirtualeX = gethitboxAt(nuovaX,this.position.y,this.position.z);
        boolean collisioneX = false;
        for(int bx = startX; bx <= endX; bx++){
            for(int by = startY; by <= endY; by++){
                for(int bz = startZ; bz <= endZ; bz++){
                    if(world.getBlockAt(bx,by,bz) != 0){
                        AABB hitboxBlocco = new AABB(bx,by,bz, bx+1,by+1,bz+1);
                        if(hitboxVirtualeX.intersects(hitboxBlocco)){
                            collisioneX = true;
                            break;
                        }
                    }
                }
            }
        }
        if(!collisioneX){
            this.position.x = nuovaX;
        }
        float nuovaZ = this.position.z  + spostamentoZ;
        startX = (int)Math.floor(this.position.x) -1;
        endX = (int)Math.floor(this.position.x) +1;
        startY = (int) Math.floor(this.position.y) - 1;
        endY   = (int) Math.floor(this.position.y) + 2; // Arriviamo a +2 perché il giocatore è alto 1.8 blocchi
        startZ = (int) Math.floor(nuovaZ) - 1;
        endZ   = (int) Math.floor(nuovaZ) + 1;
        AABB hitboxVirtualeZ = gethitboxAt(this.position.x,this.position.y,nuovaZ);
        boolean collisioneZ = false;
        for(int bx = startX; bx <= endX; bx++){
            for(int by = startY; by <= endY; by++){
                for(int bz = startZ; bz <= endZ; bz++){
                    if(world.getBlockAt(bx,by,bz)!=0){
                        AABB hitboxBlocco = new AABB(bx,by,bz,bx+1,by+1,bz+1);
                        if(hitboxVirtualeZ.intersects(hitboxBlocco)){
                            collisioneZ = true;
                            break;
                        }
                    }

                }
            }
        }
        if(!collisioneZ){
            this.position.z = nuovaZ;
        }


        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && isGrounded) {
            this.velocityY = 5.0f; // Dai una spinta verso l'alto
            this.isGrounded = false; // Non sei più a terra
        }
        // Applica la gravità permanente per spingere la velocità verso il basso
        this.velocityY -= 9.8f * deltaTime;
        // Ricalcola lo spostamento di questo frame in base alla velocità aggiornata
        float spostamentoY = this.velocityY * deltaTime;

        float nuovaY = this.position.y + spostamentoY;
        startX = (int)Math.floor(this.position.x) -1;
        endX = (int)Math.floor(this.position.x) +1;
        startY = (int) Math.floor(nuovaY) - 1;
        endY   = (int) Math.floor(nuovaY) + 2; // Arriviamo a +2 perché il giocatore è alto 1.8 blocchi
        startZ = (int) Math.floor(this.position.z) - 1;
        endZ   = (int) Math.floor(this.position.z) + 1;
        AABB hitboxVirtualeY = gethitboxAt(this.position.x,nuovaY - 0.005f,this.position.z);
        boolean collisioneY = false;
        float altezzaBloccoToccato = 0.0f;
        for(int bx = startX; bx <= endX; bx++){
            for(int by = startY; by <= endY; by++){
                for(int bz = startZ; bz <= endZ; bz++){
                    if(world.getBlockAt(bx,by,bz)!= 0){
                        AABB hitboxBlocco = new AABB(bx,by,bz,bx+1,by+1,bz+1);
                        if(hitboxVirtualeY.intersects(hitboxBlocco)){
                            collisioneY = true;
                            altezzaBloccoToccato = by+1f;
                            break;
                        }
                    }
                }
            }
        }
        if(collisioneY){
            if(spostamentoY <=0){
                this.position.y = altezzaBloccoToccato + 0.001f;
                this.velocityY = 0.0f;
                this.isGrounded = true;
            }
        }
        else{
            this.position.y = nuovaY;
            this.isGrounded = false;

        }
    }

    public org.joml.Matrix4f getViewMatrix() {
        //Istanziamo una nuova matrice vuota che restituiremo al Main
        org.joml.Matrix4f viewMatrix = new org.joml.Matrix4f();

        //Calcoliamo la posizione esatta degli occhi (Piedi + Altezza Occhi)
        org.joml.Vector3f posizioneOcchi = new org.joml.Vector3f(
                this.position.x,
                this.position.y + this.eyeHeight,
                this.position.z
        );

        // Calcoliamo il punto bersaglio (Target) verso cui guardare.
        // Il mirino si trova facendo: posizione degli occhi + la direzione dello sguardo (forward)
        org.joml.Vector3f target = new org.joml.Vector3f();
        posizioneOcchi.add(this.forward, target);

        //Configuriamo la matrice usando la funzione lookAt
        viewMatrix.identity().lookAt(
                posizioneOcchi.x, posizioneOcchi.y, posizioneOcchi.z, // Posizione della telecamera (Occhi)
                target.x,         target.y,         target.z,         // Punto mirato nello spazio
                this.up.x,        this.up.y,        this.up.z         // Vettore del cielo
        );

        return viewMatrix;
    }

    public void handleMouseInput(double xpos, double ypos) {
        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
        }
        // 1. Calcola lo spostamento del mouse rispetto al frame precedente
        float xoffset = (float) (xpos - lastMouseX);
        float yoffset = (float) (lastMouseY - ypos); // Invertito: le coordinate Y di GLFW vanno dall'alto verso il basso
        lastMouseX = xpos;
        lastMouseY = ypos;
        // 2. Applica la sensibilità
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;
        // 3. Aggiorna gli angoli
        yaw += xoffset;
        pitch += yoffset;
        // 4. Blocca il pitch per non spezzarsi il collo virtuale
        if (pitch > 89.0f)  pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
        // 5. Algoritmo trigonometrico per ricostruire il vettore di direzione dello sguardo
        org.joml.Vector3f direction = new org.joml.Vector3f();
        direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        // Assegna il risultato normalizzato al nostro vettore forward
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
        float raggio = getHitboxWidth()/2;
        float altezza = getHitboxHeight();
        return new AABB(
                this.position.x - raggio,  this.position.y,           this.position.z - raggio,
                this.position.x + raggio, this.position.y + altezza, this.position.z + raggio);
    }
    public AABB gethitboxAt(float x, float y, float z) {
        float raggio = 0.3f; // Larghezza totale del giocatore (0.6f) divisa per 2
        float altezza = 1.8f; // Altezza totale del corpo del giocatore

        // Calcoliamo i 6 confini della scatola partendo dalle coordinate (x, y, z) che gli passiamo
        float minX = x - raggio;
        float minY = y; // Ricorda: la Y rappresenta sempre i piedi del giocatore
        float minZ = z - raggio;

        float maxX = x + raggio;
        float maxY = y + altezza;
        float maxZ = z + raggio;

        // Restituiamo una nuova scatola AABB pronta per fare i test di scontro
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }


}


