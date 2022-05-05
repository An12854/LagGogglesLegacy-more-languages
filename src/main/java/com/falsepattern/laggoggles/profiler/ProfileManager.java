package com.falsepattern.laggoggles.profiler;

import com.falsepattern.laggoggles.CommonProxy;
import com.falsepattern.laggoggles.Main;
import com.falsepattern.laggoggles.api.event.LagGogglesEvent;
import com.falsepattern.laggoggles.client.FPSCounter;
import com.falsepattern.laggoggles.packet.ObjectData;
import com.falsepattern.laggoggles.packet.SPacketProfileStatus;
import com.falsepattern.laggoggles.util.Perms;
import com.falsepattern.laggoggles.util.RunInClientThread;
import com.falsepattern.laggoggles.util.RunInServerThread;
import com.falsepattern.laggoggles.util.Side;
import com.falsepattern.laggoggles.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ProfileManager {

    public static TimingManager timingManager = new TimingManager();
    public static final AtomicBoolean PROFILE_ENABLED = new AtomicBoolean(false);
    public static final AtomicReference<ProfileResult> LAST_PROFILE_RESULT = new AtomicReference<>();
    private static final Object LOCK = new Object();
    private static final FPSCounter FPS_COUNTER = new FPSCounter();

    public static ProfileResult runProfiler(int seconds, ScanType type, ICommandSender issuer) throws IllegalStateException{
        try {
            if(PROFILE_ENABLED.get()){
                throw new IllegalStateException("Can't start profiler when it's already running!");
            }

            /* Send status to users */
            SPacketProfileStatus status = new SPacketProfileStatus(true, seconds, issuer.getName());

            new RunInServerThread(new Runnable() {
                @Override
                public void run() {
                    for(EntityPlayerMP user : Perms.getLagGogglesUsers()) {
                        CommonProxy.sendTo(status, user);
                    }
                }
            });
            issuer.sendMessage(new TextComponentString(TextFormatting.GRAY + Main.MODID + TextFormatting.WHITE + ": Profiler started for " + seconds + " seconds."));
            Main.LOGGER.info(Main.MODID + " profiler started by " + issuer.getName() + " (" + seconds + " seconds)");

            long start = System.nanoTime();
            TickCounter.ticks.set(0L);
            timingManager = new TimingManager();
            if(Side.getSide().isClient()) {
                FPS_COUNTER.start();
            }
            PROFILE_ENABLED.set(true);
            Thread.sleep(seconds * 1000);
            PROFILE_ENABLED.set(false);
            long frames = FPS_COUNTER.stop();

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try{
                        ArrayList<Entity> ignoredEntities = new ArrayList<>();
                        ArrayList<TileEntity> ignoredTileEntities = new ArrayList<>();
                        ArrayList<BlockPos> ignoredBlocks = new ArrayList<>();

                        Main.LOGGER.info("Processing results synchronously...");
                        ProfileResult result = new ProfileResult(start, System.nanoTime(), TickCounter.ticks.get(), Side.getSide(), type);
                        if(Side.getSide().isClient()) {
                            result.setFrames(frames);
                        }

                        for(Map.Entry<Integer, TimingManager.WorldData> entry : timingManager.getTimings().entrySet()){
                            int worldID = entry.getKey();
                            WorldServer world = DimensionManager.getWorld(worldID);
                            if(world == null){
                                continue;
                            }
                            for(Map.Entry<UUID, Long> entityTimes : entry.getValue().getEntityTimes().entrySet()){
                                Entity e = world.getEntityFromUuid(entityTimes.getKey());
                                if(e == null){
                                    continue;
                                }
                                try {
                                    result.addData(new ObjectData(
                                            worldID,
                                            e.getName(),
                                            Graphical.formatClassName(e.getClass().toString()),
                                            e.getPersistentID(),
                                            entityTimes.getValue(),
                                            ObjectData.Type.ENTITY)
                                    );
                                }catch (Throwable t){
                                    ignoredEntities.add(e);
                                }
                            }
                            for(Map.Entry<BlockPos, Long> tileEntityTimes : entry.getValue().getBlockTimes().entrySet()){
                                if(world.isBlockLoaded(tileEntityTimes.getKey()) == false){
                                    continue;
                                }
                                TileEntity e = world.getTileEntity(tileEntityTimes.getKey());
                                if(e != null) {
                                    try {
                                        String name;
                                        ITextComponent displayName = e.getDisplayName();
                                        if (displayName != null) {
                                            name = displayName.getFormattedText();
                                        } else {
                                            name = e.getClass().getSimpleName();
                                        }
                                        result.addData(new ObjectData(
                                                worldID,
                                                name,
                                                Graphical.formatClassName(e.getClass().toString()),
                                                e.getPos(),
                                                tileEntityTimes.getValue(),
                                                ObjectData.Type.TILE_ENTITY)
                                        );
                                    }catch (Throwable t){
                                        ignoredTileEntities.add(e);
                                    }
                                }else{
                                    /* The block is not a tile entity, get the actual block. */
                                    try {
                                        IBlockState state = world.getBlockState(tileEntityTimes.getKey());
                                        String name = state.getBlock().getLocalizedName();
                                        result.addData(new ObjectData(
                                                worldID,
                                                name,
                                                Graphical.formatClassName(state.getBlock().getClass().toString()),
                                                tileEntityTimes.getKey(),
                                                tileEntityTimes.getValue(),
                                                ObjectData.Type.BLOCK));
                                    }catch (Throwable t){
                                        ignoredBlocks.add(tileEntityTimes.getKey());
                                    }
                                }
                            }
                        }
                        for(Map.Entry<TimingManager.EventTimings, AtomicLong> entry : timingManager.getEventTimings().entrySet()){
                            result.addData(new ObjectData(entry.getKey(), entry.getValue().get()));
                        }
                        if(result.getSide().isClient()) {
                            insertGuiData(result, timingManager);
                        }
                        result.lock();
                        LAST_PROFILE_RESULT.set(result);
                        synchronized (LOCK){
                            LOCK.notifyAll();
                        }
                        if(ignoredBlocks.size() + ignoredEntities.size() + ignoredTileEntities.size() > 0) {
                            Main.LOGGER.info("Ignored some tracked elements:");
                            Main.LOGGER.info("Entities: " + ignoredEntities);
                            Main.LOGGER.info("Tile entities: " + ignoredTileEntities);
                            Main.LOGGER.info("Blocks in locations: " + ignoredBlocks);
                        }
                    } catch (Throwable e) {
                        Main.LOGGER.error("Woa! Something went wrong while processing results! Please contact Terminator_NL and submit the following error in an issue at github!");
                        e.printStackTrace();
                    }
                }
            };
            Side side = Side.getSide();
            if(side.isServer()){
                new RunInServerThread(task);
            }else if(side.isClient()){
                new RunInClientThread(task);
            }else{
                Main.LOGGER.error("LagGoggles did something amazing. I have no clue how this works, but here's a stacktrace, please submit an issue at github with the stacktrace below!");
                Thread.dumpStack();
            }
            synchronized (LOCK) {
                LOCK.wait();
            }
            MinecraftForge.EVENT_BUS.post(new LagGogglesEvent.LocalResult(LAST_PROFILE_RESULT.get()));
            Main.LOGGER.info("Profiling complete.");
            issuer.sendMessage(new TextComponentString(TextFormatting.GRAY + Main.MODID + TextFormatting.WHITE + ": Profiling complete."));
            return LAST_PROFILE_RESULT.get();
        } catch (Throwable e) {
            Main.LOGGER.error("Woa! Something went wrong while processing results! Please contact Terminator_NL and submit the following error in an issue at github!");
            e.printStackTrace();
            return null;
        }
    }

    public static void insertGuiData(ProfileResult result, TimingManager timings) {
        TreeMap<UUID, Long> entityTimes = timings.getGuiEntityTimings();
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) {
            Long time = entityTimes.get(e.getUniqueID());
            if (time == null) {
                continue;
            }
            result.addData(new ObjectData(
                    e.world.provider.getDimension(),
                    e.getName(),
                    Graphical.formatClassName(e.getClass().toString()),
                    e.getPersistentID(),
                    time,
                    ObjectData.Type.GUI_ENTITY)
            );
        }

        TreeMap<BlockPos, Long> blockTimes = timings.getGuiBlockTimings();
        WorldClient world = Minecraft.getMinecraft().world;
        for (Map.Entry<BlockPos, Long> e: blockTimes.entrySet()) {
            Long time = e.getValue();
            TileEntity entity = world.getTileEntity(e.getKey());
            if(entity != null) {
                String name;
                ITextComponent displayName = entity.getDisplayName();
                if (displayName != null) {
                    name = displayName.getFormattedText();
                } else {
                    name = entity.getClass().getSimpleName();
                }
                result.addData(new ObjectData(
                        entity.getWorld().provider.getDimension(),
                        name,
                        Graphical.formatClassName(entity.getClass().toString()),
                        entity.getPos(),
                        time,
                        ObjectData.Type.GUI_BLOCK)
                );
            }else{
                /* The block is not a tile entity, get the actual block. */
                IBlockState state = world.getBlockState(e.getKey());
                String name = state.getBlock().getLocalizedName();
                result.addData(new ObjectData(
                        world.provider.getDimension(),
                        name,
                        Graphical.formatClassName(state.getBlock().getClass().toString()),
                        e.getKey(),
                        time,
                        ObjectData.Type.GUI_BLOCK));
            }
        }
    }

}
