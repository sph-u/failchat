Failchat is a desktop application for streamers. It aggregates chat messages from multiple sources, shows you viewer count, etc.   
Detailed description could be found [on the site](https://onoderis.github.io/failchat/).

### Requirements
Java 11 with bundled JavaFX. [Liberica JDK](https://bell-sw.com/pages/downloads/#/java-11-lts) is recommended.

### Before you build
Create file `src/main/resources/config/private.properties` with the following properties and replace the values:
```properties
twitch.bot-name = BOT_NAME
twitch.bot-password = BOT_PASSWORD (has prefix "oauth:")
twitch.api-token = API_TOKEN
```
