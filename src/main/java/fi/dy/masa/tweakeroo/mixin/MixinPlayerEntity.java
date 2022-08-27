package fi.dy.masa.tweakeroo.mixin;

import fi.dy.masa.tweakeroo.Tweakeroo;
import fi.dy.masa.tweakeroo.config.Configs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import fi.dy.masa.tweakeroo.config.FeatureToggle;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity
{
    @Shadow protected abstract boolean clipAtLedge();

    @Shadow public abstract ActionResult interact(Entity entity, Hand hand);

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Inject(method = "method_30263", at = @At("HEAD"), cancellable = true)
    private void restore_1_15_2_sneaking(CallbackInfoReturnable<Boolean> cir)
    {
        if (FeatureToggle.TWEAK_SNEAK_1_15_2.getBooleanValue())
        {
            cir.setReturnValue(this.onGround);
        }
    }

    @Redirect(method = "adjustMovementForSneaking", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/entity/player/PlayerEntity;clipAtLedge()Z", ordinal = 0))
    private boolean fakeSneaking(PlayerEntity entity)
    {
        if (FeatureToggle.TWEAK_FAKE_SNEAKING.getBooleanValue() && ((Object) this) instanceof ClientPlayerEntity)
        {
            return true;
        }

        return this.clipAtLedge();
    }

    private SoundEvent getFallSound(int distance)
    {
        return distance > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;travel(Lnet/minecraft/util/math/Vec3d;)V"))
    public void travel(LivingEntity instance, Vec3d movementInput)
    {
        boolean bl;
        double d = 0.08;
        boolean bl2 = bl = this.getVelocity().y <= 0.0;
        if (bl && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = 0.01;
            this.onLanding();
        }
        FluidState fluidState = this.world.getFluidState(this.getBlockPos());
        if (this.isTouchingWater() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState)) {
            double e = this.getY();
            float f = this.isSprinting() ? 0.9f : this.getBaseMovementSpeedMultiplier();
            float g = 0.02f;
            float h = EnchantmentHelper.getDepthStrider(this);
            if (h > 3.0f) {
                h = 3.0f;
            }
            if (!this.onGround) {
                h *= 0.5f;
            }
            if (h > 0.0f) {
                f += (0.54600006f - f) * h / 3.0f;
                g += (this.getMovementSpeed() - g) * h / 3.0f;
            }
            if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96f;
            }
            this.updateVelocity(g, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            Vec3d vec3d = this.getVelocity();
            if (this.horizontalCollision && this.isClimbing()) {
                vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
            }
            this.setVelocity(vec3d.multiply(f, 0.8f, f));
            Vec3d vec3d2 = this.applyFluidMovingSpeed(d, bl, this.getVelocity());
            this.setVelocity(vec3d2);
            if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + (double)0.6f - this.getY() + e, vec3d2.z)) {
                this.setVelocity(vec3d2.x, 0.3f, vec3d2.z);
            }
        }
        else if (this.isInLava() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState))
        {
            if (Configs.Disable.DISABLE_LAVA_SLOWDOWN.getBooleanValue())
            {
                double e = this.getY();
                float f = this.isSprinting() ? 0.9f : this.getBaseMovementSpeedMultiplier();
                float g = 0.02f;
                float h = EnchantmentHelper.getDepthStrider(this);

                if (h > 3.0f)
                {
                    h = 3.0f;
                }
                if (!this.onGround) {
                    h *= 0.5f;
                }

                if (h > 0.0f) {
                    f += (0.54600006f - f) * h / 3.0f;
                    g += (this.getMovementSpeed() - g) * h / 3.0f;
                }

                this.updateVelocity(g, movementInput);
                this.move(MovementType.SELF, this.getVelocity());
                Vec3d vec3d = this.getVelocity();
                if (this.horizontalCollision && this.isClimbing()) {
                    vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
                }
                this.setVelocity(vec3d.multiply(f, 0.8f, f));
                Vec3d vec3d2 = this.applyFluidMovingSpeed(d, bl, this.getVelocity());
                this.setVelocity(vec3d2);
                if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + (double)0.6f - this.getY() + e, vec3d2.z)) {
                    this.setVelocity(vec3d2.x, 0.3f, vec3d2.z);
                }

                if (isSneaking())
                {
                    knockDownwards();
                }
            }
            else
            {
                Vec3d vec3d3;
                double e = this.getY();
                this.updateVelocity(0.02f, movementInput);
                this.move(MovementType.SELF, this.getVelocity());
                if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
                    this.setVelocity(this.getVelocity().multiply(0.5, 0.8f, 0.5));
                    vec3d3 = this.applyFluidMovingSpeed(d, bl, this.getVelocity());
                    this.setVelocity(vec3d3);
                } else {
                    this.setVelocity(this.getVelocity().multiply(0.5));
                }
                if (!this.hasNoGravity()) {
                    this.setVelocity(this.getVelocity().add(0.0, -d / 4.0, 0.0));
                }
                vec3d3 = this.getVelocity();
                if (this.horizontalCollision && this.doesNotCollide(vec3d3.x, vec3d3.y + (double)0.6f - this.getY() + e, vec3d3.z)) {
                    this.setVelocity(vec3d3.x, 0.3f, vec3d3.z);
                }
            }
        } else if (this.isFallFlying()) {
            double n;
            float o;
            double m;
            Vec3d vec3d4 = this.getVelocity();
            if (vec3d4.y > -0.5) {
                this.fallDistance = 1.0f;
            }
            Vec3d vec3d5 = this.getRotationVector();
            float f = this.getPitch() * ((float)Math.PI / 180);
            double i = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
            double j = vec3d4.horizontalLength();
            double k = vec3d5.length();
            double l = Math.cos(f);
            l = l * l * Math.min(1.0, k / 0.4);
            vec3d4 = this.getVelocity().add(0.0, d * (-1.0 + l * 0.75), 0.0);
            if (vec3d4.y < 0.0 && i > 0.0) {
                m = vec3d4.y * -0.1 * l;
                vec3d4 = vec3d4.add(vec3d5.x * m / i, m, vec3d5.z * m / i);
            }
            if (f < 0.0f && i > 0.0) {
                m = j * (double)(-MathHelper.sin(f)) * 0.04;
                vec3d4 = vec3d4.add(-vec3d5.x * m / i, m * 3.2, -vec3d5.z * m / i);
            }
            if (i > 0.0) {
                vec3d4 = vec3d4.add((vec3d5.x / i * j - vec3d4.x) * 0.1, 0.0, (vec3d5.z / i * j - vec3d4.z) * 0.1);
            }
            this.setVelocity(vec3d4.multiply(0.99f, 0.98f, 0.99f));
            this.move(MovementType.SELF, this.getVelocity());
            if (this.horizontalCollision && !this.world.isClient && (o = (float)((n = j - (m = this.getVelocity().horizontalLength())) * 10.0 - 3.0)) > 0.0f) {
                this.playSound(this.getFallSound((int)o), 1.0f, 1.0f);
                this.damage(DamageSource.FLY_INTO_WALL, o);
            }
            if (this.onGround && !this.world.isClient) {
                this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, false);
            }
        } else {
            BlockPos blockPos = this.getVelocityAffectingPos();
            float p = this.world.getBlockState(blockPos).getBlock().getSlipperiness();
            float f = this.onGround ? p * 0.91f : 0.91f;
            Vec3d vec3d6 = this.applyMovementInput(movementInput, p);
            double q = vec3d6.y;
            if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
                q += (0.05 * (double)(this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - vec3d6.y) * 0.2;
                this.onLanding();
            } else if (!this.world.isClient || this.world.isChunkLoaded(blockPos)) {
                if (!this.hasNoGravity()) {
                    q -= d;
                }
            } else {
                q = this.getY() > (double)this.world.getBottomY() ? -0.1 : 0.0;
            }
            if (this.hasNoDrag()) {
                this.setVelocity(vec3d6.x, q, vec3d6.z);
            } else {
                this.setVelocity(vec3d6.x * (double)f, q * (double)0.98f, vec3d6.z * (double)f);
            }
        }

        this.updateLimbs(this, this instanceof Flutterer);
    }
}
