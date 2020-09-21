# TODO List

## Completed
- [x] Reports
- [x] User Profiles (UUID/Name cache & other attributes)
- [x] Moderation (Mute, Kick, Ban, Temp Ban, Alt alerts, punishment lookup / history), IP tracking
- [x] Add ban import command (Import from banned-players.json, future support for other major platforms e.g CommandBook)
- [x] Teleports (/tp, /bring, consider CommandBook and what's used)
- [x] Chat Management (/chat lock, /chat clear, /chat slow)
- [x] Unlimited "info" commands, defined in config

## User Profiles
- [x] `/seen` command
- [x] `/alts` command - checks online players `/alts` display online `/alts [player]` looks up if other players have joined with same IP
- [x] Add IP tracking
- [x] Add {X} has joined with the same IP as recently banned player {Y}, See PGM impl
- [x] `/profile` command: Display extensive info on player, user profile info, IP history, etc (See OCC Project Insight for reference)

## Major ToDo (Confirmed):
- [ ] Teleports WIP: (Consider config options, teleport delay, cooldown
- [ ] MOTD login messages

## Bugs
- [ ] /msg is sending even when player is muted, look into PGM to fix this
- [ ] Allow commands to unregister if disabled (maybe?)

## ToDo GitHub:
- [ ] Setup PGM.dev pages with Install, Commands, Permissions, Config pages
- [ ] Transfer TODO above to github projects and issues for each item, if not complete
- [ ] Finalize README with project info

## Feature Ideas Major (not confirmed yet)
- [ ] Friends Support: Command `/friends`, hook into PGM, restore name highlighting in tab, friend only death/join messages. Include non-pgm option too if needed
- [ ] Bungee support: Store info about server, hook into Bungee directly from plugin. Useful for gathering network stats (I.e joins per day, week) Maybe redis support for pub/sub like things. Ex. network server status messages, join/leave messages to other servers Look at OCN for example
- [ ] Discord Support: could include account linking, auto role assignment, transfer what Cloudy already does (reports/server status), PGM specific(auto audio team channels for linked players)

## Feature Ideas Minor (not confirmed yet)
- [ ] Party Mode - Convert from OCC, make more customized (General Party / PGM Party)
- [ ] World Commands: /time, /weather, /mobs, 
- [ ] Fun Commands: /fireworks, /tnt, /hat, etc
- [ ] A generalized Freeze option (not a priority) 
