# Totem Optimizer (Fabric, Minecraft 1.21.11)

Client-side mod that cuts the window where the server doesn't yet know what you're holding, and
removes "ghost totems" the moment a pop happens.

## What it does

1. **Instant hotbar sync** (`instantHotbarSync`)
   Vanilla reads the hotbar keys once per 50 ms tick and sends the new slot to the server on the
   *next* tick. If you press `2` and a crystal/anchor hits you right then, the server may still think
   you're on the old slot, so no totem pops. This mod selects the slot and sends the slot packet at the
   exact moment the key event arrives from the OS (no waiting for a frame or a tick). Scroll wheel
   switching is sent on the next frame. No duplicate packets are sent.

2. **Instant offhand swap** (`instantOffhandSwap`)
   Same idea for the swap-hands key (default `F`): the swap packet is sent when the key event
   arrives instead of on the next tick, and vanilla's duplicate swap is dropped so it only swaps once.
   If swapping ever feels wrong, set this to `false`.

3. **Predicted pop** (`predictPop`)
   When the server tells you a totem popped, the mod removes one totem from your hands right away
   (main hand first, then off hand, same as the server). The totem no longer sits in your hand
   looking usable. The server's own slot update arrives afterwards and corrects any wrong guess.

## Honest limits

No client mod can make the server act on something it hasn't received yet. With high ping the slot
packet still takes ping/2 to arrive. This mod removes the extra 0-100 ms *before* the packet is sent,
which is the part a client can control. It does not automate anything: you still press the keys.
Some servers ban modified clients, so check the rules.

## Config

`.minecraft/config/totemoptimizer.properties` (created on first launch, restart after editing):

```
instantHotbarSync=true
instantOffhandSwap=true
predictPop=true
```

## Build

Needs JDK 21. Easiest: generate the official Fabric template for 1.21.11 at
https://fabricmc.net/develop/template/ (gives you the Gradle wrapper), then copy `src/`,
`build.gradle`, `gradle.properties` over it and run:

```
./gradlew build
```

The jar is in `build/libs/` (use the one without `-sources`). Put it in `.minecraft/mods/`.
Requires Fabric Loader 0.18.1+; Fabric API is not required.

This project uses Mojang mappings (Fabric recommends them for 1.21.11).
