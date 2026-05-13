uniform float2 resolution;
uniform float time;
uniform float intensity;

float noise(float2 p) {
    return sin(p.x) * sin(p.y);
}

float fbm(float2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p * 3.0);
    f += 0.2500 * noise(p * 6.0);
    f += 0.1250 * noise(p * 12.0);
    f += 0.0625 * noise(p * 24.0);
    return f;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;

    // distance to nearest edge
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));

    // edge falloff (controls how far glow enters screen)
    float glow = smoothstep(0.0, 0.18, edge);

    // animated noise field
    float n = fbm(uv * 4.0 + time * 0.6);

    // color palette (Siri-like)
    float3 base = float3(
        0.2 + 0.8 * n,
        0.3 + 0.7 * n,
        1.0
    );

    // chromatic separation
    float3 rgb = base * glow * intensity;

    return half4(rgb, glow * intensity);
}
