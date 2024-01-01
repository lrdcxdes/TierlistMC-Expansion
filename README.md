# TierlistMC-Expansion

TierlistMC-Expansion is a PlaceholderAPI Expansion for Bukkit/Spigot Minecraft servers. It provides placeholders related
to player tier data, which can be used in other plugins that support PlaceholderAPI.

## Features

- Fetches player tier data from the TierlistMC API.
- Caches the fetched data to reduce API calls.
- Automatically removes cached data when a player leaves the server.
- Configurable cache duration and format for each tier type.

## Placeholders

- `%tierlistmc_name_<tierType>%`: Returns the name of the player's current tier of the specified type.
- `%tierlistmc_id_<tierType>%`: Returns the ID of the player's current tier of the specified type.

Replace `<tierType>` with the type of the tier you want to fetch.

A list of available tier types:
  - `diamond` (Beast)
  - `netherpot` (Netherite Potion)
  - `crystal` (Vanilla Crystal)

## Additional Placeholder

- `%tierlistmc_find_<field>_<tierType>_<playerName>%`: Returns the specified field of the specified player's current
  tier of the specified type.

Replace `<field>` with the field you want to fetch (`name` or `id`), `<tierType>` with the type of the tier you want to
fetch, and `<playerName>` with the name of the player whose tier data you want to fetch.

For example, `%tierlistmc_find_name_diamond_john%` will return the name of the diamond tier of the player named John.

## Configuration

The configuration file `tierlistmc.yml` is located in the `PlaceholderAPI` plugin's data folder. Here are the available
options:

- `api-key`: Your API key for the TierlistMC API.
- `language`: The language to use for tier names (default: "en").
- `cache-seconds`: The duration to keep data in the cache (default: 60).
- `read-timeout`: The read timeout for API calls (default: 10).
- `retry-on-connection-failure`: Whether to retry API calls on connection failure (default: true).
- `connect-timeout`: The connect timeout for API calls (default: 10).
- `max-idle-connections`: The maximum number of idle connections for the API client (default: 256).
- `keep-alive-duration`: The keep-alive duration for the API client (default: 10).
- `remove-on-quit`: Whether to remove a player's data from the cache when they quit the server (default: true).
- `formats`: The format for each tier type. Must contain `{name}` which will be replaced with the tier name.

## Installation

1. Download the latest release from
   the [releases page](https://github.com/lrdcxdes/TierlistMC-Expansion/releases/latest/download/Expansion-tierlistmc.jar).
2. Place the downloaded `.jar` file into your papi expansions `plugins/PlaceholderAPI/expansions` folder.
3. Use `/papi reload` or Restart your server.
4. Configure the expansion by editing the `tierlistmc.yml` file in the `plugins/PlaceholderAPI` plugin's data folder.
5. Reload the plugin or restart your server again.

## Usage

After installation and configuration, you can use the placeholders in any plugin that supports PlaceholderAPI. For
example, in a chat / tab plugin, you could display a player's current tier in their chat messages / tab.

## Support

If you encounter any issues or have any questions, please open an issue on
the [GitHub repository](https://github.com/lrdcxdes/TierlistMC-Expansion/issues).
