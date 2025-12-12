# Blood Magic Teams

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.83+-orange.svg)](https://neoforged.net)
[![License](https://img.shields.io/badge/License-CC%20BY%204.0-blue.svg)](LICENSE)

A Minecraft NeoForge mod that integrates [Blood Magic](https://github.com/WayofTime/BloodMagic) with [FTB Teams](https://github.com/FTBTeam/FTB-Teams), allowing players to bind items to their team's shared LP network.

## Overview

Blood Magic Teams enables cooperative Blood Magic gameplay by allowing team members to share bound items and LP networks. When binding a Blood Magic item, players on a team can choose to bind it to their team instead of themselves, enabling all team members to use the item and contribute to a shared LP pool.

## Features

- **Team Binding** - Bind blood orbs, daggers, sigils, and other items to your FTB Team
- **Shared LP Network** - Team-bound items draw from and contribute to a shared LP pool
- **Binding UI** - Clean interface to choose between personal and team binding
- **Preference System** - Save your binding preference with "Don't ask again"
- **Dynamic Tooltips** - Team names update automatically when teams are renamed
- **Commands** - Manage preferences via `/bloodmagicteams`
- **Configurable** - Server-side config for all features
- **Team Properties** - Team leaders can restrict team binding permissions

## Dependencies

| Mod | Type | Link |
|-----|------|------|
| Blood Magic | Required | [GitHub Releases](https://github.com/Saereth/BloodMagic/releases) |
| FTB Teams | Required | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge) |
| FTB Library | Required | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge) |
| Architectury API | Required | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/architectury-api) |

## Commands

| Command | Description |
|---------|-------------|
| `/bloodmagicteams status` | View your current binding settings |
| `/bloodmagicteams self` | Set default to personal binding |
| `/bloodmagicteams team` | Set default to team binding |
| `/bloodmagicteams reset` | Clear preference and ask again |

## Configuration

Configuration file located at `config/bloodmagicteams-common.toml`:

```toml
[general]
# Enable team-level soul network binding
enableTeamBinding = true

# Show UI when binding items to choose between personal and team binding
showBindingUI = true

# Allow players to use /bmteams bindingmode command to set their default binding mode
allowBindingModeCommand = true

# Default binding mode when UI is disabled or player hasn't set a preference
# PERSONAL = bind to player's personal soul network
# TEAM = bind to team's shared soul network
# ASK = always show the binding UI
defaultBindingMode = "ASK"
```

## How It Works

1. Join or create an FTB Team
2. Obtain a Blood Magic bindable item (blood orb, dagger, etc.)
3. Right-click to bind - a UI will appear asking Personal or Team binding
4. Select **Team Binding** to share with teammates
5. Optionally check "Don't ask again" to save your preference

Team-bound items:

- Can be used by any team member
- Share a single LP network across the team
- Display `[Team Bound]` in the tooltip
- Show the current team name (updates if renamed)

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the [Creative Commons Attribution 4.0 International License (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/) - see the [LICENSE](LICENSE) file for details.

## Credits

- [WayofTime](https://github.com/WayofTime) - Blood Magic
- [FTB Team](https://github.com/FTBTeam) - FTB Teams & FTB Library
- [Architectury](https://github.com/architectury) - Architectury API

## Links

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/blood-magic-teams)
- [Issues](https://github.com/saereth/bloodmagicteams/issues)
