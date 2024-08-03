package io.github.crthpl;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.impl.dimension.FabricDimensionInternals;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.dedicated.command.StopCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import static net.minecraft.server.command.CommandManager.*;


public class DimensionDestroyer implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("dimension-destroyer");
	private static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("deletedimension")
					.requires(source -> source.hasPermissionLevel(2))
					.then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
						.executes(context -> {
							try {
								final ServerWorld srcDimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
								ArrayList<ServerPlayerEntity> players = new ArrayList<>(srcDimension.getPlayers());
								for (ServerPlayerEntity serverPlayerEntity : players) {
									ServerWorld destDimension = serverPlayerEntity.getServer().getWorld(serverPlayerEntity.getSpawnPointDimension());
									if (destDimension == null) {
										destDimension = serverPlayerEntity.getServer().getWorld(World.OVERWORLD);
									}
									BlockPos position = serverPlayerEntity.getSpawnPointPosition();
									if (position == null) {
										position = destDimension.getSpawnPos();
									}
									Vec3d vecPosition = new Vec3d(position.getX(), position.getY(), position.getZ());
									FabricDimensionInternals.changeDimension(serverPlayerEntity, destDimension, new TeleportTarget(vecPosition, new Vec3d(0, 0, 0), serverPlayerEntity.getSpawnAngle(), 0));
								}
								ServerChunkManager manager = srcDimension.getChunkManager();
//								manager.removePersistentTickets();
//								manager.tick(() -> true, false);
//								srcDimension.save(null, true, false);
//								//ThreadedAnvilChunkStorage anvil = manager.threadedAnvilChunkStorage;
//								manager.tick(() -> true, false);
								srcDimension.getServer().shutdown();
								File saveDir = new File(manager.threadedAnvilChunkStorage.getSaveDir());
								deleteDirectory(saveDir);
								//srcDimension.getServer().runTaskTillTickEnd();
								return 0;
							} catch (Exception e) {
								LOGGER.error("Error deleting dimension", e);
								return 1;
							}
						}))));
	}
}
