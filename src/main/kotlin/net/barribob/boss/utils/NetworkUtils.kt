package net.barribob.boss.utils

import io.netty.buffer.Unpooled
import net.barribob.boss.Mod
import net.barribob.boss.mob.mobs.gauntlet.GauntletEntity
import net.barribob.boss.mob.mobs.obsidilith.ObsidilithEffectHandler
import net.barribob.boss.mob.mobs.obsidilith.ObsidilithUtils
import net.barribob.maelstrom.MaelstromMod
import net.barribob.maelstrom.static_utilities.readVec3d
import net.barribob.maelstrom.static_utilities.writeVec3d
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.network.Packet
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

class NetworkUtils(private val isDevEnvironment: Boolean) {

    companion object {
        private val spawnEntityPacketId = Mod.identifier("spawn_entity")
        private val clientTestPacketId = Mod.identifier("client_test")
        private val changeHitboxPacketId = Mod.identifier("change_hitbox")
        private val playerVelocityPacketId = Mod.identifier("player_velocity")

        fun LivingEntity.sendVelocity(velocity: Vec3d) {
            this.velocity = velocity
            if(this is ServerPlayerEntity) {
                val packet = PacketByteBuf(Unpooled.buffer())
                packet.writeVec3d(velocity)
                ServerPlayNetworking.send(this, playerVelocityPacketId, packet)
            }
        }

        fun GauntletEntity.changeHitbox(open: Boolean) {
            val packet = PacketByteBuf(Unpooled.buffer())
            packet.writeInt(this.entityId)
            packet.writeBoolean(open)
            PlayerLookup.tracking(this).forEach {
                ServerPlayNetworking.send(it, changeHitboxPacketId, packet)
            }
        }
    }

    @Environment(EnvType.CLIENT)
    fun registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(spawnEntityPacketId) { client, _, buf, _ ->
            handleSpawnClientEntity(client, buf)
        }
        ClientPlayNetworking.registerGlobalReceiver(playerVelocityPacketId) { client, _, buf, _ ->
            handlePlayerVelocity(client, buf)
        }
        ClientPlayNetworking.registerGlobalReceiver(changeHitboxPacketId) { client, _, buf, _ ->
            handleChangeHitbox(client, buf)
        }

        if(isDevEnvironment) clientInitDev()
    }

    @Environment(EnvType.CLIENT)
    private fun clientInitDev() {
        ClientPlayNetworking.registerGlobalReceiver(clientTestPacketId) { client, _, _, _ ->
            Mod.networkUtils.testClientCallback(client)
        }
    }

    @Environment(EnvType.CLIENT)
    private fun handlePlayerVelocity(client: MinecraftClient, buf: PacketByteBuf) {
        val velocity = buf.readVec3d()
        client.execute {
            client.player?.velocity = velocity
        }
    }

    private fun packSpawnClientEntity(packet: EntitySpawnS2CPacket): PacketByteBuf {
        val packetData = PacketByteBuf(Unpooled.buffer())
        packet.write(packetData)
        return packetData
    }

    fun createClientEntityPacket(entity: Entity): Packet<*> {
        return ServerPlayNetworking.createS2CPacket(spawnEntityPacketId, packSpawnClientEntity(
            EntitySpawnS2CPacket(entity)))
    }

    @Environment(EnvType.CLIENT)
    private fun handleSpawnClientEntity(client: MinecraftClient, buf: PacketByteBuf) {
        val packet = EntitySpawnS2CPacket()
        packet.read(buf)

        client.execute { VanillaCopies.handleClientSpawnEntity(client, packet) }
    }

    fun testClient(world: ServerWorld, watchPoint: Vec3d) {
        val packetData = PacketByteBuf(Unpooled.buffer())
        PlayerLookup.around(world, watchPoint, 100.0).forEach {
            ServerPlayNetworking.send(it, clientTestPacketId, packetData)
        }
    }

    @Environment(EnvType.CLIENT)
    private fun testClientCallback(client: MinecraftClient) {
        val player = client.player ?: return
        ObsidilithEffectHandler(player, MaelstromMod.clientEventScheduler).handleStatus(ObsidilithUtils.deathStatus)
    }

    @Environment(EnvType.CLIENT)
    private fun handleChangeHitbox(client: MinecraftClient, buf: PacketByteBuf) {
        val entityId = buf.readInt()
        val open = buf.readBoolean()

        client.execute {
            val entity = client.world?.getEntityById(entityId)
            if(entity is GauntletEntity) {
                if(open) entity.hitboxHelper.setOpenHandHitbox() else entity.hitboxHelper.setClosedFistHitbox()
            }
        }
    }
}