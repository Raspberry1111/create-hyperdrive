# Create: Hyperdrive
<a href="https://mordinth.com/mod/tbd"><img alt="Static Badge" src="https://img.shields.io/badge/supported%20versions-1.21.1-brightgreen?style=for-the-badge&logo=minecraft"></a>
<a href="https://mordinth.com/mod/tbd"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/:tbd?style=for-the-badge&label=Modrinth"/></a>

Adds a Hyperdrive block, which allows for sable contraptions to teleport between dimensions

(Recommended to be used alongside the [Create: Aeronautics](https://modrinth.com/mod/create-aeronautics) mod

## Mechanics
To acquire a hyperdrive in survival, first craft the empty hyperdrive then right click on a shulker. You can also shift-right click on a shulker box (even colored ones) to put the shulker back down.

The hyperdrive by default requires 32rpm to start charging, and must be located inside a sublevel with it's target dimension set to a different one than the current (Goggles work and will give info on why it's not teleporting)

One triggering, it will enter a cooldown period. A bottle of dragon's breath can be used to immediately end the cooldown, at the cost of exhausting the shulker and making it's next charge time take longer

Using dragon's breath on a hyperdrive that is not in cooldown will infuse the shulker and make it charge quicker. If the shulker is exhausted, it will instead put it into the normal state.



<br/>

<details>
<summary>Known Issues</summary>
<h3><a href="https://github.com/hollow-egg/Dimensional-Sable">Dimensional Sable</a></h3>
Sable doesn't natively support teleporting between dimensions 
<a href="https://discord.com/channels/937435293294919690/937444160254906419/1505844222794661938">at this point in time</a>,
  so we depend on Dimensional Sable (which is an excellent mod), which adds this functionality.
  However, due to the way Sable works internally, this process is bound to be buggy:

  Sable stores the subplot on which the "real" blocks for every contraption are placed local to each dimension, likely due to differences in how certain mechanics work (beds, respawn anchors, etc).
  To counteract this, dimensional sable (and by extension this mod) first copies every block into the subplot to the target dimension, rebuilds the contraption at the corresponding coordinates, and finally deletes the original (See 

  The issue with this is that often modded blocks store block coordinates inside their nbt data that requires "fixing" to be based off their new subplots position and requires "fixing" to work correctly. Dimensional Sables applies this automatically to a couple mods but its impossible to make an exhaustive list of every block that would be broken by this.
  
</details>
