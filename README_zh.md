# EzObserver

## 项目简介
EzObserver 是一个专为 Minecraft 服务器设计的反违规物品插件。在 Minecraft 生存服务器中，玩家可能会通过各种手段获得或制造出带有违规附魔（例如，附魔等级过高）或异常属性修饰符的物品，这些物品会严重破坏游戏平衡性和玩家体验。

EzObserver 旨在解决这一问题，它通过实时监听服务器内物品的移动事件，并对这些物品进行深度扫描，自动检测并处理任何不符合服务器规则的违规附魔和属性修饰符。这有助于维护一个公平、健康的游戏环境。

## 技术栈
*   **语言**: Java
*   **构建工具**: Maven
*   **API**: Spigot/Paper API (兼容 1.20 版本及以上，支持 Folia)

## 功能详情
*   **物品移动监听**: 插件会监听所有涉及物品移动的事件，包括但不限于：
    *   玩家背包、箱子、漏斗等容器之间的物品转移。
    *   物品掉落、拾取。
    *   物品合成、附魔等操作。
*   **违规附魔检测**:
    *   根据预设规则，检测物品上附魔的类型和等级是否合法。
    *   例如，可以配置禁止某些附魔，或限制附魔的最高等级。
*   **属性修饰符检测**:
    *   检测物品上附加的属性修饰符（如攻击力、生命值等）是否在合法范围内。
    *   防止玩家通过修改物品 NBT 数据来获得超常属性。
*   **违规处理**: 插件可以根据配置对检测到的违规物品执行相应操作，例如：
    *   移除违规附魔或属性。
    *   将物品替换为合法版本。
    *   记录违规行为并通知管理员。

## 安装
1.  确保您的 Minecraft 服务器运行的是 Spigot、Paper 或 Folia 1.20 或更高版本。
2.  从 [发布页面](https://github.com/Kush-ShuL/EzObserver/releases) 下载最新版本的 `EzObserver.jar` 文件。
3.  将下载的 `EzObserver.jar` 文件放入您的 Minecraft 服务器的 `plugins` 文件夹中。
4.  启动或重启您的服务器。
5.  插件将自动在 `plugins/EzObserver/` 目录下生成默认的 `config.yml` 和 `messages.yml` 配置文件。

## 配置
*   `config.yml`: 核心配置文件，用于定义：
    *   允许或禁止的附魔列表及其最大等级。
    *   属性修饰符的合法范围。
    *   违规物品的处理方式（移除、替换、记录等）。
    *   其他插件行为设置。
*   `messages.yml`: 消息配置文件，用于自定义插件发送给玩家或管理员的所有消息文本。

## 命令
所有命令都以 `/ezobserver` 或其别名 (`/ezo`, `/ezobs`) 开头。

*   `/ezobserver reload`: 重新加载插件的 `config.yml` 和 `messages.yml` 配置文件，无需重启服务器。
*   `/ezobserver status`: 查看插件的当前运行状态和一些基本信息。
*   `/ezobserver help`: 显示插件的可用命令和简要说明。

## 权限
*   `ezobserver.admin`: 允许玩家执行所有 EzObserver 管理命令 (例如 `/ezobserver reload`)。默认情况下，此权限授予服务器操作员 (op)。
*   `ezobserver.bypass`: 允许玩家绕过所有物品检测。拥有此权限的玩家的物品将不会被 EzObserver 扫描和处理。默认情况下，此权限不授予任何玩家。

## 觉得项目不错？
如果您觉得 EzObserver 对您有帮助，请考虑在 [GitHub](https://github.com/Kush-ShuL/EzObserver) 上给项目一个免费的 ⭐，这是对开发者最大的支持！

## 开发与贡献

## 许可证
本项目根据 GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (AGPLv3) 授权。详情请参阅项目根目录下的 `LICENSE` 文件。
