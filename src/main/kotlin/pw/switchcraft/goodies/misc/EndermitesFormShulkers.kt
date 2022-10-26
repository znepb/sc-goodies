package pw.switchcraft.goodies.misc

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.ai.goal.Goal.Control.TARGET
import net.minecraft.entity.ai.goal.WanderAroundGoal
import net.minecraft.entity.mob.EndermiteEntity
import net.minecraft.entity.mob.ShulkerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.NORTH
import pw.switchcraft.goodies.mixin.LivingEntityAccessor
import pw.switchcraft.goodies.mixin.MobEntityAccessor
import pw.switchcraft.goodies.mixin.ShulkerAccessor
import java.util.*

// TODO: Config for this
private const val CHANCE = 0.005

object EndermitesFormShulkers {
  fun init() {
    ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
      if (entity is EndermiteEntity) {
        val gs = (entity as MobEntityAccessor).goalSelector

        if (gs.goals.none { it.goal is FormShulkerGoal }) {
          gs.add(2, FormShulkerGoal(entity))
        }
      }
    }
  }

  class FormShulkerGoal(private val endermite: EndermiteEntity) : WanderAroundGoal(endermite, 1.0, 10) {
    private var blockDir: Direction = NORTH
    private var merging = false

    init {
      this.controls = EnumSet.of(TARGET)
    }

    override fun canStart(): Boolean {
      if (endermite.attacking != null) return false
      if (!endermite.navigation.isIdle) return false

      val rng = endermite.random
      if (rng.nextDouble() < CHANCE) {
        blockDir = Direction.random(rng)

        if (canMerge(blockDir).first) {
          merging = true
          return true
        }
      }

      merging = false
      return super.canStart()
    }

    override fun shouldContinue() = !merging && super.shouldContinue()

    @Suppress("KotlinConstantConditions")
    override fun start() {
      if (!merging) {
        super.start() // Just wander randomly if we're not merging now
      } else {
        val world = endermite.world
        val (canMerge, blockPos) = canMerge(blockDir)

        if (canMerge) {
          world.removeBlock(blockPos, false)

          val shulker = ShulkerEntity(EntityType.SHULKER, world)
          (shulker as ShulkerAccessor).invokeSetAttachedFace(blockDir)
          shulker.setPosition(blockPos.x.toDouble() + 0.5, blockPos.y.toDouble() + 0.5, blockPos.z.toDouble() + 0.5)
          world.spawnEntity(shulker)

          if (endermite.hasCustomName()) {
            shulker.customName = endermite.customName
          }

          (endermite as LivingEntityAccessor).invokeAddDeathParticles()
          endermite.remove(Entity.RemovalReason.DISCARDED)
        }
      }
    }

    private fun canMerge(dir: Direction): Pair<Boolean, BlockPos> {
      val pos = endermite.pos
      val blockPos = BlockPos(pos.x, pos.y + 0.5, pos.z).offset(dir)
      val state = endermite.world.getBlockState(blockPos)
      return state.isOf(Blocks.PURPUR_BLOCK) to blockPos
    }
  }
}
