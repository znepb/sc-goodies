package io.sc3.goodies.enderstorage

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.context.CommandContext
import io.sc3.goodies.ScGoodies.modId
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Text.translatable
import net.minecraft.util.math.BlockPos

class EnderStorageViewCommand(private: Boolean) : EnderStorageBaseCommand(private) {
  override fun run(ctx: CommandContext<ServerCommandSource>): Int {
    val player = ctx.source.playerOrThrow
    val (inv, frequency) = getInventory(ctx)

    player.openHandledScreen(object : ExtendedScreenHandlerFactory {
      // Don't add viewingPlayers here
      override fun createMenu(syncId: Int, playerInv: PlayerInventory, player: PlayerEntity): ScreenHandler =
        EnderStorageScreenHandler(syncId, playerInv, inv, BlockPos.ORIGIN, frequency)

      override fun getDisplayName(): Text = translatable("block.$modId.ender_storage")

      override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(BlockPos.ORIGIN)
        frequency.toPacket(buf)
      }
    })

    return SINGLE_SUCCESS
  }
}
