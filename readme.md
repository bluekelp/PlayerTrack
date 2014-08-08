Player Track
------------

A Minecraft server plugin for CraftBukkit.

Periodically records player location and timestamp.

Useful for lightweight grief monitoring (can confirm/deny the presence of a player in an area) if frequency and
distance settings are correct.

Can also be used for inferring where home bases, builds, and interesting resources are -- and auto-claiming discovered territories.
(External tools required for these uses.)

To keep data small, the timestamp only is updated if the player has not moved much from the last recorded observation.

On plugin startup, a task is begun to record player locations (x,y,z) 

commands:

```
    /ptrack pause
```

Pause data recording.

```
    /ptrack resume
```

Resume a previously paused series.

```
    /ptrack frequency <seconds>
```

Set the observation recording frequency to <seconds>. Default frequency is 30 seconds.


```
    /ptrack distance <blocks>
```

Set the minimum distance (in blocks) a player must move to generate a new location observation. Default is 32 blocks (2 chunks).

todo
----
- update to use UUIDs internally instead of player name

