package org.waste.of.time

import net.minecraft.SharedConstants
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.nbt.*
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.World
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.WorldTools.LOGGER
import java.io.File

object LevelPropertySerializer {
    fun backupLevelDataFile(session: Session, levelName: String, player: ClientPlayerEntity, world: World) {
        val resultingFile = session.getDirectory(WorldSavePath.ROOT).toFile()
        val dataNbt = serialize(levelName, player, world)
        val levelNbt = NbtCompound()
        levelNbt.put("Data", dataNbt)

        try {
            val newFile = File.createTempFile("level", ".dat", resultingFile)
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = session.getDirectory(WorldSavePath.LEVEL_DAT_OLD).toFile()
            val current = session.getDirectory(WorldSavePath.LEVEL_DAT).toFile()
            Util.backupAndReplace(current, newFile, backup)
        } catch (exception: Exception) {
            LOGGER.error("Failed to save level {}", resultingFile, exception)
        }
    }

    private fun serialize(levelName: String, player: ClientPlayerEntity, world: World): NbtCompound {
        val nbt = NbtCompound()

        player.serverBrand?.let {
            val brandList = NbtList()
            brandList.add(NbtString.of(it))
            nbt.put("ServerBrands", brandList)
        }

        nbt.putBoolean("WasModded", true) // not sure
        // skip removed features

        val gameNbt = NbtCompound()
        gameNbt.putString("Name", SharedConstants.getGameVersion().name)
        gameNbt.putInt("Id", SharedConstants.getGameVersion().saveVersion.id)
        gameNbt.putBoolean("Snapshot", !SharedConstants.getGameVersion().isStable)
        gameNbt.putString("Series", SharedConstants.getGameVersion().saveVersion.series)

        nbt.put("Version", gameNbt)
        NbtHelper.putDataVersion(nbt)

        // skip world gen settings

        nbt.putInt("GameType", player.server?.defaultGameMode?.id ?: 0)
        nbt.putInt("SpawnX", world.levelProperties.spawnX)
        nbt.putInt("SpawnY", world.levelProperties.spawnY)
        nbt.putInt("SpawnZ", world.levelProperties.spawnZ)
        nbt.putFloat("SpawnAngle", world.levelProperties.spawnAngle)
        nbt.putLong("Time", world.time)
        nbt.putLong("DayTime", world.timeOfDay)
        nbt.putLong("LastPlayed", System.currentTimeMillis())
        nbt.putString("LevelName", levelName)
        nbt.putInt("version", 19133)
        nbt.putInt("clearWeatherTime", 0) // not sure
        nbt.putInt("rainTime", 0) // not sure
        nbt.putBoolean("raining", world.isRaining)
        nbt.putInt("thunderTime", 0) // not sure
        nbt.putBoolean("thundering", world.isThundering)
        nbt.putBoolean("hardcore", player.server?.isHardcore ?: false)
        nbt.putBoolean("allowCommands", true) // not sure
        nbt.putBoolean("initialized", true) // not sure
        world.worldBorder.write().writeNbt(nbt)
        nbt.putByte("Difficulty", world.levelProperties.difficulty.id.toByte())
        nbt.putBoolean("DifficultyLocked", false) // not sure
        nbt.put("GameRules", world.levelProperties.gameRules.toNbt())
        nbt.put("DragonFight", NbtCompound()) // not sure

        val playerNbt = NbtCompound()
        nbt.put("Player", player.writeNbt(playerNbt)) // ???

        // skip custom boss events

        nbt.put("ScheduledEvents", NbtCompound()) // not sure
        nbt.putInt("WanderingTraderSpawnDelay", 0) // not sure
        nbt.putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id

        return nbt
    }
}