/* **********************************************************************************************************************
 * HudPixelReloaded - License
 * <p>
 * The repository contains parts of Minecraft Forge and its dependencies. These parts have their licenses
 * under forge-docs/. These parts can be downloaded at files.minecraftforge.net.This project contains a
 * unofficial copy of pictures from the official Hypixel website. All copyright is held by the creator!
 * Parts of the code are based upon the Hypixel Public API. These parts are all in src/main/java/net/hypixel/api and
 * subdirectories and have a special copyright header. Unfortunately they are missing a license but they are obviously
 * intended for usage in this kind of application. By default, all rights are reserved.
 * The original version of the HudPixel Mod is made by palechip and published under the MIT license.
 * The majority of code left from palechip's creations is the component implementation.The ported version to
 * Minecraft 1.8.9 and up HudPixel Reloaded is made by PixelModders/Eladkay and also published under the MIT license
 * (to be changed to the new license as detailed below in the next minor update).
 * <p>
 * For the rest of the code and for the build the following license applies:
 * <p>
 * # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
 * #  HudPixel by PixelModders, Eladkay & unaussprechlich is licensed under a Creative Commons         #
 * #  Attribution-NonCommercial-ShareAlike 4.0 International License with the following restrictions.  #
 * #  Based on a work at HudPixelExtended & HudPixel.                                                  #
 * # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
 * <p>
 * Restrictions:
 * <p>
 * The authors are allowed to change the license at their desire. This license is void for members of PixelModders and
 * to unaussprechlich, except for clause 3. The licensor cannot revoke these freedoms in most cases, as long as you follow
 * the following license terms and the license terms given by the listed above Creative Commons License, however in extreme
 * cases the authors reserve the right to revoke all rights for usage of the codebase.
 * <p>
 * 1. PixelModders, Eladkay & unaussprechlich are the authors of this licensed material. GitHub contributors are NOT
 * considered authors, neither are members of the HudHelper program. GitHub contributers still hold the rights for their
 * code, but only when it is used separately from HudPixel and any license header must indicate that.
 * 2. You shall not claim ownership over this project and repost it in any case, without written permission from at least
 * two of the authors.
 * 3. You shall not make money with the provided material. This project is 100% non commercial and will always stay that
 * way. This clause is the only one remaining, should the rest of the license be revoked. The only exception to this
 * clause is completely cosmetic features. Only the authors may sell cosmetic features for the mod.
 * 4. Every single contibutor owns copyright over his contributed code when separated from HudPixel. When it's part of
 * HudPixel, it is only governed by this license, and any copyright header must indicate that. After the contributed
 * code is merged to the release branch you cannot revoke the given freedoms by this license.
 * 5. If your own project contains a part of the licensed material you have to give the authors full access to all project
 * related files.
 * 6. You shall not act against the will of the authors regarding anything related to the mod or its codebase. The authors
 * reserve the right to take down any infringing project.
 **********************************************************************************************************************/
package com.palechip.hudpixelmod.modulargui.components

import com.mojang.realmsclient.gui.ChatFormatting
import com.palechip.hudpixelmod.GameDetector
import com.palechip.hudpixelmod.HudPixelMod
import com.palechip.hudpixelmod.config.CCategory
import com.palechip.hudpixelmod.config.ConfigPropertyBoolean
import com.palechip.hudpixelmod.modulargui.SimpleHudPixelModularGuiProvider
import com.palechip.hudpixelmod.util.GameType
import com.palechip.hudpixelmod.util.McColorHelperJava
import com.palechip.hudpixelmod.util.plus
import net.minecraftforge.fml.client.FMLClientHandler
import java.util.*

object TkrTimerModularGuiProvider : SimpleHudPixelModularGuiProvider(), McColorHelperJava {
    private val lap = 0
    private var running = false
    private var startingTime: Long = 0
    private var runningTime = "00:00"

    private var officialTime = ""

    override fun doesMatchForGame(): Boolean {
        return GameDetector.doesGameTypeMatchWithCurrent(GameType.TURBO_KART_RACERS)
    }

    override fun setupNewGame() {
    }

    override fun onGameStart() {
        // start the general timer and the first lap timer
        if (this.lap == 0 || this.lap == 1) {
            // add the start delay. Setting the start into the future if we know the start delay
            this.startingTime = System.currentTimeMillis() + startDelay
            this.running = true
        }
    }

