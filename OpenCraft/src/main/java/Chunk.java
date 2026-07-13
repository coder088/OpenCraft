import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

public class Chunk {
    public int chunkX;
    public int chunkZ;
    private int vaoID;
    private int vboID;
    private int vertexCount;
    private static final int CHUNK_WIDTH = 16;
    private static final int CHUNK_HEIGHT = 64;
    private static final int CHUNK_LENGTH = 16;
    private static final int CHUNK_DATA_SIZE = CHUNK_WIDTH * CHUNK_HEIGHT * CHUNK_LENGTH;

    // L'array tridimensionale che contiene gli ID dei blocchi
    private byte[][][] blocks;
    private boolean dirty;
    private boolean saveQueued;
    private long modificationVersion;
    private final Object saveLock = new Object();

    public Chunk(int cx, int cz,World world) {
        this.chunkX = cx;
        this.chunkZ = cz;
        this.blocks = new byte[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_LENGTH];
        File file = getSaveFile();
        if (file.exists() && file.length() == CHUNK_DATA_SIZE) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                for (int x = 0; x < CHUNK_WIDTH; x++) {
                    for (int y = 0; y < CHUNK_HEIGHT; y++) {
                        for (int z = 0; z < CHUNK_LENGTH; z++) {
                            int data = fis.read();
                            if (data == -1) {
                                throw new java.io.IOException("File incompleto!");
                            }
                            this.blocks[x][y][z] = (byte) data;
                        }
                    }
                }
            }
            catch (IOException e) {
                System.out.println("Salvataggio corrotto per il chunk " + chunkX + "," + chunkZ + ". Genero nuovo terreno...");
                for (int x = 0; x < CHUNK_WIDTH; x++) {
                    for (int z = 0; z < CHUNK_LENGTH; z++) {
                        int altezzaSuperficie;
                        int worldX = chunkX * 16 + x;
                        int worldZ = chunkZ * 16 + z;
                        float valoreNoise = world.getNoiseAt(worldX, worldZ);
                        altezzaSuperficie = 20 + (int) (valoreNoise * 15f);
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
                dirty = true;
            }

        }
        else {
            for (int x = 0; x < CHUNK_WIDTH; x++) {
                for (int z = 0; z < CHUNK_LENGTH; z++) {
                    int altezzaSuperficie;
                    int worldX = chunkX * 16 + x;
                    int worldZ = chunkZ * 16 + z;
                    float valoreNoise = world.getNoiseAt(worldX, worldZ);
                    altezzaSuperficie = 20 + (int) (valoreNoise * 15f);
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
            // Ripara automaticamente i file vecchi/incompleti al primo salvataggio.
            dirty = file.exists();
        }
    }


    public void generateMesh(World world) {
        // Una rigenerazione sostituisce la vecchia mesh: senza questa pulizia ogni
        // aggiornamento lascia VAO e VBO nella VRAM fino al crash del driver.
        cleanup();

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
        if (vaoID != 0) {
            glDeleteVertexArrays(vaoID);
            vaoID = 0;
        }
        if (vboID != 0) {
            glDeleteBuffers(vboID);
            vboID = 0;
        }
        vertexCount = 0;
    }

    public boolean hasMesh() {
        return vaoID != 0;
    }

    public byte[][][] getBlocks() {
        return blocks;
    }
    public int getChunkZ(){return this.chunkZ;}
    public int getChunkX(){return chunkX;}

    public void setBlockLocal(int localX, int y, int localZ, byte newBlockType,World world) {
        if (localX < 0 || localX >= CHUNK_WIDTH || y < 0 || y >= CHUNK_HEIGHT || localZ < 0 || localZ >= CHUNK_LENGTH) {
            return;
        }
        synchronized (saveLock) {
            if (this.blocks[localX][y][localZ] == newBlockType) {
                return;
            }
            this.blocks[localX][y][localZ] = newBlockType;
            dirty = true;
            modificationVersion++;
        }
        this.generateMesh(world);
        saveChunkDuringOtherThread();
    }

    private File getSaveFile() {
        return new File("world/chunk_" + chunkX + "_" + chunkZ + ".dat");
    }

public void saveChunkDuringOtherThread(){
        synchronized (saveLock) {
            if (!dirty || saveQueued) {
                return;
            }
            saveQueued = true;
        }
        byte[][][] blocksCopy = new byte[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_LENGTH];
        long savedVersion;
        synchronized (saveLock) {
            for(int x = 0; x < CHUNK_WIDTH; x++){
                for(int y = 0; y < CHUNK_HEIGHT; y++){
                    System.arraycopy(this.blocks[x][y],0,blocksCopy[x][y],0,CHUNK_LENGTH);
                }
            }
            savedVersion = modificationVersion;
        }
        int cx = this.chunkX;
        int cz = this.chunkZ;
        try {
        Main.saveExecutor.execute(() ->{
            java.io.File directory = new java.io.File("world");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, "chunk_" + cx + "_" + cz + ".dat");
            File temporaryFile = new File(directory, file.getName() + ".tmp");
            boolean saved = false;
            try {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(temporaryFile)) {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 64; y++) {
                        for (int z = 0; z < 16; z++) {
                            fos.write(blocksCopy[x][y][z]);
                        }
                    }
                }
                fos.getFD().sync();
            }
                try {
                    Files.move(temporaryFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(temporaryFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                saved = true;
            } catch (Exception e) {
                System.err.println("Errore nel thread di salvataggio del chunk " + cx + "," + cz + ": " + e.getMessage());
            } finally {
                boolean saveAgain;
                synchronized (saveLock) {
                    saveQueued = false;
                    if (saved && modificationVersion == savedVersion) {
                        dirty = false;
                    }
                    saveAgain = saved && dirty;
                }
                if (saveAgain) {
                    saveChunkDuringOtherThread();
                }
            }
        });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            synchronized (saveLock) {
                saveQueued = false;
            }
        }
        }
}

