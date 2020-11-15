package net.barribob.invasion

import net.barribob.invasion.mob.GeoModel
import net.barribob.invasion.mob.MaelstromScoutEntity
import net.barribob.invasion.mob.utils.ModGeoRenderer
import net.barribob.maelstrom.MaelstromMod
import net.barribob.maelstrom.general.io.ConsoleLogger
import net.barribob.maelstrom.mob.ai.JumpToTargetGoal
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import software.bernie.geckolib3.GeckoLib

object Invasions {
    const val MODID = "maelstrom_invasions"

    val LOGGER = ConsoleLogger(LogManager.getLogger())
}

object Entities {
    val MAELSTROM_SCOUT: EntityType<MaelstromScoutEntity> = Registry.register(
        Registry.ENTITY_TYPE,
        Identifier(Invasions.MODID, "maelstrom_scout"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ::MaelstromScoutEntity)
            .dimensions(EntityDimensions.fixed(0.9F, 1.8F)).build()
    )
}

@Suppress("unused")
fun init() {
    GeckoLib.initialize()

    MaelstromMod.aiManager.addGoalInjection(EntityType.getId(Entities.MAELSTROM_SCOUT).toString()) { entity ->
        Pair(
            2,
            JumpToTargetGoal(entity)
        )
    }
    FabricDefaultAttributeRegistry.register(Entities.MAELSTROM_SCOUT, HostileEntity.createHostileAttributes())
}

@Environment(EnvType.CLIENT)
@Suppress("unused")
fun clientInit() {
    EntityRendererRegistry.INSTANCE.register(Entities.MAELSTROM_SCOUT) { entityRenderDispatcher, _ ->
        ModGeoRenderer<MaelstromScoutEntity>(
            entityRenderDispatcher, GeoModel(
                Identifier(Invasions.MODID, "geo/maelstrom_scout.geo.json"),
                Identifier(Invasions.MODID, "textures/entity/maelstrom_scout.png"),
                Identifier(Invasions.MODID, "animations/scout.animation.json")
            )
        )
    }
}