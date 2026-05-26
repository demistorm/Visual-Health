## Version 2.0.0

**Additions:**
- Completely reworked the damage rendering. Instead of drawing a new renderlayer over the mob with a dynamic damage
texture, the mod now grabs the mob's original texture and composites the damage onto it. This massively improves
compatibility with many mods and resourcepacks!
- Added support for [Corpse](https://modrinth.com/mod/corpse) (will now show player damage texture on corpses)
- Added support for [Physics Mod](https://modrinth.com/mod/physicsmod) ragdolls
- New option to disable damage from specific entities in the Entity Overrides menu
- Damage tier count can now be changed from the default 5
- New Extras config screen with additional options
- Per-player skin sampling damage tint option added (Extras screen) for unique player colors
- New color picker UI in the Entity Overrides screen to select for CUSTOM/EMISSIVE tints!
- Option to render damage on only the player (Extras screen)
- Added **Enderdragon** emissive damage!

**Changes:**
- Generic/bruise damage now appears on the base texture instead of emissive renderlayer
- Renamed "Color Overrides" to "Entity Overrides" to be more specific
- Increased per-tier wound count
- Removed EMF/ETF optional dependencies
- Tweaked slime and magma cube damage colors
- Added random opacity for wounds (gives a bit more of a natural look)
- Redesigned generic damage to be more bruise-like with its color now a darker shade of the damage tint instead of 
that brown (new textures and less opacity too!)
- Now only applies damage to fully opaque parts of the mob texture for consistent visuals
- Updated the Entity Overrides button to emphasize that player damage can also be modified

**Fixes:**
- Red override color appearing blue
- Baby mob damage pixel scale wrong (26.1+)
- Damage now shows properly on unsheared sheep
- Modded weapons causing generic damage
- Copper golem showing damage (1.21.10+)

*FYI: Emissive rendering still uses the v1 RenderLayer method! This is because emissive rendering requires a certain
renderlayer type and rendering the whole texture like that would look very wrong. This affects endermen and any mobs
with EMISSIVE overrides.*

### Version 1.1.0

- Fixed isEmissive overrides on 1.21.1 (no damage shown on Endermen)
- Fixed mod not working with shaders on 1.21.1
- Fixed cow/pig texture issues on 1.20.1/1.21.1
- Added custom entity damage overrides! Within the config menu, add entity ids and specify a custom hex color for the damage color to use!
- Adds support for 26.1 :D

### Version 1.0.1

- Fixed null crash with Skin Shuffle mod installed
- Added 1.21.1/1.20.1 versions
- Fixed debug log spam

## Version 1.0.0

- Initial release