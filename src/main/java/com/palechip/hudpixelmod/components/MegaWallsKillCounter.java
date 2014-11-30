package com.palechip.hudpixelmod.components;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.util.EnumChatFormatting;

public class MegaWallsKillCounter implements IComponent {
    private static final String KILL_DISPLAY = EnumChatFormatting.AQUA + "Kills: " + EnumChatFormatting.RED;
    private static final String FINAL_KILL_DISPLAY = EnumChatFormatting.BLUE + "Final Kills: " + EnumChatFormatting.RED;
    private static final String ASSISTS_DISPLAY = EnumChatFormatting.AQUA +  "" + EnumChatFormatting.ITALIC +"Assists: " + EnumChatFormatting.DARK_GRAY;
    private static final String FINAL_ASSISTS_DISPLAY = EnumChatFormatting.BLUE +  "" + EnumChatFormatting.ITALIC +"Final Assists: " + EnumChatFormatting.DARK_GRAY;
    public static enum KillType {Normal, Final, Assists, Final_Assists};

    private KillType trackedType;
    private int kills;
    
    // used for the advanced kill tracking
    private static MegaWallsKillCounter normalKillCounter;

    public MegaWallsKillCounter(KillType type) {
        this.trackedType = type;
        if(type == KillType.Normal) {
            normalKillCounter = this;
        }
    }

    @Override
    public void setupNewGame() {
        this.kills = 0;
    }

    @Override
    public void onGameStart() {
    }

    @Override
    public void onGameEnd() {
    }

    @Override
    public void onTickUpdate() {
    }

    @Override
    public void onChatMessage(String textMessage, String formattedMessage) {
        // coin message?, not from tipping
        if(textMessage.startsWith("+") && textMessage.toLowerCase().contains("coins") && !textMessage.toLowerCase().contains("for being generous :)")) {
            switch (this.trackedType) {
            case Normal:
                // exclude wither rushing reward
                if(!textMessage.contains("ASSIST") && !textMessage.contains("FINAL KILL") && !textMessage.contains("Wither Damage")) {
                    this.kills++;
                }
                // some ninja detection for kills over 18
                if(this.kills >= 18 && textMessage.contains("was killed by " + FMLClientHandler.instance().getClient().getSession().getUsername())) {
                    this.kills++;
                }
                break;
            case Final:
                if(!textMessage.contains("ASSIST") && textMessage.contains("FINAL KILL")) {
                    this.kills++;
                    // for the advanced tracking we must subtract a normal kill
                    // every time it turns out to be a final kill
                    if(normalKillCounter.kills > 18) {
                        normalKillCounter.kills -= 1;
                    }
                }
                break;
            case Assists:
                if(textMessage.contains("ASSIST") && !textMessage.contains("FINAL KILL")) {
                    this.kills++;
                }
                break;
            case Final_Assists:
                if(textMessage.contains("ASSIST") && textMessage.contains("FINAL KILL")) {
                    this.kills++;
                }
                break;
            }
        }
    }

    @Override
    public String getRenderingString() {
        switch (this.trackedType) {
        case Normal:
            return KILL_DISPLAY + this.kills;
        case Final:
            return FINAL_KILL_DISPLAY + this.kills;
        case Assists:
            return ASSISTS_DISPLAY + this.kills;
        case Final_Assists:
            return FINAL_ASSISTS_DISPLAY + this.kills;
        }
        return "";
    }

}
