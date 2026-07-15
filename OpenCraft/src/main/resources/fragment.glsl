#version 330 core
in vec2 fragmentTexCoords;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform int useTexture;
uniform vec4 guiColor;

void main() {
    if (useTexture == 1) {
        fragColor = texture(textureSampler, fragmentTexCoords);
    } else {
        fragColor =  guiColor;
    }
}

