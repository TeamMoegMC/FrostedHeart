// Added only to Frosted Heart's capture program, after the original chunk helpers.
layout(location=4) in uint fhOwner;
uniform bool fhCaptureEnabled;
uniform ivec3 fhRegionToTextureOrigin;
uniform isampler3D fhTemperatureTexture;
flat out int fhTemperature;

void fhReadTemperature() {
    fhTemperature = -32768;
    if (!fhCaptureEnabled || (fhOwner & 4096u) == 0u) return;
    ivec3 blockLocal = ivec3(fhOwner & 15u, (fhOwner >> 8u) & 15u, (fhOwner >> 4u) & 15u);
    ivec3 cell = fhRegionToTextureOrigin + ivec3(_get_relative_chunk_coord(_draw_id)) * 16 + blockLocal;
    if (all(greaterThanEqual(cell, ivec3(0))) && all(lessThan(cell, ivec3(144))))
        fhTemperature = texelFetch(fhTemperatureTexture, cell, 0).r;
}
