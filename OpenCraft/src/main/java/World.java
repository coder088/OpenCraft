import java.util.concurrent.ConcurrentHashMap;

public class World {
    // La chiave è una String (es. "x,z"), il valore è l'oggetto Chunk corrispondente
    public  java.util.Map<String, Chunk> chunks = new java.util.concurrent.ConcurrentHashMap<>();
    private FastNoiseLite noise;
    private static final int DEFAULT_RENDER_DISTANCE = 10;
    private static final int CHUNKS_TO_MESH_PER_UPDATE = 2;

    private final java.util.ArrayDeque<Chunk> chunksWaitingForMesh = new java.util.ArrayDeque<>();
    private int renderDistance = DEFAULT_RENDER_DISTANCE;

    public World() {
        this.noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        noise.SetFractalType(FastNoiseLite.FractalType.FBm);
        noise.SetFrequency(0.005f);
        noise.SetFractalOctaves(4);
        this.chunks = new ConcurrentHashMap<>();
    }

    public void update(org.joml.Vector3f playerPosition) {
        int currentChunkX = (int) Math.floor(playerPosition.x / 16.0f);
        int currentChunkZ = (int) Math.floor(playerPosition.z / 16.0f);
        java.util.ArrayList<Chunk> newChunks = new java.util.ArrayList<>();
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                int targetX = currentChunkX + x;
                int targetZ = currentChunkZ + z;
                String key = targetX + "," + targetZ;

                if (!chunks.containsKey(key)) {
                    Chunk newChunk = new Chunk(targetX, targetZ,this);
                    chunks.put(key, newChunk);
                    newChunks.add(newChunk);
                }
            }
        }

        // Prima i chunk vicini, così il terreno attorno al giocatore compare subito.
        newChunks.sort(java.util.Comparator.comparingInt(chunk ->
                Math.abs(chunk.getChunkX() - currentChunkX) + Math.abs(chunk.getChunkZ() - currentChunkZ)));
        chunksWaitingForMesh.addAll(newChunks);

        // Usa lo stesso raggio per caricare e scaricare.
        int maxDistance = renderDistance;
        chunks.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getValue();
            int distanceX = Math.abs(chunk.chunkX - currentChunkX);
            int distanceZ = Math.abs(chunk.chunkZ - currentChunkZ);
            if (distanceX > maxDistance || distanceZ > maxDistance) {
                chunk.saveChunkDuringOtherThread();
                chunk.cleanup();
                return true;
            }
            return false;
        });

        // Costruisci poche mesh per frame, evitando picchi di CPU, heap e VRAM.
        for (int generated = 0; generated < CHUNKS_TO_MESH_PER_UPDATE && !chunksWaitingForMesh.isEmpty();) {
            Chunk chunk = chunksWaitingForMesh.removeFirst();
            String key = chunk.getChunkX() + "," + chunk.getChunkZ();
            if (chunks.get(key) != chunk) {
                continue; // il chunk era uscito dal raggio mentre era in coda
            }

            chunk.generateMesh(this);
            generated++;
            updateChunkMeshAt(chunk.getChunkX() + 1, chunk.getChunkZ());
            updateChunkMeshAt(chunk.getChunkX() - 1, chunk.getChunkZ());
            updateChunkMeshAt(chunk.getChunkX(), chunk.getChunkZ() + 1);
            updateChunkMeshAt(chunk.getChunkX(), chunk.getChunkZ() - 1);
        }
    }

    public void render() {
        // Cicla attraverso tutti i chunk salvati nella mappa e chiama il loro metodo render
        for (Chunk chunk : chunks.values()) {
            if (chunk.hasMesh()) {
                chunk.render();
            }
        }
    }
    // Trova il tipo di blocco a qualsiasi coordinata X, Y, Z del mondo
    public byte getBlockAt(float worldX, float worldY, float worldZ) {
        if (worldY < 0 || worldY >= 64) {
            return 0;
        }

        int chunkX = (int) Math.floor(worldX / 16.0f);
        int chunkZ = (int) Math.floor(worldZ / 16.0f);

        String key = chunkX + "," + chunkZ;
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            return 0;
        }
        int localX = (int) Math.floor(worldX) % 16;
        if (localX < 0) localX += 16;

        int localZ = (int) Math.floor(worldZ) % 16;
        if (localZ < 0) localZ += 16;

        int localY = (int) worldY;

        return chunk.getBlocks()[localX][localY][localZ];
    }
    public void setBlockAt(int worldX, int worldY, int worldZ,byte blockType){
        if (worldY < 0 || worldY >= 64) return;
        int chunkX = (int) Math.floor(worldX / 16.0f);
        int chunkZ = (int) Math.floor(worldZ / 16.0f);
        String key = chunkX + "," + chunkZ;
        Chunk chunk = chunks.get(key);
        if (chunk != null) {
            int localX = worldX & 15;
            int localZ = worldZ & 15;
            chunk.setBlockLocal(localX, worldY, localZ, blockType, this);
            //checks if the broken block was at the end of the chunk,
            // if it was, reloads also the other chunk
            if(localX == 0){
                    String vicinoKey = (chunkX - 1) + "," + chunkZ;
                    Chunk chunkVicino = chunks.get(vicinoKey);
                    if (chunkVicino != null) chunkVicino.generateMesh(this);
            }
            else if (localX == 15) {
                String vicinoKey = (chunkX + 1) + "," + chunkZ;
                Chunk chunkVicino = chunks.get(vicinoKey);
                if (chunkVicino != null) chunkVicino.generateMesh(this);
            }
            if (localZ == 0) {
                String vicinoKey = chunkX + "," + (chunkZ - 1);
                Chunk chunkVicino = chunks.get(vicinoKey);
                if (chunkVicino != null) chunkVicino.generateMesh(this);
            }
            else if (localZ == 15) {
                String vicinoKey = chunkX + "," + (chunkZ + 1);
                Chunk chunkVicino = chunks.get(vicinoKey);
                if (chunkVicino != null) chunkVicino.generateMesh(this);
            }
        }
    }
    private void updateChunkMeshAt(int cx, int cz) {
        String key = cx + "," + cz;
        Chunk neighbor = chunks.get(key);
        if (neighbor != null && neighbor.hasMesh()) {
            neighbor.generateMesh(this); // Ricalcola i vertici aggiornati
        }

    }
    public float getNoiseAt(float x,float z){return this.noise.GetNoise(x,z);}

    public void setRenderDistance(int renderDistance) {
        if (renderDistance < 1) {
            throw new IllegalArgumentException("La render distance deve essere almeno 1");
        }
        this.renderDistance = renderDistance;
    }

}
