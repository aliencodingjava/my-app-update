uniform float2 resolution;
uniform float time;
uniform float intensity;
uniform float cornerRadius;

// Adapted from the linked SwiftUI prototype idea:
// animated mesh-gradient color field, clipped to a rounded-rectangle edge mask,
// plus a bright blurred rim.

float sinRange(float low, float high, float offset, float scale, float t) {
    float amp = (high - low) * 0.5;
    float mid = (high + low) * 0.5;
    return mid + amp * sin(scale * t + offset);
}

float roundedRectSignedDistance(float2 fragCoord, float radius) {
    float2 halfSize = resolution * 0.5;
    float2 p = fragCoord - halfSize;
    float2 q = abs(p) - (halfSize - float2(radius, radius));
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

float3 addMeshPoint(float2 uv, float2 point, float3 color, float radius, inout float weight) {
    float2 delta = uv - point;
    float d2 = dot(delta, delta);
    float w = exp(-d2 / (radius * radius));
    weight += w;
    return color * w;
}

float3 meshColor(float2 uv, float t) {
    float weight = 0.0;
    float3 col = float3(0.0, 0.0, 0.0);

    float3 yellow = float3(1.00, 0.86, 0.00);
    float3 purple = float3(0.74, 0.00, 1.00);
    float3 indigo = float3(0.12, 0.15, 1.00);
    float3 orange = float3(1.00, 0.48, 0.00);
    float3 red = float3(1.00, 0.00, 0.24);
    float3 blue = float3(0.00, 0.52, 1.00);
    float3 green = float3(0.00, 1.00, 0.28);
    float3 mint = float3(0.00, 1.00, 0.88);
    float3 ice = float3(0.96, 1.00, 1.00);

    // A 3x3 animated mesh, following the same spirit as MeshGradientView.swift.
    col += addMeshPoint(uv, float2(0.00, 0.00), yellow, 0.54, weight);
    col += addMeshPoint(uv, float2(0.50, 0.00), purple, 0.54, weight);
    col += addMeshPoint(uv, float2(1.00, 0.00), indigo, 0.54, weight);

    col += addMeshPoint(uv, float2(
        sinRange(-0.20, 0.20, 0.439, 0.342, t),
        sinRange(0.30, 0.70, 3.420, 0.984, t)
    ), orange, 0.48, weight);

    col += addMeshPoint(uv, float2(
        sinRange(0.18, 0.82, 0.239, 0.084, t),
        sinRange(0.20, 0.80, 5.210, 0.242, t)
    ), red, 0.48, weight);

    col += addMeshPoint(uv, float2(
        sinRange(0.80, 1.20, 0.939, 0.084, t),
        sinRange(0.40, 0.80, 0.250, 0.642, t)
    ), blue, 0.48, weight);

    col += addMeshPoint(uv, float2(
        sinRange(-0.15, 0.20, 1.439, 0.442, t),
        sinRange(0.88, 1.20, 3.420, 0.984, t)
    ), indigo, 0.50, weight);

    col += addMeshPoint(uv, float2(
        sinRange(0.30, 0.62, 0.339, 0.784, t),
        sinRange(0.86, 1.14, 1.220, 0.772, t)
    ), green, 0.50, weight);

    col += addMeshPoint(uv, float2(
        sinRange(0.82, 1.20, 0.939, 0.056, t),
        sinRange(0.82, 1.18, 0.470, 0.342, t)
    ), mint, 0.50, weight);

    col = col / max(weight, 0.001);

    float gray = dot(col, float3(0.299, 0.587, 0.114));
    col = mix(float3(gray, gray, gray), col, 2.40);
    return clamp(mix(col, ice, 0.02), 0.0, 1.0);
}

float2 rotateUv(float2 uv, float angle) {
    float aspect = resolution.x / max(resolution.y, 1.0);
    float2 p = uv - 0.5;
    p.x *= aspect;

    float s = sin(angle);
    float c = cos(angle);
    p = float2(p.x * c - p.y * s, p.x * s + p.y * c);

    p.x /= aspect;
    return p + 0.5;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float t = time * 1.75;

    float radius = clamp(cornerRadius, 0.0, min(resolution.x, resolution.y) * 0.5);
    float sd = roundedRectSignedDistance(fragCoord, radius);
    float inside = 1.0 - smoothstep(0.0, 2.0, sd);
    float d = abs(sd);

    // Prototype-like mask: bright stroke, blurred stroke, and subtle inner spill.
    float whiteStroke = 1.0 - smoothstep(0.0, 12.5, d);
    float colorStroke = 1.0 - smoothstep(0.5, 44.0, d);
    float glow = 1.0 - smoothstep(8.0, 132.0, d);
    float innerSpill = (1.0 - smoothstep(16.0, 172.0, -sd)) * smoothstep(0.0, 16.0, -sd);

    float2 meshUv = rotateUv(uv, time * 0.22);
    float2 meshUvSlow = rotateUv(uv, -time * 0.10) * 0.92 + float2(0.04, 0.05);
    float3 col = meshColor(meshUv, t);
    float3 shifted = meshColor(meshUvSlow, t + 1.7);
    col = mix(col, shifted, 0.26);

    float mask = whiteStroke * 0.68 + colorStroke * 0.82 + glow * 0.36 + innerSpill * 0.12;
    float breathing = 0.94 + 0.06 * sin(time * 0.72);

    col *= 1.18 + (whiteStroke + colorStroke) * 0.92;
    col = mix(col, float3(1.0, 0.98, 1.0), whiteStroke * 0.10);

    float alpha = clamp(pow(clamp(mask * breathing, 0.0, 1.0), 0.52) * (0.90 + 0.05 * intensity), 0.0, 0.95) * inside;
    col = clamp(col, 0.0, 1.0) * alpha;

    return half4(col, alpha);
}
