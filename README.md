# EzObserver

## Project Introduction
EzObserver is an anti-illegal item plugin specifically designed for Minecraft servers. In Minecraft survival servers, players may acquire or craft items with illegal enchantments (e.g., excessively high enchantment levels) or abnormal attribute modifiers through various means. These items can severely disrupt game balance and player experience.

EzObserver aims to address this issue by real-time monitoring of item movement events within the server and performing in-depth scans of these items. It automatically detects and handles any illegal enchantments and attribute modifiers that do not comply with server rules. This helps maintain a fair and healthy gaming environment.

## Technology Stack
*   **Language**: Java
*   **Build Tool**: Maven
*   **API**: Spigot/Paper API (compatible with 1.20+ versions, supports Folia)

## Feature Details
*   **Item Movement Monitoring**: The plugin monitors all item movement events, including but not limited to:
    *   Item transfers between player inventories, chests, hoppers, and other containers.
    *   Item drops and pickups.
    *   Item crafting, enchanting, and other operations.
*   **Illegal Enchantment Detection**:
    *   Detects whether the type and level of enchantments on items are legal according to preset rules.
    *   For example, it can be configured to prohibit certain enchantments or limit the maximum level of enchantments.
*   **Attribute Modifier Detection**:
    *   Detects whether attribute modifiers attached to items (such as attack damage, health, etc.) are within legal limits.
    *   Prevents players from obtaining abnormal attributes by modifying item NBT data.
*   **Violation Handling**: The plugin can perform corresponding actions on detected illegal items based on configuration, such as:
    *   Removing illegal enchantments or attributes.
    *   Replacing the item with a legal version.
    *   Logging violations and notifying administrators.

## Installation
1.  Ensure your Minecraft server is running Spigot, Paper, or Folia 1.20 or higher.
2.  Download the latest `EzObserver.jar` file from the [releases page](https://github.com/Kush-ShuL/EzObserver/releases).
3.  Place the downloaded `EzObserver.jar` file into your Minecraft server's `plugins` folder.
4.  Start or restart your server.
5.  The plugin will automatically generate default `config.yml` and `messages.yml` configuration files in the `plugins/EzObserver/` directory.

## Configuration
*   `config.yml`: The core configuration file, used to define:
    *   A list of allowed or prohibited enchantments and their maximum levels.
    *   Legal ranges for attribute modifiers.
    *   How illegal items are handled (removal, replacement, logging, etc.).
    *   Other plugin behavior settings.
*   `messages.yml`: The message configuration file, used to customize all message texts sent by the plugin to players or administrators.

## Commands
All commands start with `/ezobserver` or its aliases (`/ezo`, `/ezobs`).

*   `/ezobserver reload`: Reloads the plugin's `config.yml` and `messages.yml` configuration files without restarting the server.
*   `/ezobserver status`: Displays the plugin's current running status and some basic information.
*   `/ezobserver help`: Shows available commands and brief descriptions for the plugin.

## Permissions
*   `ezobserver.admin`: Allows players to execute all EzObserver administration commands (e.g., `/ezobserver reload`). By default, this permission is granted to server operators (op).
*   `ezobserver.bypass`: Allows players to bypass all item detection. Items belonging to players with this permission will not be scanned or processed by EzObserver. By default, this permission is not granted to any players.

## Like the project?
If you find EzObserver helpful, please consider giving the project a free ⭐ on [GitHub](https://github.com/Kush-ShuL/EzObserver). It's the biggest support for the developer!

## Development and Contribution
If you are interested in the development of EzObserver or wish to contribute code, please visit the project's GitHub repository (if available).

## License
This project is licensed under the GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (AGPLv3). Please refer to the `LICENSE` file in the project root directory for details.
