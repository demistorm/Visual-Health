# Visual Health 🩸
Clientside mod adding visible damage to hurt mobs and players!

Damage works in **tiers**, not based on hits. The tiers are based on how much health the mob has. The idea is that at a glance a player can have a rough idea of how much health is left. A heavily damaged creeper will only need 1 more hit whereas a lightly hurt baby zombie is going to take a much greater beating.


<details>
<summary>Tier Summary</summary>

| Tier | Health % | Condition |
|------|-------------------|-----------|
| 0 | 100% | Full health (no wounds) |
| 1 | 99% - 80% | Minor damage |
| 2 | 79% - 60% | Light damage |
| 3 | 59% - 40% | Moderate damage |
| 4 | 39% - 20% | Heavy damage |
| 5 | 19% - 0% | Critical damage |


</details>


---

## Features
### Per-Weapon Damage
Different weapons apply different damage textures depending on what was used to knock the entity's health down a tier. There are individual textures for sword, axe, trident, and spear damage. There is also a generic damage texture used for punches, fire, falls, etc.
### Entity-Specific Damage
Many mobs have specific colored damage to aid immersion. For instance, spider damage is a light blue to match real life, creeper damage is a dark green to match the grassiness, etc.
### Emissives
On Endermen, damage will glow like their eyes in the dark! (shaders supported)
### Resource Pack Support
This mod should be compatible with **most** resource packs. Tested extensively with Fresh Animations and its extensions. Also compatible with EMF model variants (like the creepers found in FA: Creepers).
### Mod Support
Should work with modded entities, as long as they register a LivingEntityRenderer. Tested with Animal Garden mobs.

## Configuration
- How *much* damage will be shown ranging from a multiplier of 10 to 100%. At 100% a low-health mob will be almost entirely covered in wounds.
- Configurable damage colors (red, black, white, only affects mobs without specific damage colors)
- Passive mob damage (defaults to true, whether to show damage on passive mobs or not, things like cows, pigs, polar bears, etc)
- Villager damage (defaults to false, only can be enabled if passive mob damage is true)

*Mod Menu is required on Fabric to change settings in-game.*

## Supported Versions
**1.20.1, 1.21.1, 1.21.11**

## Contact
For any questions/issues/ideas, feel free to contact me on [Discord](https://discord.gg/7uttzPbTGq) or [Matrix](https://matrix.to/#/#stormcommunity:matrix.org) :)
