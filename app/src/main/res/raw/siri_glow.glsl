uniform float2 resolution;
uniform float time;
uniform float intensity;

// --- Utils ----------------------------------------------------------------

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float ux = f.x * f.x * (3.0 - 2.0 * f.x);
    float uy = f.y * f.y * (3.0 - 2.0 * f.y);

    return mix(
    mix(hash(i), hash(i + float2(1.0, 0.0)), ux),
    mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), ux),
    uy
    );
}

// --- Edge distance --------------------------------------------------------

float edgeDist(float2 uv) {
    float dx = min(uv.x, 1.0 - uv.x);
    float dy = min(uv.y, 1.0 - uv.y);
    return min(dx, dy);
}

// --- Traveling particles --------------------------------------------------

float particles(float2 uv, float t) {
    float result = 0.0;

    for (int i = 0; i < 1; i++) {
        float fi = float(i);
        float offset = fi * 0.27 + hash(float2(fi, 0.0));
        float speed = 0.13;

        float cycle = fract(t * speed + offset);
        float perimPos = cycle * 5.0;

        // soft appear and soft disappear
        float fadeIn  = smoothstep(0.00, 0.04, cycle);
        float fadeOut = 1.0 - smoothstep(0.94, 1.00, cycle);
        float life = fadeIn * fadeOut;
        float2 particlePos;
        float2 dir;
        float2 perp;

        if (perimPos < 1.0) {
            // bottom edge, moving right
            particlePos = float2(perimPos, 0.0);
            dir  = float2(1.0, 0.0);
            perp = float2(0.0, 1.0);
        } else if (perimPos < 2.0) {
            // right edge, moving up
            particlePos = float2(1.0, perimPos - 1.0);
            dir  = float2(0.0, 1.0);
            perp = float2(1.0, 0.0);
        } else if (perimPos < 3.0) {
            // top edge, moving left
            particlePos = float2(1.0 - (perimPos - 2.0), 1.0);
            dir  = float2(-1.0, 0.0);
            perp = float2(0.0, 1.0);
        } else {
            // left edge, moving down
            particlePos = float2(0.0, 1.0 - (perimPos - 3.0));
            dir  = float2(0.0, -1.0);
            perp = float2(1.0, 0.0);
        }

        float2 delta = float2(
        uv.x - particlePos.x,
        uv.y - particlePos.y
        );

        // distance along motion and across motion
        float along  = dot(delta, dir);
        float across = dot(delta, perp);

        // in front of particle
        float ahead = max(along, 0.0);

        // behind particle = tail
        float behind = max(-along, 0.0);

        // knobs
        float width   = 0.010;                           // trail thickness
        float headLen = 0.045;                           // short bright front
        float tailLen = 0.26 + sin(t * 1.6 + fi) * 0.025; // longer back tail

        float crossFade = exp(-(across * across) / (width * width));

        float head = exp(-(ahead * ahead) / (headLen * headLen));
        float tail = exp(-(behind * behind) / (tailLen * tailLen));

        // slower fade at very end of tail
        tail = pow(tail, 0.42);

        // masks so front and back behave differently
        float headMask = step(0.0, along);
        float tailMask = 1.0 - headMask;

        // small bright core at the particle itself
        float core = exp(-dot(delta, delta) / (0.022 * 0.022)) * 0.26;

        float trail =
        crossFade *
        (head * headMask * 0.90 + tail * tailMask * 0.95);

        result += (core + trail) * life;
    }

    return result;
}

// --- Smooth edge glow -----------------------------------------------------

float edgeGlow(float2 uv) {
    float dist = edgeDist(uv);

    float core = 1.0 - smoothstep(0.0, 0.045, dist);
    float mid  = 1.0 - smoothstep(0.0, 0.11, dist);
    float soft = 1.0 - smoothstep(0.0, 0.22, dist);

    return core * 1.15 + mid * 0.55 + soft * 0.28;
}

// --- Ambient edge wave ----------------------------------------------------

float ambientWave(float2 uv, float t) {
    float breath = sin(t * 0.8) * 0.5 + 0.5;
    float perlin = noise(float2(uv.x * 6.0 + t * 0.2, uv.y * 6.0 - t * 0.15));
    return (breath * 0.4 + 0.6) * (perlin * 0.3 + 0.7);
}

// --- Color palette --------------------------------------------------------

float3 iosGradient(float t) {
    float3 c1 = float3(0.08, 0.86, 1.00); // cyan
    float3 c2 = float3(0.28, 0.58, 1.00); // blue
    float3 c3 = float3(0.62, 0.34, 1.00); // purple
    float3 c4 = float3(1.00, 0.34, 0.78); // pink
    float3 c5 = float3(1.00, 0.44, 0.52); // warm magenta/red

    t = fract(t);

    if (t < 0.25) {
        return mix(c1, c2, t / 0.25);
    } else if (t < 0.50) {
        return mix(c2, c3, (t - 0.25) / 0.25);
    } else if (t < 0.75) {
        return mix(c3, c4, (t - 0.50) / 0.25);
    } else {
        return mix(c4, c5, (t - 0.75) / 0.25);
    }
}

// --- Main -----------------------------------------------------------------

half4 main(float2 fragCoord) {
    float2 uv = float2(fragCoord.x / resolution.x, fragCoord.y / resolution.y);
    float t = time;

    float edge = edgeGlow(uv);
    float ptcls = particles(uv, t);
    float ambient = ambientWave(uv, t);

    float brightness = edge * ambient + ptcls * 1.15;

    // base animated color
    float n1 = noise(float2(uv.x * 3.0 + t * 0.05, uv.y * 3.0 - t * 0.04));
    float n2 = noise(float2(uv.x * 7.0 - t * 0.07, uv.y * 7.0 + t * 0.06));
    float colorT = t * 0.055 + n1 * 0.18 + n2 * 0.10;

    float3 baseCol = iosGradient(colorT);
    float3 edgeCol = iosGradient(colorT + 0.12);
    float3 particleCol = iosGradient(colorT + 0.30);
    float3 shimmerCol = iosGradient(colorT + 0.48);

    // richer colored base
    float edgeMix = clamp(edge * 0.55, 0.0, 1.0);
    float3 col = mix(baseCol, edgeCol, edgeMix);

    // colored particles instead of white particles
    col = float3(
    col.r + particleCol.r * ptcls * 0.38,
    col.g + particleCol.g * ptcls * 0.38,
    col.b + particleCol.b * ptcls * 0.38
    );

    // colored shimmer instead of white shimmer
    float shimmer = noise(float2(uv.x * 25.0 + t * 1.5, uv.y * 25.0 - t * 1.1));
    shimmer = pow(shimmer, 6.0) * 0.18;

    col = float3(
    col.r + shimmerCol.r * shimmer,
    col.g + shimmerCol.g * shimmer,
    col.b + shimmerCol.b * shimmer
    );

    // brightness
    float brightMult = 0.78 + brightness * 0.42;
    float bloomMult = 1.0 + brightness * 0.16;

    col = float3(
    col.r * brightMult * bloomMult,
    col.g * brightMult * bloomMult,
    col.b * brightMult * bloomMult
    );

    // alpha
    float alphaRaw = clamp((edge * 0.78 + ptcls * 1.00) * intensity, 0.0, 1.0);
    float alpha = pow(alphaRaw, 0.60);

    // premultiply
    col = float3(
    clamp(col.r, 0.0, 1.0) * alpha,
    clamp(col.g, 0.0, 1.0) * alpha,
    clamp(col.b, 0.0, 1.0) * alpha
    );

    return half4(col, alpha);
}