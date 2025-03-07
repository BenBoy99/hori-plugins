/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.vorkathHelper;

import lombok.Getter;
import net.runelite.client.config.*;

@ConfigGroup("VorkathAssistantConfig")

public interface VorkathHelperConfig extends Config
{

    @ConfigSection(
            keyName = "delayConfig",
            name = "Delay Configuration",
            description = "Configure how the plugin handles sleep/tick delays.",
            closedByDefault = true,
            position = 0)
    public static String delayConfig = "delayConfig";

    @ConfigSection(
            keyName = "vorkathConfig",
            name = "Vorkath assistant config",
            description = "Set up your assistant",
            closedByDefault = true,
            position = 1)
    public static String vorkathConfig = "vorkathConfig";

    @Range(min = 0, max = 550)
    @ConfigItem(
            keyName = "sleepMin",
            name = "Sleep Min",
            description = "",
            position = 1,
            section = "delayConfig")
    default int sleepMin() {
        return 60;
    }

    @Range(
            min = 0,
            max = 550
    )
    @ConfigItem(
            keyName = "sleepMax",
            name = "Sleep Max",
            description = "",
            position = 2,
            section = "delayConfig"
    )
    default int sleepMax() {
        return 350;
    }

    @Range(
            min = 0,
            max = 550
    )
    @ConfigItem(
            keyName = "sleepTarget",
            name = "Sleep Target",
            description = "",
            position = 3,
            section = "delayConfig"
    )
    default int sleepTarget() {
        return 100;
    }

    @Range(
            min = 0,
            max = 550
    )
    @ConfigItem(
            keyName = "sleepDeviation",
            name = "Sleep Deviation",
            description = "",
            position = 4,
            section = "delayConfig"
    )
    default int sleepDeviation() {
        return 10;
    }

    @ConfigItem(
            keyName = "sleepWeightedDistribution",
            name = "Sleep Weighted Distribution",
            description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
            position = 5,
            section = "delayConfig"
    )
    default boolean sleepWeightedDistribution() {
        return false;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
            keyName = "tickDelayMin",
            name = "Game Tick Min",
            description = "",
            position = 7,
            section = "delayConfig")
    default int tickDelayMin() {
        return 1;
    }

    @Range(
            min = 0,
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayMax",
            name = "Game Tick Max",
            description = "",
            position = 8,
            section = "delayConfig"
    )
    default int tickDelayMax() {
        return 3;
    }

    @Range(
            min = 0,
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayTarget",
            name = "Game Tick Target",
            description = "",
            position = 9,
            section = "delayConfig"
    )
    default int tickDelayTarget() {
        return 2;
    }

    @Range(
            min = 0,
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayDeviation",
            name = "Game Tick Deviation",
            description = "",
            position = 10,
            section = "delayConfig"
    )
    default int tickDelayDeviation() {
        return 1;
    }

    @ConfigItem(
            keyName = "tickDelayWeightedDistribution",
            name = "Game Tick Weighted Distribution",
            description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
            position = 11,
            section = "delayConfig"
    )
    default boolean tickDelayWeightedDistribution() {
        return false;
    }



    @ConfigItem(
            keyName = "enablePrayer",
            name = "Manage quick prayer",
            description = "Re-enables quick prayers after the pink dragonfire attack and enables/disables it while killing Vorkath.",
            position = 1,
            section = "vorkathConfig"
    )
    default boolean enablePrayer()
    {
        return true;
    }

    @ConfigItem(
            keyName = "dodgeBomb",
            name = "Dodge fire bombs",
            description = "Dodges the vertical fire bomb attack.",
            position = 2,
            section = "vorkathConfig"
    )
    default boolean dodgeBomb()
    {
        return true;
    }

    @ConfigItem(keyName = "walkMethod", name = "Walk Method", description = "Acid phase method", position = 3, section = vorkathConfig)
    default walkMethod walkMethod() { return walkMethod.NONE; }


    @Range(min = 4, max = 7)
    @ConfigItem(
            keyName = "acidFreePathMinLength",
            name = "Acid walk length",
            description = "Minimum length of acid walk. - Experimental",
            position = 4,
            section = "vorkathConfig")
    default int acidFreePathLength()
    {
        return 3;
    }

    @ConfigItem(
            keyName = "killSpawn",
            name = "Cast crumble undead on spawn",
            description = "Casts crumble undead when the Zombified spawn appears.",
            position = 5,
            section = "vorkathConfig"
    )
    default boolean killSpawn()
    {
        return true;
    }

    @ConfigItem(
            keyName = "fastRetaliate",
            name = "Faster retaliate",
            description = "Attacks vorkath automatically.",
            position = 6,
            section = "vorkathConfig"
    )
    default boolean fastRetaliate()
    {
        return true;
    }

    @ConfigItem(
            keyName = "switchBolts",
            name = "Switch bolts",
            description = "Switches bolts at the ideal health threshold.",
            position = 7,
            section = "vorkathConfig"
    )
    default boolean switchBolts()
    {
        return false;
    }

    @ConfigItem(keyName = "startHelper", name = "Start/Stop", description = "", position = 8, title = "startHelper")
    default Button startHelper() {
        return new Button();
    }

    enum walkMethod {
        NONE(1),
        WALK(2),
        WOOX_MELEE(3),
        WOOX_CROSSBOW(4),
        WOOX_BLOWPIPE(5);

        @Getter
        private final int id;

        walkMethod(int id) {
            this.id = id;
        }
    }

}