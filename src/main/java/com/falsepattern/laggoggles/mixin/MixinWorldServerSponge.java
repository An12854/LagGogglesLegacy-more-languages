package com.falsepattern.laggoggles.mixin;

import com.falsepattern.laggoggles.mixinhelper.extended.DynamicMethodReplacer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.world.WorldServerBridge;
import org.spongepowered.common.event.tracking.TrackingUtil;

import java.util.Random;

import static com.falsepattern.laggoggles.profiler.ProfileManager.PROFILE_ENABLED;
import static com.falsepattern.laggoggles.profiler.ProfileManager.timingManager;

@SuppressWarnings("ReferenceToMixin")
@Mixin(value = WorldServer.class, priority = 1001)
public abstract class MixinWorldServerSponge extends World {

    protected MixinWorldServerSponge(ISaveHandler a, WorldInfo b, WorldProvider c, Profiler d, boolean e) {
        super(a, b, c, d, e);
        throw new RuntimeException("Mixins cannot be instantiated.");
    }

    @DynamicMethodReplacer.RedirectMethodCalls(nameRegexDeobf = "randomTick", nameRegexObf = "func_180645_a", convertSelf = true)
    @Dynamic(value = "Overwritten by SpongeForge", mixin = org.spongepowered.common.mixin.core.world.WorldServerMixin.class)
    private void randomBlockTickRedirectorVanilla(Block block, World world, BlockPos pos, IBlockState state, Random random){
        long startTime = System.nanoTime();
        block.randomTick(world, pos, state, random);
        if(PROFILE_ENABLED.get()) {
            timingManager.addBlockTime(world.provider.getDimension(), pos, System.nanoTime() - startTime);
        }
    }

    @DynamicMethodReplacer.RedirectMethodCalls(nameRegexDeobf = "randomTickBlock", nameRegexObf = "randomTickBlock")
    @Dynamic(value = "Overwritten by SpongeForge", mixin = org.spongepowered.common.mixin.core.world.WorldServerMixin.class)
    private static void randomBlockTickRedirectorSponge(WorldServerBridge bridge, Block block, BlockPos pos, IBlockState state, Random random){
        long startTime = System.nanoTime();
        TrackingUtil.randomTickBlock(bridge, block, pos, state, random);
        if(PROFILE_ENABLED.get()) {
            timingManager.addBlockTime(bridge.bridge$getDimensionId(), pos, System.nanoTime() - startTime);
        }
    }

    @DynamicMethodReplacer.RedirectMethodCalls(nameRegexDeobf = "updateTick", nameRegexObf = "func_180650_b", convertSelf = true)
    @Dynamic(value = "Overwritten by SpongeForge", mixin = org.spongepowered.common.mixin.core.world.WorldServerMixin.class)
    private void normalBlockTickRedirectorVanilla(Block block, World world, BlockPos pos, IBlockState state, Random random){
        long startTime = System.nanoTime();
        block.updateTick(world, pos, state, random);
        if(PROFILE_ENABLED.get()) {
            timingManager.addBlockTime(world.provider.getDimension(), pos, System.nanoTime() - startTime);
        }
    }

    @DynamicMethodReplacer.RedirectMethodCalls(nameRegexDeobf = "updateTickBlock", nameRegexObf = "updateTickBlock")
    @Dynamic(value = "Overwritten by SpongeForge", mixin = org.spongepowered.common.mixin.core.world.WorldServerMixin.class)
    private static void normalBlockTickRedirectorSponge(WorldServerBridge bridge, Block block, BlockPos pos, IBlockState state, Random random){
        long startTime = System.nanoTime();
        TrackingUtil.updateTickBlock(bridge, block, pos, state, random);
        if(PROFILE_ENABLED.get()) {
            timingManager.addBlockTime(bridge.bridge$getDimensionId(), pos, System.nanoTime() - startTime);
        }
    }
}