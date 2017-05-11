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
package com.palechip.hudpixelmod.api.interaction

import com.palechip.hudpixelmod.HudPixelMod
import com.palechip.hudpixelmod.api.interaction.callbacks.ApiKeyLoadedCallback
import com.palechip.hudpixelmod.extended.HudPixelExtendedEventHandler
import com.palechip.hudpixelmod.extended.util.IEventHandler
import com.palechip.hudpixelmod.extended.util.LoggerHelper
import com.palechip.hudpixelmod.util.ChatMessageComposer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.text.TextFormatting
import net.minecraft.util.text.event.ClickEvent
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import java.io.*

@SideOnly(Side.CLIENT)
object ApiKeyHandler: IEventHandler {

    init {
        HudPixelExtendedEventHandler.registerIEvent(this)
    }

    fun loadKey(callback: ApiKeyLoadedCallback) {
        ApiKeyHandler.callback = callback
        // load the api in a separate thread
        Thread(Runnable { loadAPIKey() }).start()
    }

    @Throws(Throwable::class)
    override fun onChatReceived(e: ClientChatReceivedEvent) {
        val message = e.message.unformattedText
        if (!message.startsWith("Your new API key is ")) return

        // extract the key
        apiKey = message.substring(message.indexOf("is ") + 3)

        // let the callback know
        callback!!.ApiKeyLoaded(false, apiKey)

        // and save it
        Thread(Runnable { saveAPIKey() }).start()

        // tell the user
        ChatMessageComposer("API key successfully detected and saved. The API is ready for usage.", TextFormatting.GREEN).send()

    }

    /**
     * Gets called from a separate thread for loading the api key
     */
    private fun loadAPIKey() {
        try {
            LoggerHelper.logInfo("[API][Key] loading key!")
            // make sure the path exists
            val path = File(API_KEY_STORAGE_PATH!!)
            val file = File(API_KEY_STORAGE_FILE!!)
            if (!path.exists()) {
                path.mkdirs()
            }

            // isHypixelNetwork if the file exists
            if (!file.exists()) {
                // there is no file so there can't be an api key
                // create it
                file.createNewFile()
                this.resetApiFile(file)
                isLoadingFailed = true
                callback!!.ApiKeyLoaded(true, null)
                requestApiKey()
                return
            }
            // read the key
            val reader = BufferedReader(FileReader(file))
            val key = reader.readLine()
            reader.close()
            // make sure the content can be a valid key
            if (key == null || key == EMPTY_FILE_CONTENT || !this.isCorrectKeyFormat(key.replace(" ", ""))) {
                this.resetApiFile(file)
                isLoadingFailed = true
                callback!!.ApiKeyLoaded(true, null)
                requestApiKey()
                return
            }
            apiKey = key.replace(" ", "")
            isLoadingFailed = false
            callback!!.ApiKeyLoaded(false, apiKey)
        } catch (e: Exception) {
            HudPixelMod.logError("Critical error when reading the api key file: ")
            e.printStackTrace()
        }

    }

    /**
     * Gets called from a separate thread for loading the api key
     */
    private fun saveAPIKey() {
        try {
            // we don't isHypixelNetwork whether the file exists since we already do it on startup
            // there is no way the user deletes the file after startup unintentionally
            val writer = PrintWriter(File(API_KEY_STORAGE_FILE!!))
            writer.write(apiKey!!)
            writer.close()
        } catch (e: Exception) {
            HudPixelMod.logError("Critical error when storing the api key file: ")
            e.printStackTrace()
        }

    }

    /**
     * Empties the api key file and adds a message how to use it.
     */
    @Throws(FileNotFoundException::class)
    private fun resetApiFile(file: File) {
        // empty the file
        val writer = PrintWriter(file)
        // fill the file with the empty content
        writer.write(EMPTY_FILE_CONTENT)
        writer.flush()
        writer.close()
    }

    /**
     * Verifies that the key has the correct pattern.
     */
    private fun isCorrectKeyFormat(key: String): Boolean {
        return key.matches(API_KEY_PATTERN.toRegex())
    }

    override fun onClientTick() {

    }

    override fun everyTenTICKS() {

    }

    override fun everySEC() {

    }

    override fun everyFiveSEC() {

    }

    override fun everyMIN() {

    }

    override fun onRender() {

    }

    override fun handleMouseInput(i: Int, mX: Int, mY: Int) {

    }

    override fun onMouseClick(mX: Int, mY: Int) {

    }

    override fun openGUI(guiScreen: GuiScreen?) {

    }

    override fun onConfigChanged() {

    }

        private var API_KEY_STORAGE_PATH: String? = null
        private var API_KEY_STORAGE_FILE: String? = null
        private val API_KEY_REQUEST_MESSAGE_1 = "No API key found. This key is necessary for some cool features."
        private val API_KEY_REQUEST_MESSAGE_2_PART1 = "Simply do "
        private val API_KEY_REQUEST_MESSAGE_2_PART2 = " for creating a new one."
        private val API_KEY_REQUEST_MESSAGE_3 = "You can also add your key manually to config\\hypixel_api_key.txt."
        private val API_KEY_REQUEST_MESSAGE_4 = "If you don't want to use the API features, you can disable \"useAPI\" in the config"
        private val EMPTY_FILE_CONTENT = "Replace this with the api key or do /api on Hypixel Network. This File gets reset when a key doesn't work."
        private val API_KEY_PATTERN = "[a-f0-9]{8}[-]([a-f0-9]{4}[-]){3}[a-f0-9]{12}"

        init {
            try {
                // in the config folder
                API_KEY_STORAGE_PATH = FMLClientHandler.instance().client.mcDataDir.canonicalPath + File.separatorChar + "config" + File.separatorChar
                // a file called hypixel_api_key.txt
                API_KEY_STORAGE_FILE = API_KEY_STORAGE_PATH!! + "hypixel_api_key.txt"
            } catch (e: IOException) {
                HudPixelMod.logError("Critical error when finding the api key file: ")
                e.printStackTrace()
            }

        }

        // this gets set to true when the loading fails but is finished
        var isLoadingFailed = false
            private set
        var apiKey: String? = null
        private var callback: ApiKeyLoadedCallback? = null


        /**
         * Asks the user to do /api
         */
        fun requestApiKey() {
            ChatMessageComposer(API_KEY_REQUEST_MESSAGE_1).send()
            ChatMessageComposer(API_KEY_REQUEST_MESSAGE_2_PART1).appendMessage(ChatMessageComposer("/api new", TextFormatting.RED).makeClickable(ClickEvent.Action.RUN_COMMAND, "/api new", ChatMessageComposer("Runs ", TextFormatting.GRAY).appendMessage(ChatMessageComposer("/api new", TextFormatting.RED)))).appendMessage(ChatMessageComposer(API_KEY_REQUEST_MESSAGE_2_PART2)).send()
            ChatMessageComposer(API_KEY_REQUEST_MESSAGE_3).send()
            ChatMessageComposer(API_KEY_REQUEST_MESSAGE_4).send()
        }

}