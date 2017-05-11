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

package com.palechip.hudpixelmod.extended.fancychat

import com.mojang.realmsclient.gui.ChatFormatting
import com.palechip.hudpixelmod.HudPixelMod
import com.palechip.hudpixelmod.config.CCategory
import com.palechip.hudpixelmod.config.ConfigPropertyBoolean
import com.palechip.hudpixelmod.config.ConfigPropertyInt
import com.palechip.hudpixelmod.extended.util.MessageBuffer
import com.palechip.hudpixelmod.extended.util.SoundManager
import com.palechip.hudpixelmod.util.plus
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.unaussprechlich.managedgui.lib.util.RenderUtils
import java.util.*

@SideOnly(Side.CLIENT)
object FancyChat {
    //[SETTING] the offset between each chatline (in ms)
    val RENDERING_HEIGHT_OFFSET: Short = 9
    // [SETTING] the with of the fancy chat overlay
    val FIELD_WIDTH: Short = 325
    // [SETTING] the with of the fancy chat overlay
    private val BOTTOM_Y_OFFSET: Short = 28

    //######################################################################################################################
    // [SETTING] the with of the fancy chat overlay
    private val MAX_SCROLLIST_LINES: Short = 20
    // [SETTING] the with of the fancy chat overlay
    private val SCROLLED_PER_NOTCH: Short = 5
    private val partyInvite1 = " has invited you to join their party!"
    private val partyInvite2 = "Click here to join! You have 60 seconds to accept."
    private val spacer = "-----------------------------------------------------"
    private val nothing = " "
    @ConfigPropertyBoolean(category = CCategory.FANCY_CHAT, id = "fancychat", comment = "The Fancy Chat", def = true)
    @JvmStatic
    var enabled = true

    //######################################################################################################################
    @ConfigPropertyInt(category = CCategory.FANCY_CHAT, id = "storedMessages", comment = "Stored Messages", def = 1000)
    @JvmStatic
    var storedMessages: Int = 0
    @ConfigPropertyInt(category = CCategory.FANCY_CHAT, id = "displayMessages", comment = "Displayed Messages", def = 8)
    @JvmStatic
    var displayMessages: Int = 0
    // [SETTING] the time a FancyChatMessage will be displayed (in ms)
    private val displayTimeMs = (displayMessages * 1000).toLong()
    // [SETTING] the with of the fancy chat overlay
    private val MAX_STORED_FANCYCHATMESSAGES = 1000
    private var spacerPartyRequired: Boolean? = false
    private var fancyChatInstance: FancyChat? = null
    private var scroll = MAX_SCROLLIST_LINES.toInt()
    private val fancyChatObjects = ArrayList<FancyChatObject>()
    private val fancyChatMessages = MessageBuffer(MAX_STORED_FANCYCHATMESSAGES)


    //------------------------------------------------------------------------------------------------------------------

    /**
     * Fired by the onRenderTick event @ HudPixelMod.class
     */
    fun onRenderTick() {
        //returns if there is nothing to render
        if (fancyChatObjects.isEmpty() && fancyChatMessages.messageBuffer.isEmpty()) return
        if (!enabled) return
        val res = ScaledResolution(Minecraft.getMinecraft())
        if (Minecraft.getMinecraft().displayWidth < 1300) {
            return
        }

        if (Minecraft.getMinecraft().inGameHasFocus) {
            renderOverlay(fancyChatObjects, res)
        } else if (Minecraft.getMinecraft().currentScreen is GuiChat) {
            if (fancyChatMessages != null && !fancyChatMessages.messageBuffer.isEmpty()) {
                val bufferSize = fancyChatMessages.messageBuffer.size
                if (bufferSize <= MAX_SCROLLIST_LINES) {
                    renderOverlayGui(fancyChatMessages.getLast(bufferSize), res)
                } else if (MAX_SCROLLIST_LINES < bufferSize) {
                    renderOverlayGui(fancyChatMessages.getSubArray(scroll - MAX_SCROLLIST_LINES, scroll), res)
                } else if (scroll > bufferSize) {
                    renderOverlayGui(fancyChatMessages.getSubArray(bufferSize - MAX_SCROLLIST_LINES, bufferSize), res)
                } else {
                    renderOverlayGui(fancyChatMessages.getLast(MAX_SCROLLIST_LINES.toInt()), res)
                }
            }
        }
    }

    /**
     * Method that resets the scroll-height by reopening the
     * GuiChat.

     * @firedBY -> HudPixelMod.onGuiShown() -> EVENT
     */
    fun openGui() {
        scroll = MAX_SCROLLIST_LINES.toInt()
    }

