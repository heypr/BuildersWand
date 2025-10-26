# BuildersWand

Introducing BuildersWand! A free, open-source solution that gives you, and your players, the power to build quickly without breaking a sweat.

Relevant commands:
- /givewand <wand> (Aliases: /gw, /getwand) | Gives you a wand with the given id (/givewand 1 would give you the wand with an id of 1, as seen below).
- /reloadbuilderswand (Aliases: /rbw, /reloadbw, /reloadwands) | Reloads the plugin, grabbing all changes from the configuration file and loading it into memory. All wands are updated, including ones currently in use by players.

An example configuration has been given below. If you have any questions, don't hesitate to contact me through my support Discord: https://discord.gg/Drgk3CxrtV

For anyone wondering, this is (moreorless) a spiritual successor to the abandoned BuildersWand Premium resource: https://www.spigotmc.org/resources/abandoned-builders-wand-premium-building-util-api-configurable-1-8-1-20-4.105953/.

Additionally, feel free to contribute through the GitHub repository (if possible, please ask me first so that we can hash out a good plan for your implementation first).

# API

BuildersWand *does* feature an API, but currently there is no documentation nor hosted API jar that can be depended on. **If you are a plugin author and looking for an implementable API, *please* contact me via my support Discord above and let me know that there is some level of interest.**

# Default Configuration
```yaml
# this isn't required for the build wand to work, but it does reduce the amount of fired events when placing blocks through the wand
# set to false if you want to handle block place event cancellations yourself and don't want extra events fired
# set to true if you want to allow other plugins to handle block place event cancellations (like WorldGuard or SuperiorSkyblock)
fireWandBlockPlaceEvent: true
# this isn't required for the build wand to work, but it does reduce the amount of fired events when using the wand
# set to false if you don't want to add extra logic to wand usage, ideal for servers with a lot of players
fireWandPreviewEvent: true
# This can be disabled if you want blocks to be placed instantly.
# If you notice a performance impact definitely enable this and adjust the maxBlocksPerTick value.
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
    maxRayTraceDistance: 16 # max distance that the plugin will search for a block in a given direction
    consumeItems: true # whether the wand should consume items from the player's inventory when placing blocks
    generatePreviewOnMove: false # whether to generate a preview (small white particles) of where blocks will be placed when the player moves
    durability:
      amount: 100
      enabled: true
      text: "&3Durability: {durability}"
    cooldown: 2 # in seconds
    craftable: false # whether this wand can be used in crafting recipes as an ingredient
    blockedMaterials:
      - BEDROCK
      - BARRIER
    craftingRecipe:
      enabled: false
      shape: # if this is larger than 3x3, an error will be thrown
        - " E "
        - " S "
        - " S "
      ingredients: # ensure the ingredients listed here are valid materials
        E: EMERALD
        S: STICK
```
