#version 330 core
in vec2 fragmentTexCoords; // Riceve le coordinate U,V interpolate dal Vertex Shader

out vec4 fragColor;

// Questa variabile rappresenta lo slot della texture caricata in Java
uniform sampler2D textureSampler;

void main() {
    // texture() campiona l'esatto pixel dall'immagine in base alle coordinate U,V correnti
    fragColor = texture(textureSampler, fragmentTexCoords);
}
