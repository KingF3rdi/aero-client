#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform SaturationConfig {
    float Saturation;
};

const vec3 Gray = vec3(0.2126, 0.7152, 0.0722);

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float luma = dot(c, Gray);
    fragColor = vec4(clamp(mix(vec3(luma), c, Saturation), 0.0, 1.0), 1.0);
}
