package com.palechip.hudpixelmod.gui;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;

import com.palechip.hudpixelmod.HudPixelConfig;
import com.palechip.hudpixelmod.api.interaction.Queue;
import com.palechip.hudpixelmod.api.interaction.callbacks.BoosterResponseCallback;
import com.palechip.hudpixelmod.api.interaction.representations.Booster;

import net.minecraftforge.fml.client.FMLClientHandler;

public class BoosterDisplay implements BoosterResponseCallback{
    private static final int REQUEST_COOLDOWN = 30000; // = 30s
    private static final int REFRESH_TIMEOUT = 120000; // 90s this is how often it refreshes when the chat gui stays open (e.g. when the person is afk)
    private static final String TITLE = EnumChatFormatting.RED + "Boosters";
    private BoosterDisplay instance;
    private long lastRequest;
    private ArrayList<String> renderingStrings;
    private ArrayList<Booster> tippedBoosters;
    private ArrayList<Booster> activeBoosters;
    private boolean isLocked;
    private boolean isInChatGui;
    private boolean hasFailed;
    private boolean isLoading;
    private GuiButton tipAllButton;
    private boolean tipAllLock = false;
    
    public BoosterDisplay() {
        this.instance = this;
        this.renderingStrings = new ArrayList<String>();
        this.tippedBoosters = new ArrayList<Booster>();
        this.activeBoosters = new ArrayList<Booster>();
        // params id:-10 x:doesn't matter y:doesn't matter h:doesn't matter w:20 displayString:Tip all
        this.tipAllButton = new GuiButton(-10, 0,0, 50, 20, "Tip all");
    }

    private void updateRenderStrings() {
        if(!isLocked) {
            renderingStrings.clear();
            renderingStrings.add(TITLE + (isLoading ? "(Loading...)" : (hasFailed ? "(Loading failed!)" : "")));
            for(Booster booster : tippedBoosters) {
                // Update tipped boosters: Remove tipable boosters.
                // This will also remove old boosters!
                if(booster.canTip()) {
                    tippedBoosters.remove(booster);
                }
            }
            for (Booster booster : activeBoosters) {
                // Add all active boosters. Tipped ones are white. Untipped ones are green.
                renderingStrings.add(EnumChatFormatting.GOLD + booster.getGame().getName().replace("Survival Games", "SG").replace(" Champions", "") + ": " + (tippedBoosters.contains(booster) ? EnumChatFormatting.WHITE : EnumChatFormatting.GREEN) + booster.getOwner());
            }
        }
    }

    public void onChatMessage(String textMessage, String formattedMessage) {
        if(HudPixelConfig.useAPI && HudPixelConfig.displayNetworkBoosters) {
            if(textMessage.contains("You sent a") && textMessage.contains("tip of")) {
                // cut the extra stuff
                String name = textMessage.substring(0, textMessage.indexOf(" in"));
                // is it a ranked member
                if(textMessage.contains("]")) {
                    name = name.substring(name.indexOf("]") + 2);
                } else {
                    name = name.substring(name.indexOf(" to ") + 4);
                }
                // set all boosters with this name to tipped
                for (Booster booster : activeBoosters) {
                    if(booster.getOwner().equalsIgnoreCase(name)) {
                        this.tippedBoosters.add(booster);
                        // save the tipping time
                        booster.tip();
                    }
                }

                // refresh the display strings
                this.updateRenderStrings();
            }
        }
    }
    
    public void onClientTick() {
        if(HudPixelConfig.useAPI && HudPixelConfig.displayNetworkBoosters) {
            Minecraft mc = FMLClientHandler.instance().getClient();
            if((mc.currentScreen instanceof GuiChat && !this.isInChatGui) || (this.isInChatGui && System.currentTimeMillis() > this.lastRequest + REFRESH_TIMEOUT)) {
                this.isInChatGui = true;
                this.tipAllButton.visible = true;
                this.tipAllButton.enabled = true;
                this.tipAllLock = false;
                this.requestBoosters();
            }
            if(!(mc.currentScreen instanceof GuiChat)) {
                this.isInChatGui = false;
                this.tipAllButton.visible = false;
                this.tipAllButton.enabled = false;
            }
        }
    }
    
    public void onInitGui(InitGuiEvent event) {
        if(event.gui instanceof GuiChat) {
            event.buttonList.add(tipAllButton);
        }
    }
    
    public void onGuiActionPerformed(ActionPerformedEvent event) {
        if(event.gui instanceof GuiChat && event.button.id == this.tipAllButton.id && !this.tipAllLock) {
            // Only let the button fire once. Then you have to reopen the chat gui.
            this.tipAllLock = true;
            this.tipAllButton.enabled = false;
            // Run /tip all
            FMLClientHandler.instance().getClient().thePlayer.sendChatMessage("/tip all");
        }
    }
    
    public void render(int rectX1, int rectY1, int rectX2, int rectY2, int buttonX, int buttonY, int buttonWidth) {
        // Draw the semi-transparent background
        Gui.drawRect(rectX1, rectY1, rectX2, rectY2, 1610612736);
        // move the tip-all button
        this.tipAllButton.xPosition = buttonX;
        this.tipAllButton.yPosition = buttonY;
        this.tipAllButton.width = buttonWidth;
    }

    private void requestBoosters() {
        if(HudPixelConfig.useAPI && HudPixelConfig.displayNetworkBoosters) {
            if(System.currentTimeMillis() > lastRequest + REQUEST_COOLDOWN) {
                lastRequest = System.currentTimeMillis();
                Queue.getInstance().getBoosters(instance);
                // show them that we are loading date
                this.isLoading = true;
                this.updateRenderStrings();
            }
        }
    }

    public ArrayList<String> getRenderingStrings() {
        return renderingStrings;
    }

    @Override
    public void onBoosterResponse(ArrayList<Booster> boosters) {
        this.isLoading = false;
        if(boosters != null) {
            this.isLocked = true;
            this.hasFailed = false;
            this.activeBoosters.clear();
            // get the active ones
            for (Booster booster : boosters) {
                // is there less than the full duration remaining
                if(booster.getRemainingTime() != booster.getTotalLength()) {
                    // it's active
                    this.activeBoosters.add(booster);
                    // load the name of the owner if only a uuid is available
                    booster.getOwner();
                }
            }
            this.isLocked = false;
        } else {
            this.hasFailed = true;
        }
        // make it display
        this.updateRenderStrings();
    }
}