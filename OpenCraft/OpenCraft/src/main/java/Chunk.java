import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

public class Chunk {
    public int chunkX;
    public int chunkZ;
    private int vaoID;
    private int vboID;
    private int vertexCount;
    private final int CHUNK_WIDTH = 16;
    private final int CHUNK_HEIGHT = 64;
    private final int CHUNK_LENGTH = 16;

    // L'array tridimensionale che contiene gli ID dei blocchi
    private byte[][][] blocks;

    public Chunk(int cx, int cz,World world) {
        this.chunkX = cx;
        this.chunkZ = cz;
        this.blocks = new byte[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_LENGTH];

        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int z = 0; z < CHUNK_LENGTH; z++) {
                int altezzaSuperficie;
                int worldX = chunkX*16 + x;
                int worldZ = chunkZ*16 + z;
                float valoreNoise = world.getNoiseAt(worldX,worldZ);
                altezzaSuperficie = 20 + (int)(valoreNoise * 15f);
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    if (y == altezzaSuperficie) {
                        blocks[x][y][z] = 1; // Solo l'ULTIMO blocco in cima è ERBA
                    } else if (y < altezzaSuperficie) {
                        blocks[x][y][z] = 2; // Tutto ciò che sta sotto (da 0 a 9) è ROCCIA
                    } else {
                        blocks[x][y][z] = 0; // Tutto ciò che sta sopra è ARIA
                    }
                }
            }
        }
    }


    public void generateMesh(World world) {
        // Lista dinamica per accumulare i float (5 per vertice: X, Y, Z, U, V)
        java.util.ArrayList<Float> vertexData = new java.util.ArrayList<>();

        // Scansioniamo l'intero volume 3D del Chunk (X, Y, Z)
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_LENGTH; z++) {

                    byte blockType = blocks[x][y][z];
                    if (blockType == 0) continue; // Se è ARIA (0), salta il blocco

                    // Coordinate assolute nel mondo 3D per questo specifico blocco
                    float worldX = (this.chunkX * 16) + x;
                    float worldY = y;
                    float worldZ = (this.chunkZ * 16) + z;

                    // Margine di sicurezza anti-sfarfallio (Texture Bleeding)
                    float epsilon = 0.005f;
                    float vMin = 0.0f + epsilon;
                    float vMax = 1.0f - epsilon;

                    // =========================================================================
                    // 1. FACCIA SUPERIORE (Guarda verso l'alto: Asse Y+)
                    // =========================================================================
                    if (y == CHUNK_HEIGHT - 1 || blocks[x][y + 1][z] == 0) {
                        float topUMin, topUMax;
                        if (blockType == 1) {
                            topUMin = 0.003f  + epsilon;
                            topUMax = 0.163f - epsilon;
                        } else {
                            topUMin = 0.169f + epsilon;
                            topUMax = 0.329f - epsilon;
                        }

                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        topUMin, vMin);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ + 1.0f, topUMin, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, topUMax, vMax);

                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, topUMax, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        topUMax, vMin);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        topUMin, vMin);
                    }

                    // =========================================================================
                    // 2. FACCIA INFERIORE (Guarda verso il basso: Asse Y-)
                    // =========================================================================
                    if (y == 0 || blocks[x][y - 1][z] == 0) {
                        float bottomUMin, bottomUMax;
                        if (blockType == 1) {
                            // Erba sotto terra -> Slot 3: Solo Terra (0.75f a 1.0f)
                            bottomUMin = 0.501f  + epsilon;
                            bottomUMax = 0.661f - epsilon;
                        } else {
                            // Roccia -> Slot 1: Roccia (0.25f a 0.50f)
                            bottomUMin = 0.169f  + epsilon;
                            bottomUMax = 0.329f - epsilon;
                        }

                        addVertex(vertexData, worldX,        worldY,        worldZ,        bottomUMin, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        bottomUMax, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, bottomUMax, vMax);

                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, bottomUMax, vMax);
                        addVertex(vertexData, worldX,        worldY,        worldZ + 1.0f, bottomUMin, vMax);
                        addVertex(vertexData, worldX,        worldY,        worldZ,        bottomUMin, vMin);
                    }

                    // =========================================================================
                    // COORDENATE PER I LATI TEMPORANEE (Dichiarate al volo fuori per pulizia)
                    // =========================================================================
                    float sideUMin, sideUMax;
                    if (blockType == 1) {
                        // Erba sui lati -> Slot 2: Terra con Erba (0.50f a 0.75f)
                        sideUMin = 0.335f  + epsilon;
                        sideUMax = 0.495f - epsilon;
                    } else {
                        // Roccia sui lati -> Slot 1: Roccia (0.25f a 0.50f)
                        sideUMin = 0.169f  + epsilon;
                        sideUMax = 0.329f - epsilon;
                    }

                    // 3. FACCIA FRONTALE (Guarda verso Z+)
                    if (world.getBlockAt(worldX, worldY, worldZ + 1.0f) == 0) {
                        addVertex(vertexData, worldX,        worldY,        worldZ + 1.0f, sideUMin, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, sideUMax, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, sideUMax, vMax);

                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, sideUMax, vMax);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ + 1.0f, sideUMin, vMax);
                        addVertex(vertexData, worldX,        worldY,        worldZ + 1.0f, sideUMin, vMin);
                    }

                    // 4. FACCIA DESTRA (Guarda verso X+)
                    if (world.getBlockAt(worldX + 1.0f, worldY, worldZ) == 0) {
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, sideUMin, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        sideUMax, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        sideUMax, vMax);

                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        sideUMax, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, sideUMin, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, sideUMin, vMin);
                    }

                    // 5. FACCIA DIETRO (Guarda verso Z-)
                    if (world.getBlockAt(worldX, worldY, worldZ - 1.0f) == 0) {
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        sideUMin, vMin);
                        addVertex(vertexData, worldX,        worldY,        worldZ,        sideUMax, vMin);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        sideUMax, vMax);

                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        sideUMax, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        sideUMin, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        sideUMin, vMin);
                    }

                    // 6. FACCIA SINISTRA (Guarda verso X-)
                    if (world.getBlockAt(worldX - 1.0f, worldY, worldZ) == 0) {
                        addVertex(vertexData, worldX,        worldY,        worldZ,        sideUMin, vMin);
                        addVertex(vertexData, worldX,        worldY,        worldZ + 1.0f, sideUMax, vMin);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ + 1.0f, sideUMax, vMax);

                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ + 1.0f, sideUMax, vMax);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        sideUMin, vMax);
                        addVertex(vertexData, worldX,        worldY,        worldZ,        sideUMin, vMin);
                    }
                }
            }
        }

        // Calcoliamo quanti vertici totali abbiamo registrato (5 float per ogni vertice)
        this.vertexCount = vertexData.size() / 5;

        // Convertiamo la ArrayList in un array di float classico
        float[] finalVertices = new float[vertexData.size()];
        for (int i = 0; i < vertexData.size(); i++) {
            finalVertices[i] = vertexData.get(i);
        }

        // Carichiamo la mesh sulla GPU
        java.nio.FloatBuffer vertexBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(finalVertices.length);
        vertexBuffer.put(finalVertices).flip();

        this.vaoID = org.lwjgl.opengl.GL30.glGenVertexArrays();
        org.lwjgl.opengl.GL30.glBindVertexArray(this.vaoID);

        this.vboID = org.lwjgl.opengl.GL15.glGenBuffers();
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, this.vboID);
        org.lwjgl.opengl.GL15.glBufferData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, vertexBuffer, org.lwjgl.opengl.GL15.GL_STATIC_DRAW);

        // Configurazione dei puntatori del VAO (Ogni blocco occupa 20 byte)
        int stride = 5 * Float.BYTES;

        // Strada 0: Posizione (X, Y, Z)
        org.lwjgl.opengl.GL20.glVertexAttribPointer(0, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 0);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);

        // Strada 1: Texture Coords (U, V)
        org.lwjgl.opengl.GL20.glVertexAttribPointer(1, 2, org.lwjgl.opengl.GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(1);

        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, 0);
        org.lwjgl.opengl.GL30.glBindVertexArray(0);

        org.lwjgl.system.MemoryUtil.memFree(vertexBuffer);
    }


    // Assicurati di avere questo metodo di supporto in fondo a Chunk.java
    private void addVertex(java.util.ArrayList<Float> list, float x, float y, float z, float u, float v) {
        list.add(x);
        list.add(y);
        list.add(z);
        list.add(u);
        list.add(v);
    }


    public void render() {
        org.lwjgl.opengl.GL30.glBindVertexArray(this.vaoID);
        org.lwjgl.opengl.GL11.glDrawArrays(org.lwjgl.opengl.GL11.GL_TRIANGLES, 0, this.vertexCount);
        org.lwjgl.opengl.GL30.glBindVertexArray(0);
    }

    public void cleanup(){
        glDeleteVertexArrays(vaoID);
        glDeleteBuffers(vboID);
    }

    public byte[][][] getBlocks() {
        return blocks;
    }
    public int getChunkZ(){return this.chunkZ;}
    public int getChunkX(){return chunkX;}
}
