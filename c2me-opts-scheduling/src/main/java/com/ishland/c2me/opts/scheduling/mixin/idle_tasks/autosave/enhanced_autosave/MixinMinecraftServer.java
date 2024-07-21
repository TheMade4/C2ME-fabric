package com.ishland.c2me.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.ishland.c2me.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends ReentrantThreadExecutor<ServerTask> {

    @Shadow protected abstract boolean shouldKeepTicking();

    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Shadow private long timeReference;

    public MixinMinecraftServer(String string) {
        super(string);
    }

    @Unique
    private boolean c2me$shouldKeepSavingChunks() {
        return this.hasRunningTasks() || Util.getMeasuringTimeNano() < ((this.timeReference*1000000L) - 1_000_000L); // reserve 1ms
    }

    /**
     * @author ishland
     * @reason improve task execution when waiting for next tick
     */
    @Inject(method = "runOneTask", at = @At("RETURN"), cancellable = true)
    private void postRunTask(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            cir.setReturnValue(true);
            return;
        }
        if (this.c2me$shouldKeepSavingChunks()) {
            for (ServerWorld serverWorld : this.getWorlds()) {
                if (((IThreadedAnvilChunkStorage) serverWorld.getChunkManager().threadedAnvilChunkStorage).c2me$runOneChunkAutoSave()) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        cir.setReturnValue(false);
    }

}
