# PlayTimeTracker

[![Build Status](https://travis-ci.org/NyaaCat/PlayTimeTracker.svg)](https://travis-ci.org/NyaaCat/PlayTimeTracker)

Playtime tracking plugin for Bukkit/Spigot

### Features
* Track playtime for every player, including:
  * Current session
  * Today
  * This week
  * This month
  * Total time
* Simple reward system
  * Recurring reward based on playtime
  * One time reward based on total time
  * Long time absence handler
  * Execute (almost any) commands for rewarding
* AFK time is ignored (configurable)
* Custom messages

### Commands and Permissions

* `/ptt` - view current playtime stats, defaults to all - `ptt.view`
* `/ptt <player>` - view others stats, defaults to `op` - `ptt.view.others`
* `/ptt acquire [id]` - Acquire available reward (or specified reward with id parameter), defaults to all - `ptt.acquire`
* `/ptt reset all|<player>` - reset all/specific player's stats, defaults to `op` - `ptt.reset`
* `/ptt reload` - reload configuration, default to `op` - `ptt.reload`

### Configuration

(Pending update)
 
### License

MIT.
