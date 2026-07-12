import java.util.concurrent.ConcurrentHashMap;

public class World {
    // La chiave è una String (es. "x,z"), il valore è l'oggetto Chunk corrispondente
    // Ricordati di importarla in alto: import java.util.concurrent.ConcurrentHashMap;
    private  java.util.Map<String, Chunk> chunks = new java.util.concurrent.ConcurrentHashMap<>();
    private FastNoiseLite noise;

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
        int renderDistance = 3;

        // Lista per ricordarci quali chunk sono appena nati
        java.util.ArrayList<Chunk> newChunksThisFrame = new java.util.ArrayList<>();

        // 1. POPOLAMENTO MAPPA: Creiamo gli oggetti dei chunk e li inseriamo NELLA MAPPA
        // Alla fine di questo doppio ciclo, TUTTI i chunk della Render Distance esistono formalmente in memoria
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                int targetX = currentChunkX + x;
                int targetZ = currentChunkZ + z;
                String key = targetX + "," + targetZ;

                if (!chunks.containsKey(key)) {
                    Chunk newChunk = new Chunk(targetX, targetZ,this);
                    chunks.put(key, newChunk);
                    newChunksThisFrame.add(newChunk);
                }
            }
        }

        // 2. GENERAZIONE DELLE MESH (EFFETTO DOMINO BLINDATO)
        // Entriamo qui solo se sono nati nuovi chunk in questo frame
        if (!newChunksThisFrame.isEmpty()) {

            // Sincronizziamo: generiamo prima la mesh di base di TUTTI i nuovi nati.
            // Quando un chunk chiama getBlockAt(), il suo vicino ora ESISTE SICURAMENTE nella mappa!
            for (Chunk chunk : newChunksThisFrame) {
                chunk.generateMesh(this);
            }

            // Ora che i nuovi hanno la mesh, forziamo ANCHE i chunk vecchi confinanti a raddrizzarsi
            // Questo abbatte i muri grigi perimetrali rimasti impressi per errore!
            for (Chunk chunk : newChunksThisFrame) {
                updateChunkMeshAt(chunk.getChunkX() + 1, chunk.getChunkZ());
                updateChunkMeshAt(chunk.getChunkX() - 1, chunk.getChunkZ());
                updateChunkMeshAt(chunk.getChunkX(),     chunk.getChunkZ() + 1);
                updateChunkMeshAt(chunk.getChunkX(),     chunk.getChunkZ() - 1);
            }
        }

        // 3. SCARICAMENTO CHUNK LONTANI
        int maxDistance = 4;
        chunks.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getValue();
            int distanceX = Math.abs(chunk.chunkX - currentChunkX);
            int distanceZ = Math.abs(chunk.chunkZ - currentChunkZ);
            if (distanceX > maxDistance || distanceZ > maxDistance) {
                chunk.cleanup();
                return true;
            }
            return false;
        });
    }

    public void render() {
        // Cicla attraverso tutti i chunk salvati nella mappa e chiama il loro metodo render
        for (Chunk chunk : chunks.values()) {
            chunk.render();
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
            return 0; // Se il chunk vicino non è ancora caricato, assumiamo sia aria per ora
        }

        // CALCOLO CORRETTO CON IL RESTO DELLA DIVISIONE (Gestisce i numeri negativi in sicurezza!)
        int localX = (int) Math.floor(worldX) % 16;
        if (localX < 0) localX += 16; // Raddrizza il valore se è negativo

        int localZ = (int) Math.floor(worldZ) % 16;
        if (localZ < 0) localZ += 16;

        int localY = (int) worldY;

        return chunk.getBlocks()[localX][localY][localZ];
    }
    private void updateChunkMeshAt(int cx, int cz) {
        String key = cx + "," + cz;
        Chunk neighbor = chunks.get(key);
        if (neighbor != null) {
            neighbor.generateMesh(this); // Ricalcola i vertici aggiornati
        }

    }
    public float getNoiseAt(float x,float z){return this.noise.GetNoise(x,z);}

}
