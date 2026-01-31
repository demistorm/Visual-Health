# Visual Health Damage Assets

This folder contains wound textures that get stamped onto entity textures based on their health percentage.

## Folder Structure

- **scratches/** - Light surface damage (tier 1: 60-80% health)
- **cuts/** - Medium cuts (tier 2-3: 40-60% health)
- **wounds/** - Deep gashes (tier 3-4: 20-40% health)
- **drips/** - Blood drip effects (tier 4: <20% health)

## Asset Requirements

All wound PNGs must be **greyscale** (black to white):
- White areas (255,255,255) become bright highlights
- Black areas (0,0,0) become dark shadows
- Mid greys become transitional shades

This allows the mod to tint wounds to any color while preserving natural shading.

## Technical Specs

- **Format**: PNG with alpha channel
- **Color space**: Greyscale (0-255 luminance)
- **Recommended size**: 16x16 to 32x32 pixels
- **Alpha channel**: Required for transparency/soft edges
- **Multiple variations**: Add multiple PNGs per folder for randomness

## Example Usage

Place your wound assets in the appropriate folder:
- `damage/scratches/scratch1.png`
- `damage/scratches/scratch2.png`
- `damage/cuts/cut_gash.png`
- `damage/wounds/deep_wound.png`
- `damage/drips/blood_drip.png`

The mod will randomly select and stamp these onto entity textures based on damage tier.
