# TODO List

Complete:
- [x] Reports
- [x] User Profiles (UUID/Name cache & other attributes)
- [x] Moderation


## User Profiles
- [x] `/seen` command
- [ ] `/alts` command - checks online players `/alts` display online `/alts [player]` looks up if other players have joined with same IP
- [ ] `/profile` command: Display extensive info on player, user profile info, IP history, etc
- [ ] Add IP tracking

## Feature ToDo (Confirmed):
- [ ] Add ban import command (Import from banned-players.json, future support for other major platforms e.g CommandBook)
- [ ] Teleports (/tp, /bring, consider CommandBook and what's used)
- [ ] MOTD login messages and unlimited "info" commands. Example, define rules section in config, command: 'rules', message: "etc" which would translate to `/rules` -> msg
- [ ] A generalized Freeze option (not a priority) 


## ToDo GitHub:
- [ ] Setup PGM.dev pages with Install, Commands, Permissions, Config pages
- [ ] Transfer TODO above to github projects and issues for each item, if not complete
- [ ] Finalize README with project info


## Bugs
- [ ] /msg is sending even when player is muted, look into PGM to fix this



## Feature Ideas (not confirmed yet)
- [ ] Friends Support: Command `/friends`, hook into PGM, restore name highlighting in tab, friend only death/join messages. Include non-pgm option too
- [ ] Discord Support: could include account linking, transfer what Cloudy already does (reports/server status), PGM specific(auto audio team channels for linked players)
- [ ] Fun Commands: fireworks, tnt, etc