import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

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
            try (FileInputStream fis = new FileInputStream(file)) {
                for (int x = 0; x < CHUNK_WIDTH; x++) {
                    for (int y = 0; y < CHUNK_HEIGHT; y++) {
                        for (int z = 0; z < CHUNK_LENGTH; z++) {
                            int data = fis.read();
                            if (data == -1) {
                                throw new java.io.IOException("Incompleate file!");
                            }
                            this.blocks[x][y][z] = (byte) data;
                        }
                    }
                }
            }
            catch (IOException e) {
                System.out.println("corrupted save file for chunk " + chunkX + "," + chunkZ + ". Loading new terrain...");
                for (int x = 0; x < CHUNK_WIDTH; x++) {
                    for (int z = 0; z < CHUNK_LENGTH; z++) {
                        int surfaceHeight;
                        int worldX = chunkX * 16 + x;
                        int worldZ = chunkZ * 16 + z;
                        float noiseValue = world.getNoiseAt(worldX, worldZ);
                        surfaceHeight = 20 + (int) (noiseValue * 15f);
                        for (int y = 0; y < CHUNK_HEIGHT; y++) {
                            if (y == surfaceHeight) {
                                blocks[x][y][z] = 1; //Only the first block is grass
                            } else if (y < surfaceHeight) {
                                blocks[x][y][z] = 2; //below is rock
                            } else {
                                blocks[x][y][z] = 0; //above its air
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
                    float noiseValue = world.getNoiseAt(worldX, worldZ);
                    altezzaSuperficie = 20 + (int) (noiseValue * 15f);
                    for (int y = 0; y < CHUNK_HEIGHT; y++) {
                        if (y == altezzaSuperficie) {
                            blocks[x][y][z] = 1;
                        } else if (y < altezzaSuperficie) {
                            blocks[x][y][z] = 2;
                        } else {
                            blocks[x][y][z] = 0;
                        }
                    }
                }
            }
            dirty = file.exists();
        }
    }


    public void generateMesh(World world) {
        cleanup();
        ArrayList<Float> vertexData = new ArrayList<>();

     //cheks the entire chunk volume
        for (int x = 0; x < CHUNK_WIDTH; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_LENGTH; z++) {

                    byte blockType = blocks[x][y][z];
                    if (blockType == 0) continue; //if its air we skip
                    //world's coords for that specific block
                    float worldX = (this.chunkX * 16) + x;
                    float worldY = y;
                    float worldZ = (this.chunkZ * 16) + z;

                    //takes care of texture bleading
                    float epsilon = 0.005f;
                    float vMin = 0.0f + epsilon;
                    float vMax = 1.0f - epsilon;


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


                    float sideUMin, sideUMax;
                    if (blockType == 1) {

                        sideUMin = 0.335f  + epsilon;
                        sideUMax = 0.495f - epsilon;
                    } else {

                        sideUMin = 0.169f  + epsilon;
                        sideUMax = 0.329f - epsilon;
                    }

                    if (world.getBlockAt(worldX, worldY, worldZ + 1.0f) == 0) {
                        addVertex(vertexData, worldX,        worldY,        worldZ + 1.0f, sideUMin, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, sideUMax, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, sideUMax, vMax);

                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, sideUMax, vMax);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ + 1.0f, sideUMin, vMax);
                        addVertex(vertexData, worldX,        worldY,        worldZ + 1.0f, sideUMin, vMin);
                    }

                    if (world.getBlockAt(worldX + 1.0f, worldY, worldZ) == 0) {
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, sideUMin, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        sideUMax, vMin);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        sideUMax, vMax);

                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        sideUMax, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ + 1.0f, sideUMin, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ + 1.0f, sideUMin, vMin);
                    }

                    if (world.getBlockAt(worldX, worldY, worldZ - 1.0f) == 0) {
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        sideUMin, vMin);
                        addVertex(vertexData, worldX,        worldY,        worldZ,        sideUMax, vMin);
                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        sideUMax, vMax);

                        addVertex(vertexData, worldX,        worldY + 1.0f, worldZ,        sideUMax, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY + 1.0f, worldZ,        sideUMin, vMax);
                        addVertex(vertexData, worldX + 1.0f, worldY,        worldZ,        sideUMin, vMin);
                    }

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
        this.vertexCount = vertexData.size() / 5;
        float[] finalVertices = new float[vertexData.size()];
        for (int i = 0; i < vertexData.size(); i++) {
            finalVertices[i] = vertexData.get(i);
        }


        FloatBuffer vertexBuffer = memAllocFloat(finalVertices.length);
        vertexBuffer.put(finalVertices).flip();

        this.vaoID = glGenVertexArrays();
        glBindVertexArray(this.vaoID);

        this.vboID =glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vboID);
       glBufferData(GL_ARRAY_BUFFER, vertexBuffer,GL_STATIC_DRAW);
        int stride = 5 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2,GL_FLOAT, false, stride, 3 * Float.BYTES);
      glEnableVertexAttribArray(1);

       glBindBuffer(GL_ARRAY_BUFFER, 0);
      glBindVertexArray(0);

        memFree(vertexBuffer);
    }

    private void addVertex(ArrayList<Float> list, float x, float y, float z, float u, float v) {
        list.add(x);
        list.add(y);
        list.add(z);
        list.add(u);
        list.add(v);
    }


    public void render() {
        glBindVertexArray(this.vaoID);
        glDrawArrays(GL_TRIANGLES, 0, this.vertexCount);
        glBindVertexArray(0);
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
            try (FileOutputStream fos = new FileOutputStream(temporaryFile)) {
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
                System.err.println("error in the thread while loading chunk " + cx + "," + cz + ": " + e.getMessage());
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

