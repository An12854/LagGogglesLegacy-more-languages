package com.falsepattern.laggoggles.client;

import com.falsepattern.laggoggles.Main;
import com.falsepattern.laggoggles.client.gui.GuiProfile;
import com.falsepattern.laggoggles.packet.CPacketRequestServerData;
import com.falsepattern.laggoggles.packet.SPacketMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MessagePacketHandler implements IMessageHandler<SPacketMessage, CPacketRequestServerData> {

    @Override
    public CPacketRequestServerData onMessage(SPacketMessage msg, MessageContext messageContext) {
        GuiProfile.MESSAGE = msg;
        GuiProfile.MESSAGE_END_TIME = System.currentTimeMillis() + (msg.seconds * 1000);
        GuiProfile.update();
        Main.LOGGER.info("message received from server: " + msg.message);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(TextFormatting.RED + msg.message));
        return new CPacketRequestServerData();
    }
}
