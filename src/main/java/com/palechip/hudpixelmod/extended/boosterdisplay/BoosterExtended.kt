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

import com.mojang.realmsclient.gui.ChatFormatting
import com.palechip.hudpixelmod.api.interaction.representations.Booster
import com.palechip.hudpixelmod.extended.HudPixelExtended
import com.palechip.hudpixelmod.extended.util.ImageLoader
import com.palechip.hudpixelmod.extended.util.LoggerHelper
import com.palechip.hudpixelmod.extended.util.McColorHelper
import com.palechip.hudpixelmod.extended.util.gui.FancyListObject
import com.palechip.hudpixelmod.util.GameType
import com.palechip.hudpixelmod.util.plus
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
class BoosterExtended
/**
 * @param gameType The gametype this boosterdisplay is for
 */
internal constructor(//######################################################################################################################
        val gameType: GameType) : FancyListObject(), McColorHelper {
    private var timeNextTip: Long = 0
    internal var booster:

            Booster? = null
        private set
    private var lastBoosterAdded: Long = 0

    init {
        timeNextTip = 0
        this.resourceLocation = ImageLoader.gameIconLocation(gameType)
    }

    internal fun setCurrentBooster(booster: Booster) {
        lastBoosterAdded = System.currentTimeMillis()
        this.booster = booster
        this.fancyListObjectButtons.clear()
    }

    /**
     * sets a this gamemode tipped also sets the current booster to tipped if there is any

     * @param player the player you have tipped to
     */
    internal fun setGameModeTipped(player: String) {
        LoggerHelper.logInfo("[BoosterDisplay]: You tipped " + player + " in " + gameType.nm)
        timeNextTip = System.currentTimeMillis() + tipDelay
        if (booster != null && booster!!.owner0.equals(player, ignoreCase = true)) {
            LoggerHelper.logInfo("[BoosterDisplay]: Also found a booster for " + player)
            booster!!.tip()
        }
    }


    /**
     * It checks if the booster is outdated and also generates the current
     * RenderStrings
     */
    override fun onTick() {
        if (booster != null)
            if (System.currentTimeMillis() - booster!!.remainingTime * 1000 > lastBoosterAdded) {
                this.booster = null
                HudPixelExtended.boosterManager.requestBoosters(true)
            }

        this.renderPicture = ChatFormatting.WHITE + countDown()
        this.renderLineSmall = McColorHelper.YELLOW + gameType.nm
        this.renderLine1 = McColorHelper.GOLD + gameType.nm
        if (booster == null) {
            this.renderLine2 = McColorHelper.GRAY + "No Booster online!"
            this.fancyListObjectButtons.clear()
        } else {
            if (booster!!.isTipped) {
                this.renderLine2 = McColorHelper.RED + booster!!.owner0!!
                this.fancyListObjectButtons.clear()
            } else {
                if (fancyListObjectButtons.isEmpty()) addButton(BoosterTipButton(booster!!))
                this.renderLine2 = McColorHelper.GREEN + booster!!.owner0!!
            }

        }
    }

    /**
     * Generates the countdown displayed over the game icon

     * @return the current countdown string
     */
    private fun countDown(): String {
        if (timeNextTip < System.currentTimeMillis()) {
            timeNextTip = 0
            return ""
        }
        val timeBuff = timeNextTip - System.currentTimeMillis()
        val sMin: String
        val min = timeBuff / 1000 / 60
        if (min < 10)
            sMin = "0" + min
        else
            sMin = "" + min
        val sSec: String
        val sec = timeBuff / 1000 - min * 60
        if (sec < 10)
            sSec = "0" + sec
        else
            sSec = "" + sec
        return sMin + ":" + sSec
    }

    companion object {
        private val tipDelay = 60 * 60 * 1000.toLong() //the tipdelay time for a gamemode

        //######################################################################################################################
        private val boosterLenght = 60 * 60 * 1000.toLong() //the time a booster gets activated
    }

}