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
package com.palechip.hudpixelmod.extended.boosterdisplay

import com.palechip.hudpixelmod.api.interaction.ApiQueueEntryBuilder
import com.palechip.hudpixelmod.api.interaction.callbacks.BoosterResponseCallback
import com.palechip.hudpixelmod.api.interaction.representations.Booster
import com.palechip.hudpixelmod.config.CCategory
import com.palechip.hudpixelmod.config.ConfigPropertyBoolean
import com.palechip.hudpixelmod.config.ConfigPropertyInt
import com.palechip.hudpixelmod.config.GeneralConfigSettings
import com.palechip.hudpixelmod.extended.util.LoggerHelper
import com.palechip.hudpixelmod.extended.util.gui.FancyListManager
import com.palechip.hudpixelmod.extended.util.gui.FancyListObject
import com.palechip.hudpixelmod.util.GameType
import net.hypixel.api.reply.BoostersReply
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.function.Consumer

@SideOnly(Side.CLIENT)
class BoosterManager : FancyListManager(5, BoosterManager.xOffsetBoosterDisplay.toFloat(), BoosterManager.yOffsetBoosterDisplay.toFloat(), BoosterManager.shownBooosterDisplayRight), BoosterResponseCallback {

    //######################################################################################################################
    /**
     * do some things while the gametick ... you should also send the tip
     * to each FancyListObject
     */
    internal var count = 0
    private var lastRequest: Long = 0

    init {
        this.isButtons = true
        for (g in gamesWithBooster) {
            this.fancyListObjects.add(BoosterExtended(g))
        }
        this.shownObjects = boostersShownAtOnce
        this.yStart = yOffsetBoosterDisplay.toFloat()
        this.xStart = xOffsetBoosterDisplay.toFloat()
        this.renderRightSide = shownBooosterDisplayRight
    }//this sets how many boosters are displayed at once you can change that


    override val configxStart: Int
        get() = xOffsetBoosterDisplay

    override val configRenderRight: Boolean
        get() = shownBooosterDisplayRight

    override val configyStart: Int
        get() = yOffsetBoosterDisplay

    /**
     * Well you can do some stuff here befor rendering the display
     * You still have to call the renderDisplay() method ... otherwise there
     * will be nothing shown.
     */
    override fun onRender() {
        if (!enabled) return
        if (Minecraft.getMinecraft().currentScreen is GuiChat && Minecraft.getMinecraft().displayHeight > 600) {
            this.renderDisplay()
            this.isMouseHander = true
        } else {
            this.isMouseHander = false
        }
    }

    override fun onClientTick() {
        requestBoosters(false)
        fancyListObjects.forEach(Consumer<FancyListObject> { it.onClientTick() })
    }

    /**
     * Filters out the tipped message and notifies the BoosterExtended that is had been tipped.

     * @param e The chatEvent
     */
    override fun onChatReceived(e: ClientChatReceivedEvent) {
        val chat = e.message.unformattedText
        if (!chat.contains("You tipped ") || chat.contains(":")) return

        val split = chat.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val player = split[2]
        var gamemode = split[4]

        for (i in 5..split.size - 1)
            gamemode += " " + split[i]

        val gameType = GameType.getTypeByName(gamemode)

        for (f in fancyListObjects) {
            val b = f as BoosterExtended
            if (b.gameType === gameType)
                b.setGameModeTipped(player)
        }
    }

    /**
     * This requests the boosters via the api interaction

     * @param forceRequest Set this to true if you want to force the request
     */
    fun requestBoosters(forceRequest: Boolean?) {
        if (GeneralConfigSettings.useAPI && enabled) {
            // isHypixelNetwork if enough time has past
            if (System.currentTimeMillis() > lastRequest + REQUEST_COOLDOWN) {
                // save the time of the request
                lastRequest = System.currentTimeMillis()
                // tell the queue that we need boosters
                ApiQueueEntryBuilder.newInstance().boosterRequest().setCallback(this).create()
            }
        }
    }

    fun checkBooster(b: Booster) {
        for (fco in fancyListObjects) {
            val be = fco as BoosterExtended
            if (be.gameType === b.gameType) {
                if (be.booster != null && be.booster!!.owner0 == b.owner0) {
                    return
                } else {
                    be.setCurrentBooster(b)
                    LoggerHelper.logInfo("[BoosterDisplay]: stored booster with ID " + b.gameType.nm + "[" + b.gameType.databaseID + "]"
                            + " and owner " + b.owner0 + " in the boosterdisplay!")
                    return
                }
            }
        }

        LoggerHelper.logWarn("[BoosterDisplay]: No display found for booster with ID "
                + b.gameType.databaseID + " and owner " + b.owner0 + "!")
    }

    /**
     * This method gets called when there is a booster response
     * Sorry for this messy if-for but somehow it works :P

     * @param boosters the boosters parsed by the callback
     */
    override fun onBoosterResponse(boosters: List<BoostersReply.Booster>) {

        // we aren't loading anymore
        if (boosters != null) {
            for (b in boosters) {
                if (b.length < b.originalLength) {
                    Booster(b.purchaserUuid.toString(), GameType.getTypeByDatabaseID(b.gameType?.id))
                }
            }
        } else
            LoggerHelper.logWarn("[BoosterDisplay]: The buuster response was NULL!")
    }

    companion object {

        //######################################################################################################################

        private val REQUEST_COOLDOWN = 10 * 60 * 1000 // = 10min
        /**
         * Enter a  new gamemode with booster here, the system will add the booster then!
         * Also please upload the gameicon to the resource folder and link it in util.ImageLoader
         * Also please add the new gamemode with the right ID and right name (put there the name it says
         * when tipping somebody in this gamemode) to the GameType enum class. Also add the right tipname in the
         * GameType enum!
         */
        private val gamesWithBooster = arrayOf(GameType.SPEED_UHC, GameType.SMASH_HEROES, GameType.CRAZY_WALLS, GameType.SKYWARS, GameType.TURBO_KART_RACERS, GameType.WARLORDS, GameType.MEGA_WALLS, GameType.UHC, GameType.BLITZ, GameType.COPS_AND_CRIMS, GameType.THE_WALLS, GameType.ARCADE_GAMES, GameType.ARENA, GameType.PAINTBALL, GameType.TNT_GAMES, GameType.VAMPIREZ, GameType.QUAKECRAFT)
        @ConfigPropertyInt(category = CCategory.BOOSTER_DISPLAY, id = "xOffsetBoosterDisplay", comment = "X offset of Booster display", def = 2)
        @JvmStatic
        var xOffsetBoosterDisplay = 2
        @ConfigPropertyInt(category = CCategory.BOOSTER_DISPLAY, id = "yOffsetBoosterDisplay", comment = "Y offset of Booster display", def = 2)
        @JvmStatic
        var yOffsetBoosterDisplay = 2
        @ConfigPropertyBoolean(category = CCategory.BOOSTER_DISPLAY, id = "shownBooosterDisplayRight", comment = "Show booster display on right", def = true)
        @JvmStatic
        var shownBooosterDisplayRight = true
        @ConfigPropertyInt(category = CCategory.BOOSTER_DISPLAY, id = "boostersShownAtOnce", comment = "Boosters Shown at Once", def = 5)
        @JvmStatic
        var boostersShownAtOnce = 5
        @ConfigPropertyBoolean(category = CCategory.BOOSTER_DISPLAY, id = "isBoosterDisplay", comment = "Enable or disable the BoosterDisplay", def = true)
        @JvmStatic
        var enabled = false
    }
}
/**
 * sets the settings for the fancyListManager and also generates all boosters
 * in the gamesWithBooster array.
 */