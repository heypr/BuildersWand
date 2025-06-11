# BuildersWand

Introducing BuildersWand! A free, open-source solution that gives you, and your players, the power to build quickly without breaking a sweat. An example configuration has been given below. If you have any questions, don't hesitate to contact me through my support Discord: https://discord.gg/Drgk3CxrtV

Additionally, feel free to contribute through the GitHub repository (if possible, please ask me first so that we can hash out a good plan for your implementation first).

```yaml
placementQueue:
  enabled: true
  maxBlocksPerTick: 20 # max blocks to place per tick
wands:
  1:
    name: "&3Builders Wand"
    material: "BLAZE_ROD"
    lore:
      - "&7A powerful wand for building!"
      - "&7Use it wisely."
    maxSize: 8 # max block search & placement size
    maxSizeText: "&3Max Size: {maxSize}"
    maxRayTraceDistance: 16
    consumeItems: true
    generatePreviewOnMove: false
    durability:
      amount: 100
      enabled: true
      text: "&3Durability: {durability}"
    cooldown: 2 # in seconds
    craftable: false
    blockedMaterials:
      - BEDROCK
      - BARRIER
```
