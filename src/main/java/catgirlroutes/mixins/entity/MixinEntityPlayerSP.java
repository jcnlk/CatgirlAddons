package catgirlroutes.mixins.entity;

import catgirlroutes.events.impl.MotionUpdateEvent;
import catgirlroutes.module.impl.player.NoDebuff;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {EntityPlayerSP.class})
public abstract class MixinEntityPlayerSP extends EntityPlayer {
    private double oldPosX;
    private double oldPosY;
    private double oldPosZ;

    private float oldYaw;
    private float oldPitch;

    private boolean oldOnGround;


    public MixinEntityPlayerSP(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @Inject(method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true)
    public void onUpdatePre(CallbackInfo ci) {

        this.oldPosX = this.posX;
        this.oldPosY = this.posY;
        this.oldPosZ = this.posZ;

        this.oldYaw = this.rotationYaw;
        this.oldPitch = this.rotationPitch;

        this.oldOnGround = this.onGround;

        MotionUpdateEvent.Pre motionUpdateEvent = new MotionUpdateEvent.Pre(this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ, this.rotationYaw, this.rotationPitch, this.onGround);

        if (MinecraftForge.EVENT_BUS.post(motionUpdateEvent)) ci.cancel();

        this.posX = motionUpdateEvent.x;
        this.posY = motionUpdateEvent.y;
        this.posZ = motionUpdateEvent.z;

        this.rotationYaw = motionUpdateEvent.yaw;
        this.rotationPitch = motionUpdateEvent.pitch;

        this.onGround = motionUpdateEvent.onGround;
    }

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    public void onUpdatePost(CallbackInfo ci) {
        this.posX = this.oldPosX;
        this.posY = this.oldPosY;
        this.posZ = this.oldPosZ;

        this.rotationYaw = this.oldYaw;
        this.rotationPitch = this.oldPitch;

        this.onGround = this.oldOnGround;

        MotionUpdateEvent.Post motionUpdateEvent = new MotionUpdateEvent.Post(posX, posY, posZ, motionX, motionY, motionZ, rotationYaw, rotationPitch, onGround);

        if (MinecraftForge.EVENT_BUS.post(motionUpdateEvent)) ci.cancel();

        this.posX = motionUpdateEvent.x;
        this.posY = motionUpdateEvent.y;
        this.posZ = motionUpdateEvent.z;

        this.rotationYaw = motionUpdateEvent.yaw;
        this.rotationPitch = motionUpdateEvent.pitch;

        this.onGround = motionUpdateEvent.onGround;
    }

    @Redirect(method = {"pushOutOfBlocks"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;noClip:Z"))
    public boolean shouldPrevent(EntityPlayerSP instance) {
        return NoDebuff.INSTANCE.getNoPush();
    }
}