    /**
     * Fired by the onClientTick event @ HudPixelMod.class
     * updates all the FancyChatObjects and delete them if they are expired
     */
    fun onClientTick() {

        // add a the secound spacer for the party-invite message
        if (spacerPartyRequired!!) {
            this.printMessage(nothing)
            this.printMessage(ChatFormatting.GOLD + spacer)
            spacerPartyRequired = false
        }

        // updates the fancyChatObjects stored in the ArrayList and deletes them if there are outdated
        if (fancyChatObjects != null && !fancyChatObjects.isEmpty()) {
            val fcoBufferRemove = ArrayList<FancyChatObject>()
            for (fco in fancyChatObjects) {
                if (fco.timestamp + displayTimeMs <= System.currentTimeMillis()) {
                    fcoBufferRemove.add(fco)
                }
            }
            for (fco in fcoBufferRemove) {
                fancyChatObjects.remove(fco)
            }
        }
    }

    fun addMessage(s: String) {
        fancyChatMessages.addMinecraftEntry(s)
    }


    /**
     * Fired by the onChat event @ HudPixelMod.class.

     * @param e The chat event
     */
    fun onChat(e: ClientChatReceivedEvent) {
        val message = e.message.unformattedText.replace("§6[Hud§4Admin§6]§4", "") //ffs
        //System.out.println(message);
        // [IF] it is a partyinventation
        if (message.contains(partyInvite1) || message.contains(partyInvite2)) {
            partyChecker(message, e)

            // [IF] the message is blacklisted
        } else if (blacklistChecker(message)) {
            e.isCanceled = true

        } else if (fancyChatChecker(message)) {
            addFancyChatEntry(e.message.formattedText)

            //playes a sound when the player recieves a message
            if (!message.contains(" " + Minecraft.getMinecraft().thePlayer.name + ": ")) {
                SoundManager.playSound(SoundManager.Sounds.NOTIFICATION_SHORT_WOOD)
            }

        } else if (message.startsWith("From ")) {
            addFancyChatEntry(e.message.formattedText)

            SoundManager.playSound(SoundManager.Sounds.NOTIFICATION_SHORT2)

            // [IF] the message is a FancyChatMessage
        } else if (message.startsWith("To ")) {
            addFancyChatEntry(e.message.formattedText)
        } else if (HudPixelMod.IS_DEBUGGING) {
            if (message.startsWith("<aussprechlich2> messagebuffer.clear")) {
                fancyChatMessages.messageBuffer.clear()
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * small methode that adds a FancyChat message to the right buffers and adjust the scrollheight

     * @param s Formatted chat string
     */
    private fun addFancyChatEntry(s: String) {
        fancyChatObjects.add(FancyChatObject(s))
        fancyChatMessages.addMinecraftEntry(s)
        if (scroll < fancyChatMessages.messageBuffer.size && scroll > MAX_SCROLLIST_LINES) {
            scroll++
        }
    }

    /**
     * renders the overlay, when you are in the GuiChat

     * @param renderList renderList with the strings
     * *
     * @param res        the resolution of the screen
     * *
     * @firedBY -> this.onRenderTick()
     */
    private fun renderOverlayGui(renderList: List<String>, res: ScaledResolution) {

        val fontRenderer = FMLClientHandler.instance().client.fontRendererObj
        var yStart = res.scaledHeight - BOTTOM_Y_OFFSET.toInt() - RENDERING_HEIGHT_OFFSET.toInt()
        val xStart = res.scaledWidth - FIELD_WIDTH.toInt() - 2
        val size = renderList.size

        //draws the transparent box behind the text
        RenderUtils.renderBox(xStart, yStart - (size - 1) * RENDERING_HEIGHT_OFFSET,
                FIELD_WIDTH.toInt(), RENDERING_HEIGHT_OFFSET * size)

        //corrects the offset between background and text
        yStart += 1

        //draws the strings & splits them with a method @ FancyChatObject.class
        for (s in renderList) {
            fontRenderer.drawStringWithShadow(s, xStart.toFloat(), yStart.toFloat(), 0xffffff)
            yStart -= RENDERING_HEIGHT_OFFSET.toInt()
        }

        //renders the scrollbar
        if (size >= MAX_SCROLLIST_LINES) renderScrollBar(res)
    }

    /**
     * Don't try to understand it ... I don't understand it either, but somehow it works!
     * Maybe i should try paper&pencil to improve this wonderful code. Let's mark it - perhaps one day!
     * TODO: make it pretty :D

     * @param res screen-resolution
     */
    private fun renderScrollBar(res: ScaledResolution) {

        //gets the height of the scroller in ratio to the currently stored messages
        val boxHeight = (MAX_SCROLLIST_LINES * RENDERING_HEIGHT_OFFSET).toDouble()
        val heightRatio = MAX_SCROLLIST_LINES.toDouble() / fancyChatMessages.messageBuffer.size.toDouble()
        var height: Long = 0
        if (heightRatio > 0) height = Math.round(boxHeight * heightRatio)

        //gets the position of the scroller in ratio to the currently stored messages
        var posRatio = scroll.toDouble() / fancyChatMessages.messageBuffer.size.toDouble()
        if (posRatio > 1) posRatio = 1.0
        val yPos = posRatio * boxHeight

        //start (x/y) of the scrollbar
        val yStart = res.scaledHeight - BOTTOM_Y_OFFSET.toInt() - RENDERING_HEIGHT_OFFSET * MAX_SCROLLIST_LINES.toDouble()
        val xStart = res.scaledWidth - FIELD_WIDTH.toInt() - 2 - 5.toDouble()

        //renders the scrollbar background
        RenderUtils.renderBoxWithColor(xStart.toFloat().toDouble(), yStart.toFloat().toDouble(), 5.0, (MAX_SCROLLIST_LINES * RENDERING_HEIGHT_OFFSET).toDouble(),
                0f, 0f, 0f, 0.5f)

        //renders the scroller on the scrollbar
        RenderUtils.renderBoxWithColor(xStart.toFloat().toDouble(), (yStart + boxHeight - yPos).toFloat().toDouble(), 3.0, height.toInt().toDouble(),
                1f, 1f, 1f, 1f)

    }

    /**
     * renders the overlay when you are ingame

     * @param renderFco renderList with the fco elements
     * *
     * @param res       resolution of the screen
     * *
     * @firedBY -> this.onRenderTick()
     */
    private fun renderOverlay(renderFco: ArrayList<FancyChatObject>, res: ScaledResolution) {

        val fontRenderer = FMLClientHandler.instance().client.fontRendererObj
        var lines = 0

        //gets the number of lines
        for (fco in renderFco) {
            lines += fco.size
        }

        // sets the correct start cords
        var yStart = res.scaledHeight - BOTTOM_Y_OFFSET.toInt() - RENDERING_HEIGHT_OFFSET * lines
        val xStart = res.scaledWidth - FIELD_WIDTH.toInt() - 2

        //draws the transparent box behind the text
        RenderUtils.renderBox(xStart, yStart, FIELD_WIDTH.toInt(), RENDERING_HEIGHT_OFFSET * lines)

        //draws the strings & splits them with a method @ FancyChatObject.class
        for (fco in renderFco) {
            yStart += RENDERING_HEIGHT_OFFSET * fco.drawTextField(xStart, yStart, fontRenderer)
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * @return -> [true] -> if the blacklist contains the message
     */
    private fun fancyChatChecker(m: String): Boolean {
        for (a in FancyChatFilter.fancyChatTriggers) {
            if (m.startsWith(a)) {
                return true
            }
        }
        return false
    }


    /**
     * Performs a better party invite message and plays a sound

     * @param message unformatted message received by the client
     * *
     * @param e       ClientChatReceivedEvent
     */
    private fun partyChecker(message: String, e: ClientChatReceivedEvent) {

        if (message.contains(partyInvite1)) {
            this.printMessage(ChatFormatting.GOLD + spacer)
            this.printMessage(nothing)
            this.printMessage(e.message.formattedText)
            e.isCanceled = true
            SoundManager.playSound(SoundManager.Sounds.NOTIFICATION_OLDSCHOOLMESSAGE)

        } else if (message.contains(partyInvite2)) {
            spacerPartyRequired = true
        }
    }


    /**
     * @return true -> if the blacklist contains the message
     */
    private fun blacklistChecker(m: String): Boolean {
        for (a in FancyChatFilter.blacklistStart) {
            if (m.startsWith(a)) {
                return true
            }
        }
        for (a in FancyChatFilter.blacklistContains) {
            if (m.contains(a)) {
                return true
            }
        }
        for (a in FancyChatFilter.blacklistEnds) {
            if (m.endsWith(a)) {
                return true
            }
        }
        return false
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * prints the message to the clientchat

     * @param message the message
     */
    private fun printMessage(message: String) {
        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessage(
                TextComponentString(message))
    }

    /**
     * Method that handel's the mouse input and sets the scroll-height.
     * Is fired every client tick

     * @firedBY -> this.onClientTick()
     */
    fun handleMouseInput(i: Int) {

        if (i != 0) {
            if (i > 0) {
                if (scroll + SCROLLED_PER_NOTCH > fancyChatMessages.messageBuffer.size - MAX_SCROLLIST_LINES) {
                    scroll = fancyChatMessages.messageBuffer.size
                } else {
                    scroll += SCROLLED_PER_NOTCH.toInt()
                }

            } else if (i < 0) {
                if (scroll - SCROLLED_PER_NOTCH <= MAX_SCROLLIST_LINES) {
                    scroll = MAX_SCROLLIST_LINES.toInt()
                } else {
                    scroll -= SCROLLED_PER_NOTCH.toInt()
                }
            }
        }
    }


    //


}

