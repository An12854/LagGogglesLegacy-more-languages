package com.falsepattern.laggoggles;

import com.falsepattern.laggoggles.client.ClientProxy;
import com.falsepattern.laggoggles.mixinhelper.MixinValidator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.Logger;

@Mod(modid = Main.MODID_LOWER, name = Main.MODID, version = Main.VERSION, acceptableRemoteVersions = "*", guiFactory = "com.falsepattern.laggoggles.client.gui.GuiInGameConfigFactory")
@IFMLLoadingPlugin.SortingIndex(1001)
public class Main {
    public static final String MODID = "LagGoggles";
    public static final String MODID_LOWER = "laggoggles";
    public static final String VERSION = "${version}";
    public static Logger LOGGER;

    @SidedProxy(
            modId = Main.MODID_LOWER,
            serverSide = "com.falsepattern.laggoggles.CommonProxy",
            clientSide = "com.falsepattern.laggoggles.client.ClientProxy"
    )
    public static CommonProxy proxy;

    @EventHandler
    public void preinit(FMLPreInitializationEvent e){
        LOGGER = e.getModLog();
        proxy.preinit(e);
        MixinValidator.validate();
        Main.LOGGER.info("Registered sided proxy for: " + (proxy instanceof ClientProxy ? "Client" : "Dedicated server"));
    }

    @EventHandler
    public void postinit(FMLPostInitializationEvent e) {
        proxy.postinit(e);
    }

    @EventHandler
    public void onServerStart(FMLServerStartingEvent e){
        proxy.serverStartingEvent(e);
    }

}
