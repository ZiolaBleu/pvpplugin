package com.pvptournament;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pvptournament")
public interface TournamentConfig extends Config
{
    @ConfigItem(keyName = "serviceUrl", name = "Tournament service URL", description = "Use the same trusted HTTPS service as the organizer. Local testing: http://127.0.0.1:8080")
    default String serviceUrl() { return "http://127.0.0.1:8080"; }
}