    override fun onGameEnd() {
        this.running = false
    }

    override fun onTickUpdate() {
        if (this.running) {
            // update the time
            val timeDifference = System.currentTimeMillis() - this.startingTime
            val timeDifferenceSeconds = timeDifference / 1000
            val timeDifferenceMinutes = timeDifferenceSeconds / 60

            // translate to our format
            this.runningTime = if (timeDifference < 0) "-" else "" + if (Math.abs(timeDifferenceMinutes % 60) < 10) "0" else "" + Math.abs(timeDifferenceMinutes % 60) + ":" + if (Math.abs(timeDifferenceSeconds) % 60 < 10) "0" else "" + Math.abs(timeDifferenceSeconds % 60)
        }
    }

    override fun onChatMessage(textMessage: String, formattedMessage: String) {
        // isHypixelNetwork if the message is relevant
        if (textMessage.matches(LAP_COMPLETION_MESSAGE_REGEX.toRegex())) {
            try {
                // the lap number is the 5th character. It needs to be cast to String first because otherwise we get the wrong value
                val lapNo = Integer.valueOf(textMessage[4].toString())!!

                // isHypixelNetwork if the listened lap was completed
                if (this.lap == lapNo) {
                    // extract the start message
                    this.officialTime = textMessage.substring(textMessage.indexOf('(') + 1, textMessage.indexOf(')'))
                    // stop the timer
                    this.running = false
                }

                // start the next timer
                if (this.lap - 1 == lapNo) {
                    this.running = true
                    // there is no delay here
                    this.startingTime = System.currentTimeMillis()
                }

                // accuracy isHypixelNetwork and correction for the main timer after the first lap
                if (lapNo == 1 && this.lap == 0) {
                    // save the current time
                    val currentTime = System.currentTimeMillis()
                    // save the measured time
                    val measuredTime = currentTime - this.startingTime

                    val officialTime = ArrayList<Int>(2)
                    // convert the officialTime
                    for (s in textMessage.substring(textMessage.indexOf('(') + 1, textMessage.indexOf(')')).split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                        officialTime.add(Integer.valueOf(s))
                    }
                    // convert everything into milliseconds
                    val officialTimeMilliSeconds = (officialTime[0] * 60 * 1000 + officialTime[1] * 1000).toLong()

                    // correct the main timer
                    this.startingTime = currentTime - officialTimeMilliSeconds

                    // the start delay is the difference between our (greater) measured time and the official time
                    startDelay = startDelay + (measuredTime - officialTimeMilliSeconds)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                HudPixelMod.logWarn("Failed to parse the lap completion message. Ignoring")
            }

        }
        // stop the general timer when finishing
        if (textMessage.startsWith(FMLClientHandler.instance().client.session.username + " has finished the race at position ") && this.lap == 0) {
            this.running = false
        }
    }

    // for the general timer
    // show the running time
    // show the result if the timer is running
    // return the official time if it isn't running. This may also be empty if it hasn't started yet
    val renderingString: String
        get() {
            if (this.lap == 0) {
                return TimerModularGuiProvider.TIME_DISPLAY_MESSAGE + this.runningTime
            } else {
                if (this.running) {
                    return ChatFormatting.YELLOW + "Lap " + this.lap + ": " + this.runningTime
                } else {
                    return if (this.officialTime.isEmpty()) "" else ChatFormatting.YELLOW + "Lap " + this.lap + ": " + this.officialTime
                }
            }
        }

    override fun showElement(): Boolean {
        return doesMatchForGame() && !GameDetector.isLobby() && enabled
    }

    override fun content(): String {
        return renderingString
    }

    override fun ignoreEmptyCheck(): Boolean {
        return false
    }

    override fun getAfterstats(): String {
        return McColorHelperJava.YELLOW + "You played a total of " + McColorHelperJava.GREEN + lap + McColorHelperJava.YELLOW + " laps."
    }

        val LAP_COMPLETION_MESSAGE_REGEX = "(Lap \\d Completed!).*"
        @ConfigPropertyBoolean(category = CCategory.HUD, id = "kartRacersAccurateTimeDisplay", comment = "The TKR Time Tracker", def = true)
        @JvmStatic
        var enabled = false
        private var startDelay = 0L

}