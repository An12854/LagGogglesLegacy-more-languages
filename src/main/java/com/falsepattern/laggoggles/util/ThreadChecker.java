package com.falsepattern.laggoggles.util;

import com.falsepattern.laggoggles.profiler.TimingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.FMLCommonHandler;

public class ThreadChecker {

    public static TimingManager.EventTimings.ThreadType getThreadType(){
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if(server == null){
            /* No server at all. Multiplayer... probably. */
            if(Minecraft.getMinecraft().isCallingFromMinecraftThread()){
                return TimingManager.EventTimings.ThreadType.CLIENT;
            }
        }else{
            if (server.isDedicatedServer() == true) {
                /* Dedicated server */
                if (server.isCallingFromMinecraftThread()) {
                    return TimingManager.EventTimings.ThreadType.SERVER;
                }
            } else {
                /* Not a dedicated server, we have both the client and server classes. */
                if (server.isCallingFromMinecraftThread()) {
                    return TimingManager.EventTimings.ThreadType.SERVER;
                } else if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
                    return TimingManager.EventTimings.ThreadType.CLIENT;
                }
            }
        }
        return TimingManager.EventTimings.ThreadType.ASYNC;
    }
}
