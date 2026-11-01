package com.mcradio;

import com.mcradio.McRadio.StationBlockEntity.Mode;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Mod entry point. Runs on both physical sides and therefore must never touch client-only classes.
 */
@Mod(McRadio.MOD_ID)
public final class McRadio {
    public static final String MOD_ID = "mcradio";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public McRadio() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // config/mc_radio/tracks/ is created on every side, so the folder always exists after the first launch.
        TrackLibrary.ensureFolder();

        ModRegistry.register(modBus);
        RadioNetwork.init();
        LOGGER.info("MC Radio initialised; track folder: {}", TrackLibrary.folder());
    }

    // ==========================================================================
    // Everything below is part of the mod; nested so the whole mod is one source file.
    // ==========================================================================

    /** Wire identifiers for the audio payload carried by {@code AudioChunkPacket}. */
    public static final class AudioCodec {
        /** Raw MPEG audio bytes, decoded on the client by JLayer. */
        public static final int MP3 = 0;
        /** 16 kHz, mono, G.711 mu-law (one byte per sample): used for live microphone audio. */
        public static final int ULAW_16K = 1;

        public static final int VOICE_SAMPLE_RATE = 16000;

        private AudioCodec() {
        }
    }


    /** G.711 mu-law companding used for the live microphone stream (8 bit per sample on the wire). */
    public static final class UlawCodec {
        private static final int BIAS = 0x84;
        private static final int CLIP = 32635;
        private static final short[] DECODE_TABLE = new short[256];

        static {
            for (int i = 0; i < 256; i++) {
                int value = ~i & 0xFF;
                int sign = value & 0x80;
                int exponent = (value >> 4) & 0x07;
                int mantissa = value & 0x0F;
                int sample = ((mantissa << 3) + BIAS) << exponent;
                sample -= BIAS;
                DECODE_TABLE[i] = (short) (sign != 0 ? -sample : sample);
            }
        }

        private UlawCodec() {
        }

        public static short decode(byte encoded) {
            return DECODE_TABLE[encoded & 0xFF];
        }

        public static byte encode(short pcm) {
            int sample = pcm;
            int sign = (sample >> 8) & 0x80;
            if (sign != 0) {
                sample = -sample;
            }
            if (sample > CLIP) {
                sample = CLIP;
            }
            sample += BIAS;
            int exponent = 7;
            for (int mask = 0x4000; (sample & mask) == 0 && exponent > 0; mask >>= 1) {
                exponent--;
            }
            int mantissa = (sample >> (exponent + 3)) & 0x0F;
            return (byte) ~(sign | (exponent << 4) | mantissa);
        }

        /** The encoded value for digital silence. */
        public static byte silence() {
            return (byte) 0xFF;
        }
    }


    /** Access to the {@code config/mc_radio/tracks/} folder. All lookups are sandboxed to that folder. */
    public static final class TrackLibrary {
        public static final int MAX_TRACKS = 512;
        public static final int MAX_NAME_LENGTH = 200;
        public static final long MAX_FILE_BYTES = 96L * 1024L * 1024L;

        private TrackLibrary() {
        }

        public static Path folder() {
            return FMLPaths.CONFIGDIR.get().resolve("mc_radio").resolve("tracks");
        }

        public static void ensureFolder() {
            try {
                Files.createDirectories(folder());
            } catch (IOException e) {
                McRadio.LOGGER.error("Could not create track folder {}", folder(), e);
            }
        }

        public static boolean isMp3Name(String name) {
            return name.toLowerCase(Locale.ROOT).endsWith(".mp3");
        }

        /** Sorted file names of all playable tracks. */
        public static List<String> list() {
            ensureFolder();
            try (Stream<Path> files = Files.list(folder())) {
                return files.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(TrackLibrary::isMp3Name)
                        .filter(name -> name.length() <= MAX_NAME_LENGTH)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .limit(MAX_TRACKS)
                        .toList();
            } catch (IOException e) {
                McRadio.LOGGER.warn("Could not list track folder {}", folder(), e);
                return List.of();
            }
        }

        /** Resolves a track name sent by a client to a real file, rejecting anything outside the folder. */
        public static Optional<Path> resolve(String name) {
            if (name == null || name.isEmpty() || name.length() > MAX_NAME_LENGTH || !isMp3Name(name)) {
                return Optional.empty();
            }
            if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0 || name.contains("..")) {
                return Optional.empty();
            }
            Path root = folder().toAbsolutePath().normalize();
            Path file = root.resolve(name).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return Optional.empty();
            }
            return Optional.of(file);
        }
    }


    /**
     * A fully loaded MP3 file on the server. The server never decodes audio for playback; it only scans the
     * frame headers once to learn the average byte rate, so that raw MP3 bytes can be streamed in real time.
     */
    public static final class Mp3Source {
        private static final ExecutorService IO = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "mcradio-mp3-loader");
            thread.setDaemon(true);
            return thread;
        });

        private final byte[] data;
        private final int start;
        private final int end;
        private final double bytesPerSecond;

        private Mp3Source(byte[] data, int start, int end, double bytesPerSecond) {
            this.data = data;
            this.start = start;
            this.end = end;
            this.bytesPerSecond = bytesPerSecond;
        }

        public static CompletableFuture<Mp3Source> loadAsync(Path path) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return load(path);
                } catch (IOException | JavaLayerException e) {
                    throw new CompletionException(e);
                }
            }, IO);
        }

        static Mp3Source load(Path path) throws IOException, JavaLayerException {
            long size = Files.size(path);
            if (size > TrackLibrary.MAX_FILE_BYTES) {
                throw new IOException("Track is too large (" + size + " bytes): " + path.getFileName());
            }
            byte[] data = Files.readAllBytes(path);
            int start = id3v2Length(data);
            int end = data.length;
            if (end - 128 > start && data[end - 128] == 'T' && data[end - 127] == 'A' && data[end - 126] == 'G') {
                end -= 128;
            }
            if (end <= start) {
                throw new IOException("Track contains no audio data: " + path.getFileName());
            }

            double milliseconds = 0.0;
            int frames = 0;
            Bitstream bitstream = new Bitstream(new ByteArrayInputStream(data, start, end - start));
            try {
                while (true) {
                    Header header = bitstream.readFrame();
                    if (header == null) {
                        break;
                    }
                    milliseconds += header.ms_per_frame();
                    frames++;
                    bitstream.closeFrame();
                }
            } catch (JavaLayerException e) {
                if (frames == 0) {
                    throw e;
                }
            } finally {
                try {
                    bitstream.close();
                } catch (JavaLayerException ignored) {
                    // nothing to release
                }
            }
            if (frames == 0 || milliseconds <= 0.0) {
                throw new IOException("No MPEG audio frames found in " + path.getFileName());
            }
            double bytesPerSecond = (end - start) / (milliseconds / 1000.0);
            bytesPerSecond = Math.max(4000.0, Math.min(80000.0, bytesPerSecond));
            return new Mp3Source(data, start, end, bytesPerSecond);
        }

        private static int id3v2Length(byte[] data) {
            if (data.length > 10 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
                int size = ((data[6] & 0x7F) << 21) | ((data[7] & 0x7F) << 14) | ((data[8] & 0x7F) << 7) | (data[9] & 0x7F);
                boolean footer = (data[5] & 0x10) != 0;
                return Math.min(10 + size + (footer ? 10 : 0), data.length);
            }
            return 0;
        }

        public byte[] data() {
            return data;
        }

        /** Offset of the first audio byte (after any ID3v2 tag). */
        public int start() {
            return start;
        }

        /** Offset one past the last audio byte (before any ID3v1 tag). */
        public int end() {
            return end;
        }

        public double bytesPerSecond() {
            return bytesPerSecond;
        }
    }


    /** Range model of the radio system. All distances are in blocks. */
    public static final class RadioPhysics {
        public static final int MIN_FREQUENCY = 1;
        public static final int MAX_FREQUENCY = 9999;
        public static final int DEFAULT_FREQUENCY = 101;

        public static final int STATION_BASE_RADIUS = 48;
        public static final int REPEATER_BASE_RADIUS = 96;
        public static final int RADIUS_PER_TRANSMITTER = 32;
        public static final int MAX_TRANSMITTERS = 8;

        /** Players within this distance of a receiver that has a signal are sent the audio stream. */
        public static final int LISTEN_RANGE = 64;

        /** Direct range between two walkie-talkies. Further than that, a repeater has to relay the transmission. */
        public static final int WALKIE_RANGE = 300;
        /** Radius of an active Signal Jammer. */
        public static final int JAMMER_RADIUS = 64;
        /** A Radio Scanner overhears relayed traffic of repeaters closer than this. */
        public static final int SCANNER_RANGE = 32;
        /** A computer can be linked to a distributor at most this far away. */
        public static final int COMPUTER_LINK_RANGE = 64;

        private RadioPhysics() {
        }

        public static int clampFrequency(int frequency) {
            return Math.max(MIN_FREQUENCY, Math.min(MAX_FREQUENCY, frequency));
        }

        public static int clampVolume(int percent) {
            return Math.max(0, Math.min(100, percent));
        }

        /**
         * Number of Signal Transmitters that are connected to {@code origin} through a chain of face-adjacent
         * transmitter blocks (antenna array). Capped at {@link #MAX_TRANSMITTERS}.
         */
        public static int transmitterCount(Level level, BlockPos origin) {
            Set<BlockPos> visited = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            visited.add(origin);
            queue.add(origin);
            int count = 0;
            while (!queue.isEmpty() && count < MAX_TRANSMITTERS) {
                BlockPos current = queue.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos neighbour = current.relative(direction);
                    if (!visited.add(neighbour)) {
                        continue;
                    }
                    if (level.isLoaded(neighbour) && level.getBlockState(neighbour).getBlock() instanceof TransmitterBlock) {
                        count++;
                        queue.add(neighbour);
                        if (count >= MAX_TRANSMITTERS) {
                            break;
                        }
                    }
                }
            }
            return count;
        }

        public static int stationRadius(Level level, BlockPos pos) {
            return STATION_BASE_RADIUS + RADIUS_PER_TRANSMITTER * transmitterCount(level, pos);
        }

        public static int repeaterRadius(Level level, BlockPos pos) {
            return REPEATER_BASE_RADIUS + RADIUS_PER_TRANSMITTER * transmitterCount(level, pos);
        }
    }


    /** Serverbound: the open station GUI asks for the list of MP3 files in {@code config/mc_radio/tracks/}. */
    public static record TrackListRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, TrackListRequestPacket> CODEC =
                StreamCodec.unit(new TrackListRequestPacket());

        public static void handle(TrackListRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender != null && sender.containerMenu instanceof RadioDeviceMenu) {
                    RadioNetwork.sendToPlayer(sender, new TrackListPacket(TrackLibrary.list()));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: file names available on the server. */
    public static record TrackListPacket(List<String> tracks) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TrackListPacket> CODEC =
                StreamCodec.ofMember(TrackListPacket::write, TrackListPacket::new);

        public TrackListPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<String> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), TrackLibrary.MAX_TRACKS);
            List<String> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(buf.readUtf(TrackLibrary.MAX_NAME_LENGTH));
            }
            return List.copyOf(result);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(tracks.size());
            for (String track : tracks) {
                buf.writeUtf(track, TrackLibrary.MAX_NAME_LENGTH);
            }
        }

        public static void handle(TrackListPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.trackList(message.tracks()));
            context.setPacketHandled(true);
        }
    }


    /**
     * Serverbound: every GUI action of the configurable devices. The server only honours it while the sender
     * has the matching device menu open (which itself enforces distance and block validity). Distributor
     * actions sent from a computer GUI are applied to the distributor the computer is linked to.
     */
    public static record ConfigPacket(BlockPos pos, Action action, int a, int b, String text) {
        public static final int MAX_TEXT = 128;

        public enum Action {
            /** a = frequency, b = mode ordinal */
            STATION_APPLY,
            /** text = track file name */
            STATION_BROADCAST,
            STATION_STOP,
            /** text = message to display to players near tuned receivers */
            STATION_TEXT,
            /** a = frequency, b = volume percent */
            RECEIVER_SET,
            /** a = input frequency, b = output frequency */
            REPEATER_SET,
            /** text = packed position (BlockPos#asLong) of the distributor to connect to */
            COMPUTER_LINK,
            COMPUTER_UNLINK
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPacket> CODEC =
                StreamCodec.ofMember(ConfigPacket::write, ConfigPacket::new);

        public ConfigPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Action.class), buf.readVarInt(), buf.readVarInt(), buf.readUtf(MAX_TEXT));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeEnum(action);
            buf.writeVarInt(a);
            buf.writeVarInt(b);
            buf.writeUtf(text, MAX_TEXT);
        }

        public static void handle(ConfigPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> process(sender, message));
            context.setPacketHandled(true);
        }

        private static void process(ServerPlayer sender, ConfigPacket message) {
            if (sender == null) {
                return;
            }
            if (!(sender.containerMenu instanceof RadioDeviceMenu menu) || !menu.pos().equals(message.pos())) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(message.pos());

            if (blockEntity instanceof ComputerBlockEntity computer
                    && (message.action() == Action.COMPUTER_LINK || message.action() == Action.COMPUTER_UNLINK)) {
                boolean changed;
                if (message.action() == Action.COMPUTER_LINK) {
                    changed = link(level, computer, message.text());
                } else {
                    computer.unlink(level);
                    changed = true;
                }
                if (changed) {
                    // Re-open the menu so the client receives the new link state.
                    sender.openMenu(computer, buf -> computer.menuData().write(buf));
                }
                return;
            }

            StationBlockEntity station = StationBlockEntity.find(level, message.pos());
            switch (message.action()) {
                case STATION_APPLY -> {
                    if (station != null) {
                        station.applySettings(level, RadioPhysics.clampFrequency(message.a()),
                                StationBlockEntity.Mode.of(message.b()));
                    }
                }
                case STATION_BROADCAST -> {
                    if (station != null && !station.startTrack(level, message.text())) {
                        sender.displayClientMessage(Component.translatable("message.mcradio.track_missing"), true);
                    }
                }
                case STATION_STOP -> {
                    if (station != null) {
                        station.stopBroadcast(level);
                    }
                }
                case STATION_TEXT -> {
                    if (station != null && !station.broadcastText(level, sender, message.text())) {
                        sender.displayClientMessage(Component.translatable("message.mcradio.text_not_live"), true);
                    }
                }
                case RECEIVER_SET -> {
                    if (blockEntity instanceof ReceiverBlockEntity receiver) {
                        receiver.configure(level, RadioPhysics.clampFrequency(message.a()),
                                RadioPhysics.clampVolume(message.b()));
                    }
                }
                case REPEATER_SET -> {
                    if (blockEntity instanceof RepeaterBlockEntity repeater) {
                        repeater.configure(level, RadioPhysics.clampFrequency(message.a()),
                                RadioPhysics.clampFrequency(message.b()));
                    }
                }
                case COMPUTER_LINK, COMPUTER_UNLINK -> {
                    // only meaningful for computers, handled above
                }
            }
        }

        private static boolean link(ServerLevel level, ComputerBlockEntity computer, String packedPosition) {
            try {
                return computer.link(level, BlockPos.of(Long.parseLong(packedPosition.trim())));
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }


    /** Serverbound: ~100 ms of microphone audio (16 kHz mono mu-law) for a station that is in live mode. */
    public static record LiveAudioPacket(BlockPos pos, byte[] data) {
        public static final int MAX_BYTES = 4096;

        public static final StreamCodec<RegistryFriendlyByteBuf, LiveAudioPacket> CODEC =
                StreamCodec.ofMember(LiveAudioPacket::write, LiveAudioPacket::new);

        public LiveAudioPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeByteArray(data);
        }

        public static void handle(LiveAudioPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || message.data().length == 0) {
                    return;
                }
                if (!(sender.containerMenu instanceof RadioDeviceMenu menu) || !menu.pos().equals(message.pos())) {
                    return;
                }
                ServerLevel level = sender.serverLevel();
                StationBlockEntity station = StationBlockEntity.find(level, message.pos());
                if (station != null) {
                    station.relayLive(level, message.data());
                }
            });
            context.setPacketHandled(true);
        }
    }


    /**
     * Clientbound: a slice of a station's audio stream. {@code station} is the packed position of the
     * originating Signal Broadcast Station and identifies the stream on the client.
     */
    public static record AudioChunkPacket(long station, int codec, byte[] data) {
        public static final int MAX_BYTES = 262144;

        public static final StreamCodec<RegistryFriendlyByteBuf, AudioChunkPacket> CODEC =
                StreamCodec.ofMember(AudioChunkPacket::write, AudioChunkPacket::new);

        public AudioChunkPacket(FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(station);
            buf.writeVarInt(codec);
            buf.writeByteArray(data);
        }

        public static void handle(AudioChunkPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.audioChunk(message.station(), message.codec(), message.data()));
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: the station stopped (or changed mode); drop the stream and its decoder. */
    public static record StreamEndPacket(long station) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StreamEndPacket> CODEC =
                StreamCodec.ofMember(StreamEndPacket::write, StreamEndPacket::new);

        public StreamEndPacket(FriendlyByteBuf buf) {
            this(buf.readLong());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(station);
        }

        public static void handle(StreamEndPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.streamEnd(message.station()));
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: a typed message from a station in live mode, shown in chat to players near tuned receivers. */
    public static record RadioTextPacket(int frequency, String sender, String text) {
        public static final StreamCodec<RegistryFriendlyByteBuf, RadioTextPacket> CODEC =
                StreamCodec.ofMember(RadioTextPacket::write, RadioTextPacket::new);

        public RadioTextPacket(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readUtf(64), buf.readUtf(ConfigPacket.MAX_TEXT));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(frequency);
            buf.writeUtf(sender, 64);
            buf.writeUtf(text, ConfigPacket.MAX_TEXT);
        }

        public static void handle(RadioTextPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.radioText(message.frequency(), message.sender(), message.text()));
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: set the frequency of the walkie-talkie the player holds in the given hand. */
    public static record WalkieConfigPacket(InteractionHand hand, int frequency) {
        public static final StreamCodec<RegistryFriendlyByteBuf, WalkieConfigPacket> CODEC =
                StreamCodec.ofMember(WalkieConfigPacket::write, WalkieConfigPacket::new);

        public WalkieConfigPacket(FriendlyByteBuf buf) {
            this(buf.readEnum(InteractionHand.class), buf.readVarInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeEnum(hand);
            buf.writeVarInt(frequency);
        }

        public static void handle(WalkieConfigPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null) {
                    return;
                }
                ItemStack stack = sender.getItemInHand(message.hand());
                if (stack.getItem() instanceof WalkieTalkieItem) {
                    WalkieTalkieItem.setFrequency(stack, RadioPhysics.clampFrequency(message.frequency()));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: ~100 ms of push-to-talk audio (16 kHz mono mu-law) from a player holding a walkie-talkie. */
    public static record WalkieVoicePacket(byte[] data) {
        public static final int MAX_BYTES = 4096;

        public static final StreamCodec<RegistryFriendlyByteBuf, WalkieVoicePacket> CODEC =
                StreamCodec.ofMember(WalkieVoicePacket::write, WalkieVoicePacket::new);

        public WalkieVoicePacket(FriendlyByteBuf buf) {
            this(buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeByteArray(data);
        }

        public static void handle(WalkieVoicePacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || message.data().length == 0) {
                    return;
                }
                int frequency = WalkieTalkieItem.heldFrequency(sender);
                if (frequency <= 0 || !RadioNetworkManager.allowVoice(sender, message.data().length)) {
                    return;
                }
                RadioNetworkManager.routeVoice(sender, frequency, message.data());
            });
            context.setPacketHandled(true);
        }
    }


    /**
     * Clientbound: walkie-talkie voice from {@code speaker}. {@code mode} is 0 = direct, 1 = relayed through a
     * repeater, 2 = intercepted with a Radio Scanner.
     */
    public static record VoiceChunkPacket(UUID speaker, String name, int frequency, int mode, byte[] data) {
        public static final int MAX_BYTES = 4096;

        public static final StreamCodec<RegistryFriendlyByteBuf, VoiceChunkPacket> CODEC =
                StreamCodec.ofMember(VoiceChunkPacket::write, VoiceChunkPacket::new);

        public VoiceChunkPacket(FriendlyByteBuf buf) {
            this(buf.readUUID(), buf.readUtf(48), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(MAX_BYTES));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(speaker);
            buf.writeUtf(name, 48);
            buf.writeVarInt(frequency);
            buf.writeVarInt(mode);
            buf.writeByteArray(data);
        }

        public static void handle(VoiceChunkPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.voice(message.speaker(), message.name(),
                    message.frequency(), message.mode(), message.data()));
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: the tablet asks for all radio devices of the player's dimension. */
    public static record MapRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, MapRequestPacket> CODEC =
                StreamCodec.unit(new MapRequestPacket());

        public static void handle(MapRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender != null && TabletItem.isHolding(sender) && RadioNetworkManager.allowMapRequest(sender)) {
                    RadioNetwork.sendToPlayer(sender, new MapDataPacket(
                            RadioNetworkManager.mapNodes(sender.serverLevel(), sender.blockPosition())));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: radio devices for the tablet map. */
    public static record MapDataPacket(List<Node> nodes) {
        public static final int KIND_DISTRIBUTOR = 0;
        public static final int KIND_REPEATER = 1;
        public static final int KIND_RECEIVER = 2;
        public static final int KIND_JAMMER = 3;
        public static final int MAX_NODES = 512;

        /**
         * One device. Distributor/receiver: a = frequency. Repeater: a = input, b = output frequency.
         * {@code radius} is the coverage radius in blocks (0 for receivers); {@code active} means on air /
         * relaying / receiving / powered.
         */
        public record Node(int kind, BlockPos pos, int a, int b, int radius, boolean active) {
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, MapDataPacket> CODEC =
                StreamCodec.ofMember(MapDataPacket::write, MapDataPacket::new);

        public MapDataPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<Node> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), MAX_NODES);
            List<Node> nodes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int kind = buf.readVarInt();
                BlockPos pos = buf.readBlockPos();
                int a = buf.readVarInt();
                int b = buf.readVarInt();
                int radius = buf.readVarInt();
                boolean active = buf.readBoolean();
                nodes.add(new Node(kind, pos, a, b, radius, active));
            }
            return List.copyOf(nodes);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(nodes.size());
            for (Node node : nodes) {
                buf.writeVarInt(node.kind());
                buf.writeBlockPos(node.pos());
                buf.writeVarInt(node.a());
                buf.writeVarInt(node.b());
                buf.writeVarInt(node.radius());
                buf.writeBoolean(node.active());
            }
        }

        public static void handle(MapDataPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.mapData(message.nodes()));
            context.setPacketHandled(true);
        }
    }


    /** Serverbound: a computer GUI asks for the distributors it could connect to. */
    public static record StationListRequestPacket() {
        public static final StreamCodec<RegistryFriendlyByteBuf, StationListRequestPacket> CODEC =
                StreamCodec.unit(new StationListRequestPacket());

        public static void handle(StationListRequestPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> {
                if (sender == null || !(sender.containerMenu instanceof RadioDeviceMenu menu)) {
                    return;
                }
                ServerLevel level = sender.serverLevel();
                if (level.getBlockEntity(menu.pos()) instanceof ComputerBlockEntity) {
                    RadioNetwork.sendToPlayer(sender, new StationListPacket(RadioNetworkManager.stationsNear(level, menu.pos())));
                }
            });
            context.setPacketHandled(true);
        }
    }


    /** Clientbound: distributors a computer may link to. */
    public static record StationListPacket(List<Entry> entries) {
        public static final int MAX_ENTRIES = 64;

        public record Entry(BlockPos pos, int frequency, boolean onAir) {
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, StationListPacket> CODEC =
                StreamCodec.ofMember(StationListPacket::write, StationListPacket::new);

        public StationListPacket(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<Entry> read(FriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                BlockPos pos = buf.readBlockPos();
                int frequency = buf.readVarInt();
                boolean onAir = buf.readBoolean();
                entries.add(new Entry(pos, frequency, onAir));
            }
            return List.copyOf(entries);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (Entry entry : entries) {
                buf.writeBlockPos(entry.pos());
                buf.writeVarInt(entry.frequency());
                buf.writeBoolean(entry.onAir());
            }
        }

        public static void handle(StationListPacket message, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> ClientPacketHandlers.stationList(message.entries()));
            context.setPacketHandled(true);
        }
    }


    /**
     * Single registration point for every packet of the mod. Direction is declared explicitly
     * (clientbound / serverbound), and the handlers of clientbound packets only reach client code through
     * {@code ClientPacketHandlers}, so a dedicated server never loads client classes.
     */
    public static final class RadioNetwork {
        public static final int PROTOCOL_VERSION = 1;

        public static final SimpleChannel CHANNEL = ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "main"))
                .networkProtocolVersion(PROTOCOL_VERSION)
                .simpleChannel()
                .play()
                .clientbound()
                    .add(TrackListPacket.class, TrackListPacket.CODEC, TrackListPacket::handle)
                    .add(AudioChunkPacket.class, AudioChunkPacket.CODEC, AudioChunkPacket::handle)
                    .add(StreamEndPacket.class, StreamEndPacket.CODEC, StreamEndPacket::handle)
                    .add(RadioTextPacket.class, RadioTextPacket.CODEC, RadioTextPacket::handle)
                    .add(VoiceChunkPacket.class, VoiceChunkPacket.CODEC, VoiceChunkPacket::handle)
                    .add(MapDataPacket.class, MapDataPacket.CODEC, MapDataPacket::handle)
                    .add(StationListPacket.class, StationListPacket.CODEC, StationListPacket::handle)
                .serverbound()
                    .add(TrackListRequestPacket.class, TrackListRequestPacket.CODEC, TrackListRequestPacket::handle)
                    .add(ConfigPacket.class, ConfigPacket.CODEC, ConfigPacket::handle)
                    .add(LiveAudioPacket.class, LiveAudioPacket.CODEC, LiveAudioPacket::handle)
                    .add(WalkieConfigPacket.class, WalkieConfigPacket.CODEC, WalkieConfigPacket::handle)
                    .add(WalkieVoicePacket.class, WalkieVoicePacket.CODEC, WalkieVoicePacket::handle)
                    .add(MapRequestPacket.class, MapRequestPacket.CODEC, MapRequestPacket::handle)
                    .add(StationListRequestPacket.class, StationListRequestPacket.CODEC, StationListRequestPacket::handle)
                .build();

        private RadioNetwork() {
        }

        /** Forces class initialisation (and therefore channel registration) during mod construction. */
        public static void init() {
            // intentionally empty: touching the class runs the static initialiser above
        }

        public static void sendToPlayer(ServerPlayer player, Object packet) {
            CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
        }

        public static void sendToServer(Object packet) {
            CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
        }
    }


    /**
     * Walkie-talkie (Рация). Right-click to set the frequency; hold the push-to-talk key (default V) while it is
     * in a hand to speak. Talks directly to other walkies within {@link RadioPhysics#WALKIE_RANGE} blocks; for
     * longer distances the speaker has to be in reach of a repeater.
     */
    public static class WalkieTalkieItem extends Item {
        private static final String FREQUENCY_KEY = "Frequency";

        public WalkieTalkieItem(Properties properties) {
            super(properties);
        }

        public static int getFrequency(ItemStack stack) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return tag.contains(FREQUENCY_KEY) ? RadioPhysics.clampFrequency(tag.getInt(FREQUENCY_KEY))
                    : RadioPhysics.DEFAULT_FREQUENCY;
        }

        public static void setFrequency(ItemStack stack, int frequency) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(FREQUENCY_KEY, frequency));
        }

        /** Frequency of the walkie in the player's main hand, else off hand; 0 if none is held. */
        public static int heldFrequency(Player player) {
            ItemStack main = player.getMainHandItem();
            if (main.getItem() instanceof WalkieTalkieItem) {
                return getFrequency(main);
            }
            ItemStack off = player.getOffhandItem();
            if (off.getItem() instanceof WalkieTalkieItem) {
                return getFrequency(off);
            }
            return 0;
        }

        /** Frequency the player listens on: a held walkie first, otherwise any walkie in the inventory; 0 if none. */
        public static int tunedFrequency(Player player) {
            int held = heldFrequency(player);
            if (held > 0) {
                return held;
            }
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof WalkieTalkieItem) {
                    return getFrequency(stack);
                }
            }
            return 0;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                ClientHooks.openWalkie(hand, getFrequency(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
            super.inventoryTick(stack, level, entity, slotId, isSelected);
            if (level.isClientSide) {
                ClientHooks.walkieTick(stack, entity);
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.walkie_talkie.frequency", getFrequency(stack)));
            tooltip.add(Component.translatable("item.mcradio.walkie_talkie.range", RadioPhysics.WALKIE_RANGE));
        }
    }


    /**
     * Radio Scanner (Сканер): while held it overhears every walkie-talkie conversation that is relayed by a
     * repeater within {@link RadioPhysics#SCANNER_RANGE} blocks, whatever frequency it uses.
     */
    public static class ScannerItem extends Item {
        public ScannerItem(Properties properties) {
            super(properties);
        }

        public static boolean isHolding(Player player) {
            return player.getMainHandItem().getItem() instanceof ScannerItem
                    || player.getOffhandItem().getItem() instanceof ScannerItem;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("item.mcradio.radio_scanner.tooltip", RadioPhysics.SCANNER_RANGE));
        }
    }


    /** Radio Tablet (Планшет): opens a map with all repeaters, distributors, receivers, jammers and their coverage. */
    public static class TabletItem extends Item {
        public TabletItem(Properties properties) {
            super(properties);
        }

        public static boolean isHolding(Player player) {
            return player.getMainHandItem().getItem() instanceof TabletItem
                    || player.getOffhandItem().getItem() instanceof TabletItem;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                ClientHooks.openTablet();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }


    /**
     * Slot-less menu shared by the station, repeater and receiver. It exists so the server can verify that
     * the player really has the device open (and is in reach) before accepting configuration packets, and so
     * the initial device state travels to the client in the open-menu packet.
     */
    public static class RadioDeviceMenu extends AbstractContainerMenu {
        private final DeviceData data;

        public RadioDeviceMenu(MenuType<?> type, int containerId, DeviceData data) {
            super(type, containerId);
            this.data = data;
        }

        public DeviceData data() {
            return data;
        }

        public BlockPos pos() {
            return data.pos();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            BlockPos pos = data.pos();
            return player.level().getBlockEntity(pos) instanceof RadioDevice
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }

        /**
         * Snapshot of a device at the moment its GUI opens.
         * Station: a = frequency, b = mode ordinal, c = radius, flag = broadcasting, text = track.
         * Receiver: a = frequency, b = volume percent, flag = has signal.
         * Repeater: a = input frequency, b = output frequency, c = radius, flag = relaying.
         */
        public record DeviceData(BlockPos pos, int a, int b, int c, boolean flag, String text) {
            public void write(FriendlyByteBuf buf) {
                buf.writeBlockPos(pos);
                buf.writeVarInt(a);
                buf.writeVarInt(b);
                buf.writeVarInt(c);
                buf.writeBoolean(flag);
                buf.writeUtf(text, 256);
            }

            public static DeviceData read(FriendlyByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                int a = buf.readVarInt();
                int b = buf.readVarInt();
                int c = buf.readVarInt();
                boolean flag = buf.readBoolean();
                String text = buf.readUtf(256);
                return new DeviceData(pos, a, b, c, flag, text);
            }
        }
    }


    /** Implemented by every block entity that opens a configuration GUI. */
    public static interface RadioDevice extends MenuProvider {
        RadioDeviceMenu.DeviceData menuData();
    }


    /**
     * Server-side radio graph. Block entities register themselves while loaded; once per game tick (driven by
     * whichever device ticks first) the manager
     * <ol>
     *   <li>every second, recomputes which distributors are on air, which repeaters catch a signal and re-emit
     *       it, and which receivers are covered (jammers silence receivers), then tells receivers and nearby
     *       players about it;</li>
     *   <li>every tick, lets every on-air distributor push its audio.</li>
     * </ol>
     * It also routes walkie-talkie voice: a transmission is an emitter of {@link RadioPhysics#WALKIE_RANGE}
     * blocks around the speaker, repeaters whose input frequency matches and that lie inside that range
     * re-emit it on their output frequency, and every tuned walkie inside any emitter's radius hears it.
     * Everything here runs on the server thread only.
     */
    public static final class RadioNetworkManager {
        public static final int RECOMPUTE_INTERVAL = 20;

        public static final int MODE_DIRECT = 0;
        public static final int MODE_RELAYED = 1;
        public static final int MODE_INTERCEPTED = 2;

        private static final int MAX_MAP_NODES = 400;
        private static final int MAP_RANGE = 4096;
        private static final int VOICE_BYTES_PER_SECOND = 32_000;
        private static final int MAP_REQUEST_INTERVAL_TICKS = 10;

        public enum Kind { STATION, REPEATER, RECEIVER, JAMMER }

        private record Emitter(BlockPos pos, int frequency, double radius, long station) {
        }

        private static final class VoiceBudget {
            long window = -100;
            int bytes;
        }

        private static final class LevelNetwork {
            final Set<BlockPos> stations = new LinkedHashSet<>();
            final Set<BlockPos> repeaters = new LinkedHashSet<>();
            final Set<BlockPos> receivers = new LinkedHashSet<>();
            final Set<BlockPos> jammers = new LinkedHashSet<>();
            Map<Long, Set<UUID>> listeners = new HashMap<>();
            long lastTick = Long.MIN_VALUE;
            boolean dirty = true;

            Set<BlockPos> set(Kind kind) {
                return switch (kind) {
                    case STATION -> stations;
                    case REPEATER -> repeaters;
                    case RECEIVER -> receivers;
                    case JAMMER -> jammers;
                };
            }
        }

        private static final Map<ResourceKey<Level>, LevelNetwork> LEVELS = new HashMap<>();
        private static final Map<UUID, VoiceBudget> VOICE_BUDGETS = new HashMap<>();
        private static final Map<UUID, Long> MAP_REQUESTS = new HashMap<>();

        private RadioNetworkManager() {
        }

        private static LevelNetwork network(ServerLevel level) {
            return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelNetwork());
        }

        public static void register(ServerLevel level, BlockPos pos, Kind kind) {
            LevelNetwork network = network(level);
            network.set(kind).add(pos.immutable());
            network.dirty = true;
        }

        public static void unregister(ServerLevel level, BlockPos pos, Kind kind) {
            LevelNetwork network = network(level);
            network.set(kind).remove(pos);
            network.dirty = true;
        }

        /** Requests a graph recompute on the next tick (frequency changed, transmitter placed, ...). */
        public static void markDirty(Level level) {
            if (level instanceof ServerLevel serverLevel) {
                network(serverLevel).dirty = true;
            }
        }

        public static void clear() {
            LEVELS.clear();
            VOICE_BUDGETS.clear();
            MAP_REQUESTS.clear();
        }

        /** Advances the radio network of a level by one tick; safe to call from any number of devices per tick. */
        public static void tick(ServerLevel level) {
            LevelNetwork network = network(level);
            long now = level.getGameTime();
            if (network.lastTick == now) {
                return;
            }
            network.lastTick = now;

            if (network.dirty || now % RECOMPUTE_INTERVAL == 0) {
                network.dirty = false;
                recompute(level, network);
            }
            for (BlockPos pos : new ArrayList<>(network.stations)) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                    station.streamTick(level, now);
                }
            }
        }

        /** Players who currently should receive the stream of the given station (packed station position). */
        public static List<ServerPlayer> listeners(ServerLevel level, long stationKey) {
            Set<UUID> ids = network(level).listeners.get(stationKey);
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            List<ServerPlayer> players = new ArrayList<>(ids.size());
            for (UUID id : ids) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null && player.serverLevel() == level) {
                    players.add(player);
                }
            }
            return players;
        }

        /** Tells every current listener of a station that its stream ended, and forgets them. */
        public static void sendStreamEnd(ServerLevel level, long stationKey) {
            List<ServerPlayer> players = listeners(level, stationKey);
            if (!players.isEmpty()) {
                StreamEndPacket packet = new StreamEndPacket(stationKey);
                for (ServerPlayer player : players) {
                    RadioNetwork.sendToPlayer(player, packet);
                }
            }
        }

        /** True if an active Signal Jammer is within {@link RadioPhysics#JAMMER_RADIUS} of {@code at}. */
        public static boolean isJammed(ServerLevel level, BlockPos at) {
            double radiusSqr = (double) RadioPhysics.JAMMER_RADIUS * RadioPhysics.JAMMER_RADIUS;
            for (BlockPos pos : network(level).jammers) {
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.hasProperty(RadioDeviceBlock.ACTIVE) && state.getValue(RadioDeviceBlock.ACTIVE)
                        && pos.distSqr(at) <= radiusSqr) {
                    return true;
                }
            }
            return false;
        }

        // ------------------------------------------------------------------ walkie-talkie voice

        /** Per-player voice rate limit (about 2x the nominal microphone rate). */
        public static boolean allowVoice(ServerPlayer player, int length) {
            long now = player.serverLevel().getGameTime();
            VoiceBudget budget = VOICE_BUDGETS.computeIfAbsent(player.getUUID(), id -> new VoiceBudget());
            if (now - budget.window >= 20 || now < budget.window) {
                budget.window = now;
                budget.bytes = 0;
            }
            if (budget.bytes + length > VOICE_BYTES_PER_SECOND) {
                return false;
            }
            budget.bytes += length;
            return true;
        }

        /**
         * Delivers one voice chunk of {@code sender} to every player who can hear it. Direct delivery needs a
         * walkie on the same frequency within {@link RadioPhysics#WALKIE_RANGE}; further away, the transmission
         * must be caught by a repeater (inside walkie range of the sender) and the listener must lie inside the
         * radius of a repeater output on the listener's frequency. Players holding a Radio Scanner near a
         * repeater that took part in the relay overhear it regardless of their frequency.
         */
        public static void routeVoice(ServerPlayer sender, int frequency, byte[] data) {
            ServerLevel level = sender.serverLevel();
            BlockPos origin = sender.blockPosition();
            if (isJammed(level, origin)) {
                return;
            }
            LevelNetwork network = network(level);

            List<Emitter> emitters = new ArrayList<>();
            emitters.add(new Emitter(origin, frequency, RadioPhysics.WALKIE_RANGE, 0L));
            List<BlockPos> relays = new ArrayList<>();
            Set<BlockPos> activated = new HashSet<>();
            boolean changed;
            do {
                changed = false;
                for (BlockPos pos : network.repeaters) {
                    if (activated.contains(pos) || !level.isLoaded(pos)
                            || !(level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater)) {
                        continue;
                    }
                    Emitter source = best(emitters, repeater.getInputFrequency(), pos);
                    if (source != null) {
                        emitters.add(new Emitter(pos, repeater.getOutputFrequency(),
                                RadioPhysics.repeaterRadius(level, pos), 1L));
                        activated.add(pos);
                        relays.add(pos);
                        changed = true;
                    }
                }
            } while (changed);

            double scannerRangeSqr = (double) RadioPhysics.SCANNER_RANGE * RadioPhysics.SCANNER_RANGE;
            double directRangeSqr = (double) RadioPhysics.WALKIE_RANGE * RadioPhysics.WALKIE_RANGE;
            String name = sender.getGameProfile().getName();

            for (ServerPlayer listener : level.players()) {
                if (listener == sender) {
                    continue;
                }
                BlockPos at = listener.blockPosition();
                if (isJammed(level, at)) {
                    continue;
                }
                int mode = -1;
                int tuned = WalkieTalkieItem.tunedFrequency(listener);
                if (tuned > 0) {
                    if (tuned == frequency && origin.distSqr(at) <= directRangeSqr) {
                        mode = MODE_DIRECT;
                    } else if (best(emitters, tuned, at) != null) {
                        mode = MODE_RELAYED;
                    }
                }
                if (mode < 0 && !relays.isEmpty() && ScannerItem.isHolding(listener)) {
                    for (BlockPos relay : relays) {
                        if (relay.distSqr(at) <= scannerRangeSqr) {
                            mode = MODE_INTERCEPTED;
                            break;
                        }
                    }
                }
                if (mode >= 0) {
                    RadioNetwork.sendToPlayer(listener,
                            new VoiceChunkPacket(sender.getUUID(), name, frequency, mode, data));
                }
            }
        }

        // ------------------------------------------------------------------ queries for GUIs

        public static boolean allowMapRequest(ServerPlayer player) {
            long now = player.serverLevel().getGameTime();
            Long last = MAP_REQUESTS.get(player.getUUID());
            if (last != null && now >= last && now - last < MAP_REQUEST_INTERVAL_TICKS) {
                return false;
            }
            MAP_REQUESTS.put(player.getUUID(), now);
            return true;
        }

        /** All loaded radio devices of the level for the tablet map. */
        public static List<MapDataPacket.Node> mapNodes(ServerLevel level, BlockPos around) {
            LevelNetwork network = network(level);
            double rangeSqr = (double) MAP_RANGE * MAP_RANGE;
            List<MapDataPacket.Node> nodes = new ArrayList<>();

            for (BlockPos pos : network.stations) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_DISTRIBUTOR, pos, station.getFrequency(), 0,
                            RadioPhysics.stationRadius(level, pos), station.onAir()));
                }
            }
            for (BlockPos pos : network.repeaters) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_REPEATER, pos, repeater.getInputFrequency(),
                            repeater.getOutputFrequency(), RadioPhysics.repeaterRadius(level, pos), repeater.isActive()));
                }
            }
            for (BlockPos pos : network.receivers) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver) {
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_RECEIVER, pos, receiver.getFrequency(), 0, 0,
                            receiver.isReceiving()));
                }
            }
            for (BlockPos pos : network.jammers) {
                if (nodes.size() < MAX_MAP_NODES && pos.distSqr(around) <= rangeSqr && level.isLoaded(pos)) {
                    BlockState state = level.getBlockState(pos);
                    boolean active = state.hasProperty(RadioDeviceBlock.ACTIVE) && state.getValue(RadioDeviceBlock.ACTIVE);
                    nodes.add(new MapDataPacket.Node(MapDataPacket.KIND_JAMMER, pos, 0, 0, RadioPhysics.JAMMER_RADIUS, active));
                }
            }
            return nodes;
        }

        /** Distributors a computer at {@code computer} may link to, nearest first. */
        public static List<StationListPacket.Entry> stationsNear(ServerLevel level, BlockPos computer) {
            double rangeSqr = (double) RadioPhysics.COMPUTER_LINK_RANGE * RadioPhysics.COMPUTER_LINK_RANGE;
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos pos : network(level).stations) {
                if (pos.distSqr(computer) <= rangeSqr && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof StationBlockEntity) {
                    found.add(pos);
                }
            }
            found.sort(Comparator.comparingDouble(pos -> pos.distSqr(computer)));
            List<StationListPacket.Entry> entries = new ArrayList<>();
            for (BlockPos pos : found) {
                if (entries.size() >= StationListPacket.MAX_ENTRIES) {
                    break;
                }
                StationBlockEntity station = (StationBlockEntity) level.getBlockEntity(pos);
                entries.add(new StationListPacket.Entry(pos.immutable(), station.getFrequency(), station.onAir()));
            }
            return entries;
        }

        // ------------------------------------------------------------------ graph recompute

        private static void recompute(ServerLevel level, LevelNetwork network) {
            List<Emitter> emitters = new ArrayList<>();

            for (BlockPos pos : network.stations) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                    station.refreshActiveState(level);
                    if (station.onAir()) {
                        emitters.add(new Emitter(pos, station.getFrequency(), RadioPhysics.stationRadius(level, pos),
                                pos.asLong()));
                    }
                }
            }

            // Repeaters: a repeater catches a signal when it lies inside the radius of an emitter on its input
            // frequency, then becomes an emitter itself on its output frequency. Each repeater activates at most
            // once, so loops terminate; chains of repeaters resolve over several passes.
            Set<BlockPos> activated = new HashSet<>();
            boolean changed;
            do {
                changed = false;
                for (BlockPos pos : network.repeaters) {
                    if (activated.contains(pos) || !level.isLoaded(pos)
                            || !(level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater)) {
                        continue;
                    }
                    Emitter source = best(emitters, repeater.getInputFrequency(), pos);
                    if (source != null) {
                        emitters.add(new Emitter(pos, repeater.getOutputFrequency(), RadioPhysics.repeaterRadius(level, pos),
                                source.station()));
                        activated.add(pos);
                        changed = true;
                    }
                }
            } while (changed);

            for (BlockPos pos : network.repeaters) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RepeaterBlockEntity repeater) {
                    repeater.setActive(level, activated.contains(pos));
                }
            }

            Map<Long, Set<UUID>> listeners = new HashMap<>();
            List<ServerPlayer> players = level.players();
            double listenRangeSqr = (double) RadioPhysics.LISTEN_RANGE * RadioPhysics.LISTEN_RANGE;

            for (BlockPos pos : network.receivers) {
                if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver)) {
                    continue;
                }
                Emitter source = isJammed(level, pos) ? null : best(emitters, receiver.getFrequency(), pos);
                receiver.applySignal(level, source != null, source != null ? source.station() : 0L);
                if (source != null) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    for (ServerPlayer player : players) {
                        if (player.distanceToSqr(center) <= listenRangeSqr) {
                            listeners.computeIfAbsent(source.station(), key -> new HashSet<>()).add(player.getUUID());
                        }
                    }
                }
            }
            network.listeners = listeners;
        }

        /** The nearest emitter on {@code frequency} whose radius covers {@code at}, or null. */
        private static Emitter best(List<Emitter> emitters, int frequency, BlockPos at) {
            Emitter best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Emitter emitter : emitters) {
                if (emitter.frequency() != frequency) {
                    continue;
                }
                double distance = Math.sqrt(emitter.pos().distSqr(at));
                if (distance <= emitter.radius() && distance < bestDistance) {
                    best = emitter;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }


    /**
     * Signal Broadcast Station. In PLAYLIST mode it streams the raw bytes of one MP3 from
     * {@code config/mc_radio/tracks/} (looping) in real time; in LIVE mode it is an always-on carrier that
     * relays microphone audio sent by the player who has the GUI open.
     */
    public static final class StationBlockEntity extends BlockEntity implements RadioDevice {
        public enum Mode {
            PLAYLIST, LIVE;

            public static Mode of(int ordinal) {
                return ordinal == 1 ? LIVE : PLAYLIST;
            }
        }

        private static final double PREBUFFER_SECONDS = 1.0;
        private static final int STREAM_INTERVAL_TICKS = 4;
        private static final int MAX_CHUNK_BYTES = 200_000;
        private static final int MIN_CHUNK_BYTES = 256;
        private static final int LIVE_BYTES_PER_SECOND = 32_000;
        private static final int LIVE_KEEPALIVE_BYTES = 800;

        private int frequency = RadioPhysics.DEFAULT_FREQUENCY;
        private Mode mode = Mode.PLAYLIST;
        private String track = "";
        private boolean broadcasting;

        // Server-only transient streaming state.
        private CompletableFuture<Mp3Source> loading;
        private Mp3Source source;
        private int cursor;
        private double budget;
        private int liveBytesThisSecond;

        public StationBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.STATION_BE.get(), pos, state);
        }

        /**
         * The distributor controlled from the device at {@code pos}: the distributor itself, or the one a
         * computer at that position is linked to. Null if there is none (yet).
         */
        public static StationBlockEntity find(ServerLevel level, BlockPos pos) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationBlockEntity station) {
                return station;
            }
            if (blockEntity instanceof ComputerBlockEntity computer) {
                return computer.resolve(level);
            }
            return null;
        }

        public int getFrequency() {
            return frequency;
        }

        public Mode getMode() {
            return mode;
        }

        public boolean onAir() {
            return mode == Mode.LIVE || broadcasting;
        }

        private long key() {
            return worldPosition.asLong();
        }

        // ---------------------------------------------------------------- lifecycle

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.STATION);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.sendStreamEnd(serverLevel, key());
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.STATION);
            }
            cancelLoading();
            super.setRemoved();
        }

        // ---------------------------------------------------------------- settings

        public void applySettings(ServerLevel serverLevel, int newFrequency, Mode newMode) {
            boolean modeChanged = newMode != mode;
            boolean frequencyChanged = newFrequency != frequency;
            if (modeChanged) {
                RadioNetworkManager.sendStreamEnd(serverLevel, key());
                resetPlayback();
                broadcasting = false;
                mode = newMode;
            }
            frequency = newFrequency;
            if (modeChanged || frequencyChanged) {
                setChanged();
                RadioNetworkManager.markDirty(serverLevel);
            }
        }

        /** Starts looping the given track. Returns false when the file does not exist or the station is not in playlist mode. */
        public boolean startTrack(ServerLevel serverLevel, String name) {
            if (mode != Mode.PLAYLIST) {
                return false;
            }
            Optional<Path> file = TrackLibrary.resolve(name);
            if (file.isEmpty()) {
                return false;
            }
            RadioNetworkManager.sendStreamEnd(serverLevel, key());
            resetPlayback();
            track = name;
            broadcasting = true;
            loading = Mp3Source.loadAsync(file.get());
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
            return true;
        }

        public void stopBroadcast(ServerLevel serverLevel) {
            if (!broadcasting) {
                return;
            }
            RadioNetworkManager.sendStreamEnd(serverLevel, key());
            resetPlayback();
            broadcasting = false;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        private void resetPlayback() {
            cancelLoading();
            source = null;
            cursor = 0;
            budget = 0.0;
        }

        private void cancelLoading() {
            if (loading != null) {
                loading.cancel(false);
                loading = null;
            }
        }

        // ---------------------------------------------------------------- streaming (server tick)

        public void streamTick(ServerLevel serverLevel, long gameTime) {
            if (gameTime % 20 == 0) {
                liveBytesThisSecond = 0;
            }
            if (mode == Mode.LIVE) {
                liveKeepAlive(serverLevel, gameTime);
                return;
            }
            if (!broadcasting) {
                return;
            }

            if (source == null) {
                if (loading == null) {
                    Optional<Path> file = TrackLibrary.resolve(track);
                    if (file.isEmpty()) {
                        McRadio.LOGGER.warn("Track '{}' of station at {} is no longer available", track, worldPosition);
                        failPlayback(serverLevel);
                        return;
                    }
                    loading = Mp3Source.loadAsync(file.get());
                    return;
                }
                if (!loading.isDone()) {
                    return;
                }
                try {
                    source = loading.join();
                } catch (RuntimeException e) {
                    McRadio.LOGGER.warn("Could not load track '{}' for station at {}", track, worldPosition, e);
                    failPlayback(serverLevel);
                    return;
                }
                loading = null;
                cursor = source.start();
                budget = source.bytesPerSecond() * PREBUFFER_SECONDS;
            }

            budget += source.bytesPerSecond() / 20.0;
            if (Math.floorMod(gameTime + worldPosition.hashCode(), STREAM_INTERVAL_TICKS) != 0) {
                return;
            }
            int wanted = (int) Math.min(budget, MAX_CHUNK_BYTES);
            if (wanted < MIN_CHUNK_BYTES) {
                return;
            }
            budget -= wanted;

            byte[] chunk = new byte[wanted];
            byte[] data = source.data();
            int filled = 0;
            while (filled < wanted) {
                int take = Math.min(wanted - filled, source.end() - cursor);
                System.arraycopy(data, cursor, chunk, filled, take);
                filled += take;
                cursor += take;
                if (cursor >= source.end()) {
                    cursor = source.start();
                }
            }
            broadcast(serverLevel, new AudioChunkPacket(key(), AudioCodec.MP3, chunk));
        }

        private void failPlayback(ServerLevel serverLevel) {
            resetPlayback();
            broadcasting = false;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        /** In live mode a short burst of silence once per second keeps client streams (and sound instances) alive. */
        private void liveKeepAlive(ServerLevel serverLevel, long gameTime) {
            if (Math.floorMod(gameTime + worldPosition.hashCode(), 20) != 0) {
                return;
            }
            byte[] silence = new byte[LIVE_KEEPALIVE_BYTES];
            Arrays.fill(silence, UlawCodec.silence());
            broadcast(serverLevel, new AudioChunkPacket(key(), AudioCodec.ULAW_16K, silence));
        }

        private void broadcast(ServerLevel serverLevel, AudioChunkPacket packet) {
            for (ServerPlayer player : RadioNetworkManager.listeners(serverLevel, key())) {
                RadioNetwork.sendToPlayer(player, packet);
            }
        }

        /** Relays microphone audio received from the player operating the GUI. */
        public void relayLive(ServerLevel serverLevel, byte[] data) {
            if (mode != Mode.LIVE || liveBytesThisSecond + data.length > LIVE_BYTES_PER_SECOND) {
                return;
            }
            liveBytesThisSecond += data.length;
            broadcast(serverLevel, new AudioChunkPacket(key(), AudioCodec.ULAW_16K, data));
        }

        /** Sends a typed message to everyone who can currently hear this station. Returns false if nothing was sent. */
        public boolean broadcastText(ServerLevel serverLevel, ServerPlayer sender, String raw) {
            if (mode != Mode.LIVE) {
                return false;
            }
            StringBuilder cleaned = new StringBuilder();
            for (char c : raw.strip().toCharArray()) {
                if (c >= 32 && c != 127) {
                    cleaned.append(c);
                }
            }
            String text = cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned.toString();
            if (text.isEmpty()) {
                return false;
            }
            RadioTextPacket packet = new RadioTextPacket(frequency, sender.getGameProfile().getName(), text);
            for (ServerPlayer player : RadioNetworkManager.listeners(serverLevel, key())) {
                RadioNetwork.sendToPlayer(player, packet);
            }
            return true;
        }

        public void refreshActiveState(ServerLevel serverLevel) {
            RadioDeviceBlock.setActive(serverLevel, worldPosition, onAir());
        }

        // ---------------------------------------------------------------- persistence

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Frequency", frequency);
            tag.putString("Mode", mode.name());
            tag.putString("Track", track);
            tag.putBoolean("Broadcasting", broadcasting);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            frequency = tag.contains("Frequency") ? RadioPhysics.clampFrequency(tag.getInt("Frequency"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
            try {
                mode = Mode.valueOf(tag.getString("Mode"));
            } catch (IllegalArgumentException e) {
                mode = Mode.PLAYLIST;
            }
            track = tag.getString("Track");
            broadcasting = tag.getBoolean("Broadcasting") && mode == Mode.PLAYLIST && !track.isEmpty();
        }

        // ---------------------------------------------------------------- menu

        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            int radius = level != null ? RadioPhysics.stationRadius(level, worldPosition) : RadioPhysics.STATION_BASE_RADIUS;
            return new RadioDeviceMenu.DeviceData(worldPosition, frequency, mode.ordinal(), radius, broadcasting, track);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.station");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.STATION_MENU.get(), containerId, menuData());
        }
    }


    /** Signal Repeater: catches a signal on the input frequency and re-emits it on the output frequency. */
    public static final class RepeaterBlockEntity extends BlockEntity implements RadioDevice {
        private int inputFrequency = RadioPhysics.DEFAULT_FREQUENCY;
        private int outputFrequency = RadioPhysics.DEFAULT_FREQUENCY;
        private boolean active;

        public RepeaterBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.REPEATER_BE.get(), pos, state);
        }

        public int getInputFrequency() {
            return inputFrequency;
        }

        public int getOutputFrequency() {
            return outputFrequency;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.REPEATER);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.REPEATER);
            }
            super.setRemoved();
        }

        public void configure(ServerLevel serverLevel, int input, int output) {
            if (input == inputFrequency && output == outputFrequency) {
                return;
            }
            inputFrequency = input;
            outputFrequency = output;
            setChanged();
            RadioNetworkManager.markDirty(serverLevel);
        }

        /** Called by the radio graph after each recompute. */
        public void setActive(ServerLevel serverLevel, boolean value) {
            active = value;
            RadioDeviceBlock.setActive(serverLevel, worldPosition, value);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Input", inputFrequency);
            tag.putInt("Output", outputFrequency);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            inputFrequency = tag.contains("Input") ? RadioPhysics.clampFrequency(tag.getInt("Input"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
            outputFrequency = tag.contains("Output") ? RadioPhysics.clampFrequency(tag.getInt("Output"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
        }

        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            int radius = level != null ? RadioPhysics.repeaterRadius(level, worldPosition) : RadioPhysics.REPEATER_BASE_RADIUS;
            return new RadioDeviceMenu.DeviceData(worldPosition, inputFrequency, outputFrequency, radius, active, "");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.repeater");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.REPEATER_MENU.get(), containerId, menuData());
        }
    }


    /**
     * Radio Receiver &amp; Player. The server decides whether the receiver has a signal (frequency match inside
     * a station's or repeater's radius) and syncs {@code receiving} + the originating station to clients, which
     * then play the locally decoded stream through Minecraft's sound engine.
     */
    public static final class ReceiverBlockEntity extends BlockEntity implements RadioDevice {
        private int frequency = RadioPhysics.DEFAULT_FREQUENCY;
        private int volume = 100;
        private boolean receiving;
        private long sourceStation;

        /** Client-only handle to the running sound instance; typed as Object to keep client classes out of this class. */
        public Object clientSound;

        public ReceiverBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.RECEIVER_BE.get(), pos, state);
        }

        public int getFrequency() {
            return frequency;
        }

        public int getVolume() {
            return volume;
        }

        public boolean isReceiving() {
            return receiving;
        }

        public long getSourceStation() {
            return sourceStation;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.RECEIVER);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.RECEIVER);
            }
            super.setRemoved();
        }

        public void configure(ServerLevel serverLevel, int newFrequency, int newVolume) {
            if (newFrequency == frequency && newVolume == volume) {
                return;
            }
            boolean frequencyChanged = newFrequency != frequency;
            frequency = newFrequency;
            volume = newVolume;
            setChanged();
            syncToClients();
            if (frequencyChanged) {
                RadioNetworkManager.markDirty(serverLevel);
            }
        }

        /** Called by the radio graph after each recompute. */
        public void applySignal(ServerLevel serverLevel, boolean hasSignal, long station) {
            long newSource = hasSignal ? station : 0L;
            if (hasSignal == receiving && newSource == sourceStation) {
                return;
            }
            receiving = hasSignal;
            sourceStation = newSource;
            setChanged();
            RadioDeviceBlock.setActive(serverLevel, worldPosition, hasSignal);
            syncToClients();
        }

        private void syncToClients() {
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Frequency", frequency);
            tag.putInt("Volume", volume);
            tag.putBoolean("Receiving", receiving);
            tag.putLong("Source", sourceStation);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            frequency = tag.contains("Frequency") ? RadioPhysics.clampFrequency(tag.getInt("Frequency"))
                    : RadioPhysics.DEFAULT_FREQUENCY;
            volume = tag.contains("Volume") ? RadioPhysics.clampVolume(tag.getInt("Volume")) : 100;
            receiving = tag.getBoolean("Receiving");
            sourceStation = tag.getLong("Source");
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveWithoutMetadata(registries);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            return new RadioDeviceMenu.DeviceData(worldPosition, frequency, volume, 0, receiving, "");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.receiver");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.RECEIVER_MENU.get(), containerId, menuData());
        }
    }


    /**
     * Computer: a remote terminal for a Signal Distributor. Once linked it shows exactly the distributor's
     * interface (frequency, playlist / live mode, broadcast), and all actions are applied to the distributor.
     */
    public static final class ComputerBlockEntity extends BlockEntity implements RadioDevice {
        private BlockPos linked;

        public ComputerBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.COMPUTER_BE.get(), pos, state);
        }

        private boolean inRange(BlockPos target) {
            double max = (double) RadioPhysics.COMPUTER_LINK_RANGE * RadioPhysics.COMPUTER_LINK_RANGE;
            return target.distSqr(worldPosition) <= max;
        }

        /** Connects to the distributor at {@code target}. Returns false if there is none in range. */
        public boolean link(ServerLevel level, BlockPos target) {
            if (!inRange(target) || !level.isLoaded(target) || !(level.getBlockEntity(target) instanceof StationBlockEntity)) {
                return false;
            }
            linked = target.immutable();
            setChanged();
            RadioDeviceBlock.setActive(level, worldPosition, true);
            return true;
        }

        public void unlink(ServerLevel level) {
            linked = null;
            setChanged();
            RadioDeviceBlock.setActive(level, worldPosition, false);
        }

        /** The linked distributor, or null if not linked, unloaded, removed or out of range. */
        public StationBlockEntity resolve(ServerLevel level) {
            if (linked == null || !inRange(linked) || !level.isLoaded(linked)) {
                return null;
            }
            return level.getBlockEntity(linked) instanceof StationBlockEntity station ? station : null;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putBoolean("HasLink", linked != null);
            if (linked != null) {
                tag.putLong("Link", linked.asLong());
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            linked = tag.getBoolean("HasLink") ? BlockPos.of(tag.getLong("Link")) : null;
        }

        /**
         * Linked: the distributor's snapshot (so the client shows the same interface). Not linked: radius -1,
         * which tells the client to show the connection page.
         */
        @Override
        public RadioDeviceMenu.DeviceData menuData() {
            if (level instanceof ServerLevel serverLevel) {
                StationBlockEntity station = resolve(serverLevel);
                if (station != null) {
                    RadioDeviceMenu.DeviceData data = station.menuData();
                    return new RadioDeviceMenu.DeviceData(worldPosition, data.a(), data.b(), data.c(), data.flag(), data.text());
                }
            }
            return new RadioDeviceMenu.DeviceData(worldPosition, 0, 0, -1, false, "");
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.mcradio.computer");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new RadioDeviceMenu(ModRegistry.COMPUTER_MENU.get(), containerId, menuData());
        }
    }


    /** Keeps the Signal Jammer registered with the radio graph while its chunk is loaded. */
    public static final class JammerBlockEntity extends BlockEntity {
        public JammerBlockEntity(BlockPos pos, BlockState state) {
            super(ModRegistry.JAMMER_BE.get(), pos, state);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.register(serverLevel, worldPosition, RadioNetworkManager.Kind.JAMMER);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel serverLevel) {
                RadioNetworkManager.unregister(serverLevel, worldPosition, RadioNetworkManager.Kind.JAMMER);
            }
            super.setRemoved();
        }
    }


    /** Shared behaviour of the station, repeater and receiver: facing, "active" light/texture state, GUI opening. */
    public static abstract class RadioDeviceBlock extends BaseEntityBlock {
        public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
        public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

        protected RadioDeviceBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(ACTIVE, false);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RadioDevice device && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(device, buf -> device.menuData().write(buf));
            }
            return InteractionResult.CONSUME;
        }

        /** Updates the ACTIVE blockstate property only when it actually changes. */
        public static void setActive(Level level, BlockPos pos, boolean active) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(ACTIVE) && state.getValue(ACTIVE) != active) {
                level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
            }
        }
    }


    /** Signal Broadcast Station (Станция раздачи сигнала). */
    public static class StationBlock extends RadioDeviceBlock {
        public static final MapCodec<StationBlock> CODEC = simpleCodec(StationBlock::new);
        private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);

        public StationBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends StationBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StationBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, ModRegistry.STATION_BE.get(),
                    (tickLevel, pos, tickState, entity) -> RadioNetworkManager.tick((ServerLevel) tickLevel));
        }
    }


    /** Signal Repeater (Ретранслятор сигнала). */
    public static class RepeaterBlock extends RadioDeviceBlock {
        public static final MapCodec<RepeaterBlock> CODEC = simpleCodec(RepeaterBlock::new);
        private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 15.0, 14.0);

        public RepeaterBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends RepeaterBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new RepeaterBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                return null;
            }
            return createTickerHelper(type, ModRegistry.REPEATER_BE.get(),
                    (tickLevel, pos, tickState, entity) -> RadioNetworkManager.tick((ServerLevel) tickLevel));
        }
    }


    /** Radio Receiver &amp; Player (Приемник и воспроизводитель сигнала). */
    public static class ReceiverBlock extends RadioDeviceBlock {
        public static final MapCodec<ReceiverBlock> CODEC = simpleCodec(ReceiverBlock::new);
        private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 3.0, 14.0, 9.0, 13.0);

        public ReceiverBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends ReceiverBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ReceiverBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                      BlockEntityType<T> type) {
            if (level.isClientSide) {
                // Client-only class is referenced exclusively inside this lambda, which only runs on the client.
                return createTickerHelper(type, ModRegistry.RECEIVER_BE.get(),
                        (tickLevel, pos, tickState, entity) -> ClientReceiverLogic.tick(entity));
            }
            return createTickerHelper(type, ModRegistry.RECEIVER_BE.get(),
                    (tickLevel, pos, tickState, entity) -> RadioNetworkManager.tick((ServerLevel) tickLevel));
        }
    }


    /**
     * Signal Transmitter (Транслятор сигнала): an antenna tower. Placed face-adjacent to a Broadcast Station
     * (or a Repeater), or chained to other transmitters that touch one, it raises that device's broadcast
     * radius; see {@link RadioPhysics#transmitterCount}. It has no GUI and no block entity.
     */
    public static class TransmitterBlock extends Block {
        public static final MapCodec<TransmitterBlock> CODEC = simpleCodec(TransmitterBlock::new);
        private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

        public TransmitterBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends TransmitterBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            RadioNetworkManager.markDirty(level);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            RadioNetworkManager.markDirty(level);
        }
    }


    /** Computer (Компьютер): remote terminal for a Signal Distributor. */
    public static class ComputerBlock extends RadioDeviceBlock {
        public static final MapCodec<ComputerBlock> CODEC = simpleCodec(ComputerBlock::new);
        private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

        public ComputerBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends ComputerBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ComputerBlockEntity(pos, state);
        }
    }


    /**
     * Signal Jammer (Глушилка): while powered by redstone it blocks walkie-talkie traffic (both sending and
     * receiving) and radio receivers within {@link RadioPhysics#JAMMER_RADIUS} blocks.
     */
    public static class JammerBlock extends BaseEntityBlock {
        public static final MapCodec<JammerBlock> CODEC = simpleCodec(JammerBlock::new);
        private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

        public JammerBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(RadioDeviceBlock.ACTIVE, false));
        }

        @Override
        protected MapCodec<? extends JammerBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(RadioDeviceBlock.ACTIVE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(RadioDeviceBlock.ACTIVE,
                    context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                       BlockPos neighborPos, boolean movedByPiston) {
            if (level.isClientSide) {
                return;
            }
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != state.getValue(RadioDeviceBlock.ACTIVE)) {
                level.setBlock(pos, state.setValue(RadioDeviceBlock.ACTIVE, powered), Block.UPDATE_ALL);
                RadioNetworkManager.markDirty(level);
            }
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new JammerBlockEntity(pos, state);
        }
    }


    /** Deferred registers for blocks, items, block entities, menus and the creative tab. */
    public static final class ModRegistry {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, McRadio.MOD_ID);
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, McRadio.MOD_ID);
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
                DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, McRadio.MOD_ID);
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, McRadio.MOD_ID);
        public static final DeferredRegister<CreativeModeTab> TABS =
                DeferredRegister.create(Registries.CREATIVE_MODE_TAB, McRadio.MOD_ID);

        // ------------------------------------------------------------------ blocks

        public static final RegistryObject<Block> STATION_BLOCK = BLOCKS.register("signal_station",
                () -> new StationBlock(deviceProperties()));
        public static final RegistryObject<Block> TRANSMITTER_BLOCK = BLOCKS.register("signal_transmitter",
                () -> new TransmitterBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));
        public static final RegistryObject<Block> REPEATER_BLOCK = BLOCKS.register("signal_repeater",
                () -> new RepeaterBlock(deviceProperties()));
        public static final RegistryObject<Block> RECEIVER_BLOCK = BLOCKS.register("signal_receiver",
                () -> new ReceiverBlock(deviceProperties()));

        public static final RegistryObject<Block> COMPUTER_BLOCK = BLOCKS.register("signal_computer",
                () -> new ComputerBlock(deviceProperties()));
        public static final RegistryObject<Block> JAMMER_BLOCK = BLOCKS.register("signal_jammer",
                () -> new JammerBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(3.5F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()
                        .lightLevel(state -> state.getValue(RadioDeviceBlock.ACTIVE) ? 4 : 0)));

        // ------------------------------------------------------------------ items

        public static final RegistryObject<Item> STATION_ITEM = ITEMS.register("signal_station",
                () -> new BlockItem(STATION_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> TRANSMITTER_ITEM = ITEMS.register("signal_transmitter",
                () -> new BlockItem(TRANSMITTER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> REPEATER_ITEM = ITEMS.register("signal_repeater",
                () -> new BlockItem(REPEATER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> RECEIVER_ITEM = ITEMS.register("signal_receiver",
                () -> new BlockItem(RECEIVER_BLOCK.get(), new Item.Properties()));

        public static final RegistryObject<Item> COMPUTER_ITEM = ITEMS.register("signal_computer",
                () -> new BlockItem(COMPUTER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> JAMMER_ITEM = ITEMS.register("signal_jammer",
                () -> new BlockItem(JAMMER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> WALKIE_ITEM = ITEMS.register("walkie_talkie",
                () -> new WalkieTalkieItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> SCANNER_ITEM = ITEMS.register("radio_scanner",
                () -> new ScannerItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> TABLET_ITEM = ITEMS.register("radio_tablet",
                () -> new TabletItem(new Item.Properties().stacksTo(1)));

        // ------------------------------------------------------------------ block entities

        public static final RegistryObject<BlockEntityType<StationBlockEntity>> STATION_BE = BLOCK_ENTITIES.register(
                "signal_station",
                () -> BlockEntityType.Builder.of(StationBlockEntity::new, STATION_BLOCK.get()).build(null));
        public static final RegistryObject<BlockEntityType<RepeaterBlockEntity>> REPEATER_BE = BLOCK_ENTITIES.register(
                "signal_repeater",
                () -> BlockEntityType.Builder.of(RepeaterBlockEntity::new, REPEATER_BLOCK.get()).build(null));
        public static final RegistryObject<BlockEntityType<ReceiverBlockEntity>> RECEIVER_BE = BLOCK_ENTITIES.register(
                "signal_receiver",
                () -> BlockEntityType.Builder.of(ReceiverBlockEntity::new, RECEIVER_BLOCK.get()).build(null));

        public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BE = BLOCK_ENTITIES.register(
                "signal_computer",
                () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER_BLOCK.get()).build(null));
        public static final RegistryObject<BlockEntityType<JammerBlockEntity>> JAMMER_BE = BLOCK_ENTITIES.register(
                "signal_jammer",
                () -> BlockEntityType.Builder.of(JammerBlockEntity::new, JAMMER_BLOCK.get()).build(null));

        // ------------------------------------------------------------------ menus

        public static final RegistryObject<MenuType<RadioDeviceMenu>> STATION_MENU = MENUS.register("signal_station",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.STATION_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));
        public static final RegistryObject<MenuType<RadioDeviceMenu>> REPEATER_MENU = MENUS.register("signal_repeater",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.REPEATER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));
        public static final RegistryObject<MenuType<RadioDeviceMenu>> RECEIVER_MENU = MENUS.register("signal_receiver",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.RECEIVER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));

        public static final RegistryObject<MenuType<RadioDeviceMenu>> COMPUTER_MENU = MENUS.register("signal_computer",
                () -> IForgeMenuType.create((id, inventory, buf) ->
                        new RadioDeviceMenu(ModRegistry.COMPUTER_MENU.get(), id, RadioDeviceMenu.DeviceData.read(buf))));

        // ------------------------------------------------------------------ creative tab

        public static final RegistryObject<CreativeModeTab> RADIO_TAB = TABS.register("radio", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mcradio"))
                .icon(() -> new ItemStack(STATION_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(STATION_ITEM.get());
                    output.accept(COMPUTER_ITEM.get());
                    output.accept(TRANSMITTER_ITEM.get());
                    output.accept(REPEATER_ITEM.get());
                    output.accept(RECEIVER_ITEM.get());
                    output.accept(JAMMER_ITEM.get());
                    output.accept(WALKIE_ITEM.get());
                    output.accept(SCANNER_ITEM.get());
                    output.accept(TABLET_ITEM.get());
                })
                .build());

        private ModRegistry() {
        }

        private static BlockBehaviour.Properties deviceProperties() {
            return BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(RadioDeviceBlock.ACTIVE) ? 6 : 0);
        }

        public static void register(IEventBus modBus) {
            BLOCKS.register(modBus);
            ITEMS.register(modBus);
            BLOCK_ENTITIES.register(modBus);
            MENUS.register(modBus);
            TABS.register(modBus);
        }
    }


    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ServerEvents {
        private ServerEvents() {
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            RadioNetworkManager.clear();
        }
    }


    /** Mod-bus client initialisation. Only ever loaded on the physical client. */
    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientSetup {
        private ClientSetup() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModRegistry.STATION_MENU.get(), StationScreen::new);
                MenuScreens.register(ModRegistry.REPEATER_MENU.get(), RepeaterScreen::new);
                MenuScreens.register(ModRegistry.RECEIVER_MENU.get(), ReceiverScreen::new);
                // A computer shows the interface of the distributor it is linked to.
                MenuScreens.register(ModRegistry.COMPUTER_MENU.get(), StationScreen::new);
            });
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(WalkieClientLogic.PUSH_TO_TALK);
        }
    }


    /** Forge-bus client events. */
    @Mod.EventBusSubscriber(modid = McRadio.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            MicCapture.stop();
            ClientAudioManager.clear();
        }
    }


    /**
     * The only entry point from network packet handlers into client code. Keeping these as static methods of a
     * dedicated client-only class means a dedicated server never needs to resolve any client class.
     */
    public static final class ClientPacketHandlers {
        private ClientPacketHandlers() {
        }

        public static void trackList(List<String> tracks) {
            if (Minecraft.getInstance().screen instanceof StationScreen screen) {
                screen.setTracks(tracks);
            }
        }

        public static void audioChunk(long station, int codec, byte[] data) {
            ClientAudioManager.accept(station, codec, data);
        }

        public static void streamEnd(long station) {
            ClientAudioManager.end(station);
        }

        public static void voice(UUID speaker, String name, int frequency, int mode, byte[] data) {
            ClientAudioManager.voice(speaker, name, frequency, mode, data);
        }

        public static void mapData(List<MapDataPacket.Node> nodes) {
            if (Minecraft.getInstance().screen instanceof TabletScreen screen) {
                screen.setNodes(nodes);
            }
        }

        public static void stationList(List<StationListPacket.Entry> entries) {
            if (Minecraft.getInstance().screen instanceof StationScreen screen) {
                screen.setStations(entries);
            }
        }

        public static void radioText(int frequency, String sender, String text) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.mcradio.radio_text", frequency, sender, text), false);
            }
        }
    }


    /**
     * Registry of active client-side radio streams: station streams keyed by the packed position of the
     * originating station, and walkie-talkie voice streams keyed by the speaker.
     */
    public static final class ClientAudioManager {
        private static final long STALE_NANOS = 8_000_000_000L;
        private static final int VOICE_PREBUFFER_SAMPLES = PcmBuffer.SAMPLE_RATE * 12 / 100;

        private static final Map<Long, ClientRadioStream> STREAMS = new HashMap<>();
        private static final Map<UUID, ClientRadioStream> VOICES = new HashMap<>();

        private ClientAudioManager() {
        }

        /** Returns the live stream for a station, creating it if necessary. Main thread only. */
        public static ClientRadioStream get(long station) {
            ClientRadioStream stream = STREAMS.get(station);
            if (stream == null || stream.isClosed()) {
                stream = new ClientRadioStream(station);
                STREAMS.put(station, stream);
            }
            return stream;
        }

        public static void accept(long station, int codec, byte[] data) {
            get(station).accept(codec, data);
            prune();
        }

        public static void end(long station) {
            ClientRadioStream stream = STREAMS.remove(station);
            if (stream != null) {
                stream.close();
            }
        }

        /** Appends walkie-talkie audio; starts a new sound (and announces the speaker) when a transmission begins. */
        public static void voice(UUID speaker, String name, int frequency, int mode, byte[] data) {
            ClientRadioStream stream = VOICES.get(speaker);
            if (stream == null || stream.isClosed()) {
                stream = new ClientRadioStream(0L, VOICE_PREBUFFER_SAMPLES);
                VOICES.put(speaker, stream);
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.getSoundManager().play(new WalkieSoundInstance(speaker, stream));
                LocalPlayer player = minecraft.player;
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.mcradio.voice." + mode, name, frequency), true);
                }
            }
            stream.accept(AudioCodec.ULAW_16K, data);
        }

        public static void endVoice(UUID speaker) {
            ClientRadioStream stream = VOICES.remove(speaker);
            if (stream != null) {
                stream.close();
            }
        }

        public static void clear() {
            STREAMS.values().forEach(ClientRadioStream::close);
            STREAMS.clear();
            VOICES.values().forEach(ClientRadioStream::close);
            VOICES.clear();
        }

        private static void prune() {
            long now = System.nanoTime();
            STREAMS.values().removeIf(stream -> {
                if (stream.isClosed()) {
                    return true;
                }
                if (now - stream.lastDataNanos() > STALE_NANOS) {
                    stream.close();
                    return true;
                }
                return false;
            });
        }
    }


    /** Streaming linear-interpolation resampler for mono 16-bit PCM. One instance per source stream. */
    static final class LinearResampler {
        private final int outputRate;
        private double position;
        private short last;

        LinearResampler(int outputRate) {
            this.outputRate = outputRate;
        }

        short[] process(short[] input, int length, int sourceRate) {
            if (length <= 0) {
                return new short[0];
            }
            if (sourceRate == outputRate) {
                short[] copy = new short[length];
                System.arraycopy(input, 0, copy, 0, length);
                last = input[length - 1];
                position = 0.0;
                return copy;
            }
            double step = (double) sourceRate / outputRate;
            short[] output = new short[(int) (length / step) + 2];
            int produced = 0;
            double p = position;
            while (p < length && produced < output.length) {
                int index = (int) p;
                double fraction = p - index;
                double a = index == 0 ? last : input[index - 1];
                double b = input[index];
                output[produced++] = (short) Math.round(a + (b - a) * fraction);
                p += step;
            }
            position = p - length;
            last = input[length - 1];
            if (produced == output.length) {
                return output;
            }
            short[] trimmed = new short[produced];
            System.arraycopy(output, 0, trimmed, 0, produced);
            return trimmed;
        }
    }


    /**
     * Thread-safe ring buffer of mono 44.1 kHz PCM with a jitter-buffer state machine. One instance per sound
     * instance (so several receivers can play the same station without stealing samples from each other).
     * While "buffering", the reader gets short silent chunks until enough audio has accumulated.
     */
    public static final class PcmBuffer {
        public static final int SAMPLE_RATE = 44100;
        public static final int DEFAULT_PREBUFFER_SAMPLES = SAMPLE_RATE / 2;
        private static final int SILENCE_CHUNK = SAMPLE_RATE / 20;
        private static final int AUDIO_CHUNK = SAMPLE_RATE / 10;
        private static final long IDLE_FLUSH_NANOS = 250_000_000L;

        private final short[] ring;
        private final int prebufferSamples;
        private int readPosition;
        private int size;
        private boolean buffering = true;
        private long lastWriteNanos = System.nanoTime();

        PcmBuffer(int capacity, int prebufferSamples) {
            this.ring = new short[capacity];
            this.prebufferSamples = prebufferSamples;
        }

        synchronized void write(short[] samples, int count) {
            for (int i = 0; i < count; i++) {
                int index = (readPosition + size) % ring.length;
                ring[index] = samples[i];
                if (size == ring.length) {
                    readPosition = (readPosition + 1) % ring.length;
                } else {
                    size++;
                }
            }
            lastWriteNanos = System.nanoTime();
        }

        /** Fills {@code destination} with at most {@code max} samples and returns how many (always at least one). */
        synchronized int readChunk(short[] destination, int max) {
            int limit = Math.max(1, Math.min(max, destination.length));
            if (buffering) {
                boolean idle = size > 0 && System.nanoTime() - lastWriteNanos > IDLE_FLUSH_NANOS;
                if (size >= prebufferSamples || idle) {
                    buffering = false;
                } else {
                    int silence = Math.min(limit, SILENCE_CHUNK);
                    Arrays.fill(destination, 0, silence, (short) 0);
                    return silence;
                }
            }
            int count = Math.min(Math.min(limit, AUDIO_CHUNK), size);
            if (count <= 0) {
                buffering = true;
                int silence = Math.min(limit, SILENCE_CHUNK);
                Arrays.fill(destination, 0, silence, (short) 0);
                return silence;
            }
            for (int i = 0; i < count; i++) {
                destination[i] = ring[readPosition];
                readPosition = (readPosition + 1) % ring.length;
            }
            size -= count;
            if (size == 0) {
                buffering = true;
            }
            return count;
        }

        synchronized void clear() {
            size = 0;
            readPosition = 0;
            buffering = true;
        }
    }


    /** Producer/consumer byte pipe: the network thread offers MP3 chunks, the decoder thread reads (blocking). */
    static final class BlockingByteStream extends InputStream {
        private static final int MAX_QUEUED_BYTES = 4 * 1024 * 1024;

        private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        private final AtomicInteger queuedBytes = new AtomicInteger();
        private byte[] current;
        private int offset;
        private volatile boolean closed;

        void offer(byte[] data) {
            if (closed || data.length == 0 || queuedBytes.get() > MAX_QUEUED_BYTES) {
                return;
            }
            queuedBytes.addAndGet(data.length);
            queue.offer(data);
        }

        private boolean ensureData() {
            while (current == null || offset >= current.length) {
                if (closed) {
                    return false;
                }
                try {
                    byte[] next = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (next != null) {
                        queuedBytes.addAndGet(-next.length);
                        current = next;
                        offset = 0;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }

        @Override
        public int read() {
            if (!ensureData()) {
                return -1;
            }
            return current[offset++] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int off, int len) {
            if (len == 0) {
                return 0;
            }
            if (!ensureData()) {
                return -1;
            }
            int count = Math.min(len, current.length - offset);
            System.arraycopy(current, offset, buffer, off, count);
            offset += count;
            return count;
        }

        @Override
        public int available() {
            return current == null ? 0 : Math.max(0, current.length - offset);
        }

        @Override
        public void close() {
            closed = true;
            queue.clear();
        }
    }


    /**
     * Client-side state of one station's stream: raw MP3 chunks are fed to a background JLayer decoder thread,
     * live voice chunks are expanded inline, and everything ends up as mono 44.1 kHz PCM that is fanned out to
     * every attached {@link PcmBuffer} (one per playing sound instance).
     */
    public static final class ClientRadioStream {
        private final long station;
        private final int prebufferSamples;
        private final CopyOnWriteArrayList<PcmBuffer> readers = new CopyOnWriteArrayList<>();
        private final LinearResampler voiceResampler = new LinearResampler(PcmBuffer.SAMPLE_RATE);

        private BlockingByteStream mp3Input;
        private Thread decoderThread;
        private volatile boolean closed;
        private volatile long lastDataNanos = System.nanoTime();

        ClientRadioStream(long station) {
            this(station, PcmBuffer.DEFAULT_PREBUFFER_SAMPLES);
        }

        ClientRadioStream(long station, int prebufferSamples) {
            this.station = station;
            this.prebufferSamples = prebufferSamples;
        }

        public long station() {
            return station;
        }

        public boolean isClosed() {
            return closed;
        }

        long lastDataNanos() {
            return lastDataNanos;
        }

        PcmBuffer attach() {
            PcmBuffer buffer = new PcmBuffer(PcmBuffer.SAMPLE_RATE * 4, prebufferSamples);
            readers.add(buffer);
            return buffer;
        }

        void detach(PcmBuffer buffer) {
            readers.remove(buffer);
        }

        /** Called on the main thread when an {@code AudioChunkPacket} arrives. */
        void accept(int codec, byte[] data) {
            if (closed) {
                return;
            }
            lastDataNanos = System.nanoTime();
            switch (codec) {
                case AudioCodec.MP3 -> acceptMp3(data);
                case AudioCodec.ULAW_16K -> acceptVoice(data);
                default -> McRadio.LOGGER.debug("Ignoring audio chunk with unknown codec {}", codec);
            }
        }

        private void acceptMp3(byte[] data) {
            if (decoderThread == null) {
                mp3Input = new BlockingByteStream();
                decoderThread = new Thread(this::decodeLoop, "mcradio-mp3-decoder-" + station);
                decoderThread.setDaemon(true);
                decoderThread.start();
            }
            mp3Input.offer(data);
        }

        private void acceptVoice(byte[] data) {
            short[] pcm = new short[data.length];
            for (int i = 0; i < data.length; i++) {
                pcm[i] = UlawCodec.decode(data[i]);
            }
            short[] resampled = voiceResampler.process(pcm, pcm.length, AudioCodec.VOICE_SAMPLE_RATE);
            push(resampled);
        }

        private void push(short[] mono) {
            if (mono.length == 0) {
                return;
            }
            for (PcmBuffer buffer : readers) {
                buffer.write(mono, mono.length);
            }
        }

        private void decodeLoop() {
            LinearResampler resampler = new LinearResampler(PcmBuffer.SAMPLE_RATE);
            Bitstream bitstream = null;
            try {
                bitstream = new Bitstream(mp3Input);
                Decoder decoder = new Decoder();
                int consecutiveErrors = 0;
                while (!closed) {
                    try {
                        Header header = bitstream.readFrame();
                        if (header == null) {
                            break;
                        }
                        SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                        int channels = Math.max(1, decoder.getOutputChannels());
                        short[] interleaved = samples.getBuffer();
                        int frames = samples.getBufferLength() / channels;
                        short[] mono = new short[frames];
                        for (int i = 0; i < frames; i++) {
                            int sum = 0;
                            for (int c = 0; c < channels; c++) {
                                sum += interleaved[i * channels + c];
                            }
                            mono[i] = (short) (sum / channels);
                        }
                        push(resampler.process(mono, frames, decoder.getOutputFrequency()));
                        bitstream.closeFrame();
                        consecutiveErrors = 0;
                    } catch (JavaLayerException | RuntimeException e) {
                        if (closed || ++consecutiveErrors > 400) {
                            break;
                        }
                        bitstream.closeFrame();
                    }
                }
            } catch (JavaLayerException | RuntimeException e) {
                if (!closed) {
                    McRadio.LOGGER.debug("MP3 decoder for station {} stopped", station, e);
                }
            } finally {
                if (bitstream != null) {
                    try {
                        bitstream.close();
                    } catch (JavaLayerException ignored) {
                        // nothing left to release
                    }
                }
            }
        }

        void close() {
            closed = true;
            if (mp3Input != null) {
                mp3Input.close();
            }
            if (decoderThread != null) {
                decoderThread.interrupt();
            }
            readers.clear();
        }
    }


    /**
     * Adapter that lets Minecraft's streaming sound channel pull PCM from a {@link ClientRadioStream}.
     * It never ends on its own: while no audio is available it hands out short silent chunks, so the OpenAL
     * source keeps running and playback resumes seamlessly when data arrives.
     */
    static final class RadioAudioStream implements AudioStream {
        private static final AudioFormat FORMAT = new AudioFormat(PcmBuffer.SAMPLE_RATE, 16, 1, true,
                ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);

        private final ClientRadioStream stream;
        private final PcmBuffer buffer;
        private volatile boolean closed;

        RadioAudioStream(ClientRadioStream stream) {
            this.stream = stream;
            this.buffer = stream.attach();
        }

        @Override
        public AudioFormat getFormat() {
            return FORMAT;
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            if (closed || stream.isClosed()) {
                return ByteBuffer.allocateDirect(0);
            }
            int maxSamples = Math.max(1, size / 2);
            short[] samples = new short[Math.min(maxSamples, PcmBuffer.SAMPLE_RATE)];
            int count = buffer.readChunk(samples, maxSamples);
            ByteBuffer out = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.nativeOrder());
            out.asShortBuffer().put(samples, 0, count);
            return out;
        }

        @Override
        public void close() {
            closed = true;
            stream.detach(buffer);
            buffer.clear();
        }
    }


    /**
     * A positional, tickable sound whose audio data does not come from a .ogg file but from a live
     * {@link ClientRadioStream}. The event {@code mcradio:radio_stream} is declared in sounds.json (it points at
     * a vanilla placeholder file that is never actually read because {@link #getStream} supplies the data).
     */
    public static final class RadioSoundInstance extends AbstractTickableSoundInstance {
        private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "radio_stream"));
        private static final double STOP_DISTANCE_SQR = 64.0 * 64.0;

        private final ReceiverBlockEntity receiver;
        private final ClientRadioStream stream;

        public RadioSoundInstance(ReceiverBlockEntity receiver, ClientRadioStream stream) {
            super(EVENT, SoundSource.RECORDS, RandomSource.create());
            this.receiver = receiver;
            this.stream = stream;
            this.looping = false;
            this.delay = 0;
            this.volume = receiver.getVolume() / 100.0F;
            this.x = receiver.getBlockPos().getX() + 0.5;
            this.y = receiver.getBlockPos().getY() + 0.5;
            this.z = receiver.getBlockPos().getZ() + 0.5;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return CompletableFuture.completedFuture(new RadioAudioStream(stream));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (receiver.isRemoved() || !receiver.isReceiving() || stream.isClosed()
                    || receiver.getSourceStation() != stream.station()) {
                stop();
                return;
            }
            Player player = Minecraft.getInstance().player;
            if (player == null || player.distanceToSqr(x, y, z) > STOP_DISTANCE_SQR) {
                stop();
                return;
            }
            this.volume = receiver.getVolume() / 100.0F;
        }
    }


    /** Per-tick client logic of a receiver: starts a sound instance when it has a signal and the player is near. */
    public static final class ClientReceiverLogic {
        private static final double START_DISTANCE_SQR = 48.0 * 48.0;

        private ClientReceiverLogic() {
        }

        public static void tick(ReceiverBlockEntity receiver) {
            if (!receiver.isReceiving()) {
                receiver.clientSound = null;
                return;
            }
            if (receiver.clientSound instanceof RadioSoundInstance existing && !existing.isStopped()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || player.distanceToSqr(Vec3.atCenterOf(receiver.getBlockPos())) > START_DISTANCE_SQR) {
                return;
            }
            RadioSoundInstance instance = new RadioSoundInstance(receiver, ClientAudioManager.get(receiver.getSourceStation()));
            receiver.clientSound = instance;
            minecraft.getSoundManager().play(instance);
        }
    }


    /**
     * Push-to-talk microphone capture: 16 kHz mono, mu-law encoded, handed out in 100 ms chunks to a sink.
     * The sink returns false to stop the capture.
     */
    public static final class MicCapture {
        private static final AudioFormat FORMAT = new AudioFormat(AudioCodec.VOICE_SAMPLE_RATE, 16, 1, true, false);
        private static final int PACKET_SAMPLES = AudioCodec.VOICE_SAMPLE_RATE / 10;

        private static volatile boolean running;

        private MicCapture() {
        }

        public static boolean isActive() {
            return running;
        }

        /** Captures for a distributor GUI: chunks are sent to the server as {@link LiveAudioPacket}s. */
        public static boolean start(BlockPos station) {
            BlockPos target = station.immutable();
            return begin(data -> {
                LiveAudioPacket packet = new LiveAudioPacket(target, data);
                Minecraft.getInstance().execute(() -> RadioNetwork.sendToServer(packet));
                return true;
            });
        }

        /** Captures for a custom sink (used by the walkie-talkie). Returns false if no microphone is available. */
        public static boolean start(Predicate<byte[]> sink) {
            return begin(sink);
        }

        private static synchronized boolean begin(Predicate<byte[]> sink) {
            if (running) {
                return true;
            }
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
                if (!AudioSystem.isLineSupported(info)) {
                    return false;
                }
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(FORMAT, PACKET_SAMPLES * 4);
                line.start();
                running = true;
                Thread thread = new Thread(() -> captureLoop(line, sink), "mcradio-microphone");
                thread.setDaemon(true);
                thread.start();
                return true;
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException | LinkageError e) {
                McRadio.LOGGER.warn("Microphone unavailable: {}", e.toString());
                running = false;
                return false;
            }
        }

        public static synchronized void stop() {
            running = false;
        }

        private static void captureLoop(TargetDataLine line, Predicate<byte[]> sink) {
            byte[] raw = new byte[PACKET_SAMPLES * 2];
            try {
                while (running) {
                    int read = line.read(raw, 0, raw.length);
                    if (read <= 0) {
                        continue;
                    }
                    int samples = read / 2;
                    byte[] encoded = new byte[samples];
                    for (int i = 0; i < samples; i++) {
                        short sample = (short) ((raw[i * 2] & 0xFF) | (raw[i * 2 + 1] << 8));
                        encoded[i] = UlawCodec.encode(sample);
                    }
                    if (!sink.test(encoded)) {
                        running = false;
                    }
                }
            } finally {
                line.stop();
                line.close();
                running = false;
            }
        }
    }


    /** Entry points from common item classes into client code (only ever called when the level is client-side). */
    public static final class ClientHooks {
        private ClientHooks() {
        }

        public static void openWalkie(InteractionHand hand, int frequency) {
            Minecraft.getInstance().setScreen(new WalkieScreen(hand, frequency));
        }

        public static void openTablet() {
            Minecraft.getInstance().setScreen(new TabletScreen());
        }

        public static void walkieTick(ItemStack stack, Entity entity) {
            WalkieClientLogic.tick(stack, entity);
        }
    }


    /** Client-side push-to-talk handling for a held walkie-talkie. */
    public static final class WalkieClientLogic {
        public static final KeyMapping PUSH_TO_TALK = new KeyMapping("key.mcradio.push_to_talk",
                InputConstants.KEY_V, "key.categories.mcradio");

        private static final long HELD_GRACE_MILLIS = 500L;

        private static volatile long lastHeldMillis;
        private static boolean ownsMicrophone;

        private WalkieClientLogic() {
        }

        /** Called every client tick for every walkie-talkie stack in the local player's inventory. */
        public static void tick(ItemStack stack, Entity entity) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || entity != player) {
                return;
            }
            if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) {
                return;
            }
            boolean wantsToTalk = minecraft.screen == null && PUSH_TO_TALK.isDown();
            if (wantsToTalk) {
                lastHeldMillis = System.currentTimeMillis();
                if (!MicCapture.isActive()) {
                    ownsMicrophone = MicCapture.start(WalkieClientLogic::sendVoice);
                    if (!ownsMicrophone) {
                        player.displayClientMessage(Component.translatable("gui.mcradio.mic_unavailable"), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.mcradio.transmitting",
                                WalkieTalkieItem.getFrequency(stack)), true);
                    }
                }
            } else if (ownsMicrophone) {
                ownsMicrophone = false;
                MicCapture.stop();
            }
        }

        private static boolean sendVoice(byte[] data) {
            if (System.currentTimeMillis() - lastHeldMillis > HELD_GRACE_MILLIS) {
                return false;
            }
            WalkieVoicePacket packet = new WalkieVoicePacket(data);
            Minecraft.getInstance().execute(() -> RadioNetwork.sendToServer(packet));
            return true;
        }
    }


    /** Non-positional sound of one walkie-talkie speaker, fed by that speaker's {@link ClientRadioStream}. */
    public static final class WalkieSoundInstance extends AbstractTickableSoundInstance {
        private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(McRadio.MOD_ID, "radio_stream"));
        private static final long SILENCE_TIMEOUT_NANOS = 3_000_000_000L;

        private final UUID speaker;
        private final ClientRadioStream stream;

        public WalkieSoundInstance(UUID speaker, ClientRadioStream stream) {
            super(EVENT, SoundSource.VOICE, RandomSource.create());
            this.speaker = speaker;
            this.stream = stream;
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return CompletableFuture.completedFuture(new RadioAudioStream(stream));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (stream.isClosed() || System.nanoTime() - stream.lastDataNanos() > SILENCE_TIMEOUT_NANOS) {
                ClientAudioManager.endVoice(speaker);
                stop();
            }
        }
    }


    /** Shared drawing helpers that give all radio GUIs the same dark "equipment rack" look (no texture files needed). */
    static final class RadioUi {
        static final int TEXT = 0xFFE6F7F5;
        static final int MUTED = 0xFF8FA3B5;
        static final int ACCENT = 0xFF2EC4B6;
        static final int OK = 0xFF4CD964;
        static final int BAD = 0xFFFF5A5F;
        static final int WARN = 0xFFFFC857;

        private RadioUi() {
        }

        static void panel(GuiGraphics g, int x, int y, int w, int h, Component title, Font font) {
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF05080C);
            g.fillGradient(x, y, x + w, y + h, 0xFF1C2535, 0xFF0E131B);
            g.fill(x, y, x + w, y + 22, 0xFF243350);
            g.fill(x, y + 22, x + w, y + 23, ACCENT);
            g.drawString(font, title, x + 12, y + 7, TEXT, false);
            for (int i = 0; i < 3; i++) {
                g.fill(x + w - 18 - i * 9, y + 8, x + w - 12 - i * 9, y + 14, i == 0 ? ACCENT : 0xFF3A4A68);
            }
            screw(g, x + 3, y + h - 7);
            screw(g, x + w - 7, y + h - 7);
        }

        private static void screw(GuiGraphics g, int x, int y) {
            g.fill(x, y, x + 4, y + 4, 0xFF4B5A74);
            g.fill(x + 1, y + 1, x + 3, y + 3, 0xFF202938);
        }

        static void inset(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, 0xFF05080C);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF121821);
        }

        static void led(GuiGraphics g, int x, int y, boolean on, int onColor) {
            g.fill(x, y, x + 8, y + 8, 0xFF05080C);
            g.fill(x + 1, y + 1, x + 7, y + 7, on ? onColor : 0xFF2A3140);
            if (on) {
                g.fill(x + 2, y + 2, x + 4, y + 4, 0x80FFFFFF);
            }
        }

        /** Animated level meter, purely decorative. */
        static void equalizer(GuiGraphics g, int x, int y, int bars, boolean active) {
            long t = Util.getMillis();
            for (int i = 0; i < bars; i++) {
                double level = active ? 3 + 11 * Math.abs(Math.sin(t / 220.0 + i * 0.9) * Math.cos(t / 370.0 + i * 0.5)) : 2;
                int top = y + 14 - (int) level;
                g.fill(x + i * 5, top, x + i * 5 + 3, y + 14, active ? ACCENT : 0xFF2A3140);
            }
        }
    }


    /** Base class of the three device screens: centred custom-drawn panel plus menu bookkeeping. */
    static abstract class RadioScreen extends Screen implements MenuAccess<RadioDeviceMenu> {
        protected final RadioDeviceMenu menu;
        protected final int panelWidth;
        protected final int panelHeight;
        protected int left;
        protected int top;

        protected RadioScreen(RadioDeviceMenu menu, Component title, int panelWidth, int panelHeight) {
            super(title);
            this.menu = menu;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
        }

        @Override
        public RadioDeviceMenu getMenu() {
            return menu;
        }

        @Override
        protected void init() {
            left = (width - panelWidth) / 2;
            top = (height - panelHeight) / 2;
            buildWidgets();
        }

        protected abstract void buildWidgets();

        protected abstract void drawForeground(GuiGraphics graphics, int mouseX, int mouseY);

        protected void onEnter() {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        /** The background is drawn in {@link #render}; this prevents the default blur from being applied twice. */
        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xB0080C12, 0xD0080C12);
            RadioUi.panel(graphics, left, top, panelWidth, panelHeight, title, font);
            super.render(graphics, mouseX, mouseY, partialTick);
            drawForeground(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                onEnter();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void onClose() {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            } else {
                super.onClose();
            }
        }

        protected EditBox frequencyBox(int x, int y, int w, int initial) {
            EditBox box = new EditBox(font, x, y, w, 16, Component.translatable("gui.mcradio.frequency"));
            box.setMaxLength(4);
            box.setFilter(value -> value.matches("\\d{0,4}"));
            box.setValue(Integer.toString(initial));
            addRenderableWidget(box);
            return box;
        }

        protected static int parseFrequency(EditBox box, int fallback) {
            try {
                return RadioPhysics.clampFrequency(Integer.parseInt(box.getValue().trim()));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        protected static Minecraft mc() {
            return Minecraft.getInstance();
        }
    }


    /** Scrollable single-selection list of MP3 file names (mouse wheel + click). */
    static final class TrackListWidget extends AbstractWidget {
        private static final int ROW_HEIGHT = 14;

        private List<String> entries = List.of();
        private int selected = -1;
        private double scroll;

        TrackListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.translatable("gui.mcradio.tracks"));
        }

        void setEntries(List<String> newEntries, String preselect) {
            entries = List.copyOf(newEntries);
            selected = preselect == null ? -1 : entries.indexOf(preselect);
            scroll = Mth.clamp(scroll, 0, maxScroll());
        }

        int getSelectedIndex() {
            return selected >= 0 && selected < entries.size() ? selected : -1;
        }

        String getSelected() {
            return selected >= 0 && selected < entries.size() ? entries.get(selected) : null;
        }

        private int maxScroll() {
            return Math.max(0, entries.size() * ROW_HEIGHT - (height - 4));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            RadioUi.inset(graphics, getX(), getY(), width, height);
            if (entries.isEmpty()) {
                graphics.drawCenteredString(font, Component.translatable("gui.mcradio.no_tracks"),
                        getX() + width / 2, getY() + height / 2 - 4, RadioUi.MUTED);
                return;
            }
            graphics.enableScissor(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2);
            for (int i = 0; i < entries.size(); i++) {
                int rowY = getY() + 2 + i * ROW_HEIGHT - (int) scroll;
                if (rowY + ROW_HEIGHT < getY() || rowY > getY() + height) {
                    continue;
                }
                boolean hovered = mouseX >= getX() && mouseX < getX() + width - 6 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
                        && mouseY >= getY() && mouseY < getY() + height;
                if (i == selected) {
                    graphics.fill(getX() + 2, rowY, getX() + width - 6, rowY + ROW_HEIGHT, 0xFF1F5F63);
                } else if (hovered) {
                    graphics.fill(getX() + 2, rowY, getX() + width - 6, rowY + ROW_HEIGHT, 0xFF222C3D);
                }
                String name = font.plainSubstrByWidth(entries.get(i), width - 18);
                graphics.drawString(font, name, getX() + 6, rowY + 3, i == selected ? RadioUi.TEXT : 0xFFB8C7D6, false);
            }
            graphics.disableScissor();

            int max = maxScroll();
            if (max > 0) {
                int trackHeight = height - 4;
                int thumb = Math.max(10, trackHeight * trackHeight / (entries.size() * ROW_HEIGHT));
                int thumbY = getY() + 2 + (int) ((trackHeight - thumb) * (scroll / max));
                graphics.fill(getX() + width - 5, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFF0A0E14);
                graphics.fill(getX() + width - 5, thumbY, getX() + width - 2, thumbY + thumb, RadioUi.ACCENT);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) {
                return false;
            }
            int index = (int) ((mouseY - getY() - 2 + scroll) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                selected = index;
                playDownSound(Minecraft.getInstance().getSoundManager());
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }
            scroll = Mth.clamp(scroll - scrollY * ROW_HEIGHT, 0, maxScroll());
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }


    /** Push-to-talk button: active only while the mouse button is held down. */
    static final class HoldButton extends AbstractWidget {
        private final Component idleLabel;
        private final Component heldLabel;
        private final Runnable onPress;
        private final Runnable onRelease;
        private boolean held;

        HoldButton(int x, int y, int width, int height, Component idleLabel, Component heldLabel,
                   Runnable onPress, Runnable onRelease) {
            super(x, y, width, height, idleLabel);
            this.idleLabel = idleLabel;
            this.heldLabel = heldLabel;
            this.onPress = onPress;
            this.onRelease = onRelease;
        }

        boolean isHeld() {
            return held;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!held) {
                held = true;
                setMessage(heldLabel);
                onPress.run();
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            forceRelease();
        }

        /** Releases the button even if the mouse was let go outside of it. */
        void forceRelease() {
            if (held) {
                held = false;
                setMessage(idleLabel);
                onRelease.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = held ? RadioUi.BAD : RadioUi.ACCENT;
            int fill = held ? 0xFFA3202B : (isHoveredOrFocused() ? 0xFF2F4468 : 0xFF263650);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF05080C);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, border);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, fill);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                    getY() + (height - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }


    /** 0-100 % slider; the new value is reported once, when the mouse button is released. */
    static final class VolumeSlider extends AbstractSliderButton {
        private final IntConsumer onCommit;

        VolumeSlider(int x, int y, int width, int height, int percent, IntConsumer onCommit) {
            super(x, y, width, height, Component.empty(), percent / 100.0);
            this.onCommit = onCommit;
            updateMessage();
        }

        int percent() {
            return (int) Math.round(value * 100.0);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.mcradio.volume", percent()));
        }

        @Override
        protected void applyValue() {
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            onCommit.accept(percent());
        }
    }


    /**
     * GUI of the Signal Distributor and, identically, of a Computer linked to one: frequency, Playlist/Live
     * mode switch, track list, push-to-talk, text. An unlinked computer first shows a connection page.
     */
    public static final class StationScreen extends RadioScreen {
        private final boolean computer;
        private final boolean linked;

        private Mode mode;
        private int frequency;
        private boolean broadcasting;
        private String currentTrack;
        private final int radius;

        private List<String> tracks = List.of();
        private boolean tracksRequested;
        private boolean micFailed;

        private List<StationListPacket.Entry> stations = List.of();
        private boolean stationsRequested;

        private EditBox frequencyBox;
        private EditBox textBox;
        private TrackListWidget trackList;
        private TrackListWidget stationList;
        private HoldButton talkButton;

        public StationScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 268, 232);
            RadioDeviceMenu.DeviceData data = menu.data();
            this.computer = menu.getType() == ModRegistry.COMPUTER_MENU.get();
            this.linked = !computer || data.c() >= 0;
            this.frequency = data.a();
            this.mode = Mode.of(data.b());
            this.radius = Math.max(0, data.c());
            this.broadcasting = data.flag();
            this.currentTrack = data.text();
        }

        /** Called when the server's track list arrives. */
        public void setTracks(List<String> newTracks) {
            tracks = List.copyOf(newTracks);
            if (trackList != null) {
                trackList.setEntries(tracks, currentTrack);
            }
        }

        /** Called when the server's list of connectable distributors arrives (computer, not linked). */
        public void setStations(List<StationListPacket.Entry> newStations) {
            stations = List.copyOf(newStations);
            if (stationList != null) {
                stationList.setEntries(stationLabels(), null);
            }
        }

        private List<String> stationLabels() {
            return stations.stream()
                    .map(entry -> String.format(Locale.ROOT, "%d, %d, %d    %d kHz%s", entry.pos().getX(), entry.pos().getY(),
                            entry.pos().getZ(), entry.frequency(), entry.onAir() ? "    [ON AIR]" : ""))
                    .toList();
        }

        @Override
        protected void buildWidgets() {
            if (!linked) {
                buildLinkWidgets();
                return;
            }
            int innerWidth = panelWidth - 24;

            frequencyBox = frequencyBox(left + 90, top + 31, 56, frequency);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.apply"), button -> applyFrequency())
                    .bounds(left + 152, top + 30, 56, 18).build());

            Component modeName = Component.translatable(mode == Mode.PLAYLIST ? "gui.mcradio.mode.playlist" : "gui.mcradio.mode.live");
            int modeWidth = computer ? innerWidth - 84 : innerWidth;
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.mode", modeName), button -> toggleMode())
                    .bounds(left + 12, top + 54, modeWidth, 20).build());
            if (computer) {
                addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.disconnect"), button -> disconnect())
                        .bounds(left + 12 + innerWidth - 80, top + 54, 80, 20).build());
            }

            if (mode == Mode.PLAYLIST) {
                buildPlaylistWidgets(innerWidth);
            } else {
                buildLiveWidgets(innerWidth);
            }
        }

        private void buildLinkWidgets() {
            int innerWidth = panelWidth - 24;
            stationList = new TrackListWidget(left + 12, top + 52, innerWidth, 112);
            stationList.setEntries(stationLabels(), null);
            addRenderableWidget(stationList);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.connect"), button -> connectSelected())
                    .bounds(left + 12, top + 170, 110, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.refresh"), button -> requestStations())
                    .bounds(left + panelWidth - 12 - 110, top + 170, 110, 20).build());
            if (!stationsRequested) {
                requestStations();
            }
        }

        private void buildPlaylistWidgets(int innerWidth) {
            trackList = new TrackListWidget(left + 12, top + 80, innerWidth, 84);
            trackList.setEntries(tracks, currentTrack);
            addRenderableWidget(trackList);

            int buttonWidth = (innerWidth - 8) / 3;
            int buttonY = top + 170;
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.broadcast"), button -> broadcastSelected())
                    .bounds(left + 12, buttonY, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.stop"), button -> stopBroadcast())
                    .bounds(left + 16 + buttonWidth, buttonY, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.refresh"), button -> requestTracks())
                    .bounds(left + 20 + buttonWidth * 2, buttonY, buttonWidth, 20).build());

            if (!tracksRequested) {
                requestTracks();
            }
        }

        private void buildLiveWidgets(int innerWidth) {
            trackList = null;
            talkButton = new HoldButton(left + 12, top + 84, innerWidth, 34,
                    Component.translatable("gui.mcradio.hold_to_talk"), Component.translatable("gui.mcradio.talking"),
                    () -> micFailed = !MicCapture.start(menu.pos()), MicCapture::stop);
            addRenderableWidget(talkButton);

            textBox = new EditBox(font, left + 12, top + 130, innerWidth - 66, 18, Component.translatable("gui.mcradio.text_hint"));
            textBox.setMaxLength(120);
            textBox.setHint(Component.translatable("gui.mcradio.text_hint"));
            addRenderableWidget(textBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.send"), button -> sendText())
                    .bounds(left + panelWidth - 12 - 60, top + 129, 60, 20).build());
        }

        private void requestTracks() {
            tracksRequested = true;
            RadioNetwork.sendToServer(new TrackListRequestPacket());
        }

        private void requestStations() {
            stationsRequested = true;
            RadioNetwork.sendToServer(new StationListRequestPacket());
        }

        private void connectSelected() {
            int index = stationList == null ? -1 : stationList.getSelectedIndex();
            if (index < 0 || index >= stations.size()) {
                return;
            }
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.COMPUTER_LINK, 0, 0,
                    Long.toString(stations.get(index).pos().asLong())));
        }

        private void disconnect() {
            MicCapture.stop();
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.COMPUTER_UNLINK, 0, 0, ""));
        }

        private void applyFrequency() {
            frequency = parseFrequency(frequencyBox, frequency);
            frequencyBox.setValue(Integer.toString(frequency));
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_APPLY, frequency, mode.ordinal(), ""));
        }

        private void toggleMode() {
            MicCapture.stop();
            mode = mode == Mode.PLAYLIST ? Mode.LIVE : Mode.PLAYLIST;
            broadcasting = false;
            frequency = parseFrequency(frequencyBox, frequency);
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_APPLY, frequency, mode.ordinal(), ""));
            rebuildWidgets();
        }

        private void broadcastSelected() {
            String selected = trackList == null ? null : trackList.getSelected();
            if (selected == null) {
                return;
            }
            currentTrack = selected;
            broadcasting = true;
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_BROADCAST, 0, 0, selected));
        }

        private void stopBroadcast() {
            broadcasting = false;
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_STOP, 0, 0, ""));
        }

        private void sendText() {
            String message = textBox.getValue().strip();
            if (message.isEmpty()) {
                return;
            }
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.STATION_TEXT, 0, 0, message));
            textBox.setValue("");
        }

        @Override
        protected void onEnter() {
            if (frequencyBox != null && frequencyBox.isFocused()) {
                applyFrequency();
            } else if (textBox != null && textBox.isFocused()) {
                sendText();
            }
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            if (!linked) {
                graphics.drawString(font, Component.translatable("gui.mcradio.computer.not_connected"), left + 12, top + 34, RadioUi.WARN, false);
                graphics.drawString(font, Component.translatable("gui.mcradio.computer.hint"), left + 12, top + 198, RadioUi.MUTED, false);
                return;
            }
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 35, RadioUi.TEXT, false);
            Component range = Component.translatable("gui.mcradio.range", radius);
            graphics.drawString(font, range, left + panelWidth - 12 - font.width(range), top + 35, RadioUi.MUTED, false);

            boolean onAir = mode == Mode.LIVE || broadcasting;
            RadioUi.led(graphics, left + 12, top + 204, onAir, RadioUi.OK);
            Component status;
            if (mode == Mode.LIVE) {
                status = micFailed ? Component.translatable("gui.mcradio.mic_unavailable")
                        : Component.translatable("gui.mcradio.status.live", frequency);
            } else if (broadcasting) {
                status = Component.literal(font.plainSubstrByWidth(
                        Component.translatable("gui.mcradio.status.onair", currentTrack).getString(), 150));
            } else {
                status = Component.translatable("gui.mcradio.status.idle");
            }
            graphics.drawString(font, status, left + 26, top + 205, micFailed && mode == Mode.LIVE ? RadioUi.BAD : RadioUi.TEXT, false);
            RadioUi.equalizer(graphics, left + panelWidth - 12 - 60, top + 198, 12,
                    onAir && (mode == Mode.PLAYLIST || (talkButton != null && talkButton.isHeld())));

            if (mode == Mode.LIVE) {
                graphics.drawString(font, Component.translatable("gui.mcradio.live_hint"), left + 12, top + 156, RadioUi.MUTED, false);
            } else {
                graphics.drawString(font, Component.translatable("gui.mcradio.folder_hint",
                        TrackLibrary.folder().getParent().getFileName() + "/" + TrackLibrary.folder().getFileName()),
                        left + 12, top + 218, RadioUi.MUTED, false);
            }
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (talkButton != null) {
                talkButton.forceRelease();
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void removed() {
            MicCapture.stop();
            super.removed();
        }
    }


    /** GUI of the Radio Receiver: tuned frequency, volume, and live signal indicator. */
    public static final class ReceiverScreen extends RadioScreen {
        private int frequency;
        private int volume;
        private EditBox frequencyBox;

        public ReceiverScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 240, 140);
            this.frequency = menu.data().a();
            this.volume = menu.data().b();
        }

        @Override
        protected void buildWidgets() {
            frequencyBox = frequencyBox(left + 90, top + 35, 56, frequency);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.tune"), button -> send())
                    .bounds(left + 152, top + 34, 76, 18).build());
            addRenderableWidget(new VolumeSlider(left + 12, top + 62, panelWidth - 24, 20, volume, percent -> {
                volume = percent;
                send();
            }));
        }

        private void send() {
            frequency = parseFrequency(frequencyBox, frequency);
            frequencyBox.setValue(Integer.toString(frequency));
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.RECEIVER_SET, frequency, volume, ""));
        }

        @Override
        protected void onEnter() {
            send();
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 39, RadioUi.TEXT, false);

            boolean locked = mc().level != null && mc().level.getBlockEntity(menu.pos()) instanceof ReceiverBlockEntity receiver
                    && receiver.isReceiving();
            RadioUi.led(graphics, left + 12, top + 96, locked, RadioUi.OK);
            graphics.drawString(font, Component.translatable(locked ? "gui.mcradio.signal.locked" : "gui.mcradio.signal.none"),
                    left + 26, top + 97, locked ? RadioUi.OK : RadioUi.WARN, false);
            RadioUi.equalizer(graphics, left + panelWidth - 12 - 60, top + 90, 12, locked);
            graphics.drawString(font, Component.translatable("gui.mcradio.receiver_hint"), left + 12, top + 118, RadioUi.MUTED, false);
        }
    }


    /** GUI of the Signal Repeater: input frequency to catch, output frequency to re-emit on. */
    public static final class RepeaterScreen extends RadioScreen {
        private int inputFrequency;
        private int outputFrequency;
        private final int radius;
        private EditBox inputBox;
        private EditBox outputBox;

        public RepeaterScreen(RadioDeviceMenu menu, Inventory inventory, Component title) {
            super(menu, title, 240, 154);
            this.inputFrequency = menu.data().a();
            this.outputFrequency = menu.data().b();
            this.radius = menu.data().c();
        }

        @Override
        protected void buildWidgets() {
            inputBox = frequencyBox(left + 140, top + 33, 60, inputFrequency);
            outputBox = frequencyBox(left + 140, top + 57, 60, outputFrequency);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.apply"), button -> send())
                    .bounds(left + 12, top + 84, panelWidth - 24, 20).build());
        }

        private void send() {
            inputFrequency = parseFrequency(inputBox, inputFrequency);
            outputFrequency = parseFrequency(outputBox, outputFrequency);
            inputBox.setValue(Integer.toString(inputFrequency));
            outputBox.setValue(Integer.toString(outputFrequency));
            RadioNetwork.sendToServer(new ConfigPacket(menu.pos(), ConfigPacket.Action.REPEATER_SET,
                    inputFrequency, outputFrequency, ""));
        }

        @Override
        protected void onEnter() {
            send();
        }

        @Override
        protected void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(font, Component.translatable("gui.mcradio.input_frequency"), left + 12, top + 37, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.output_frequency"), left + 12, top + 61, RadioUi.TEXT, false);

            boolean active = mc().level != null && mc().level.getBlockEntity(menu.pos()) instanceof RepeaterBlockEntity repeater
                    && repeater.isActive();
            RadioUi.led(graphics, left + 12, top + 114, active, RadioUi.OK);
            graphics.drawString(font, Component.translatable(active ? "gui.mcradio.repeater.relaying" : "gui.mcradio.repeater.waiting"),
                    left + 26, top + 115, active ? RadioUi.OK : RadioUi.WARN, false);
            Component range = Component.translatable("gui.mcradio.range", radius);
            graphics.drawString(font, range, left + panelWidth - 12 - font.width(range), top + 115, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.repeater_hint"), left + 12, top + 134, RadioUi.MUTED, false);
        }
    }


    /** Frequency dialog of the walkie-talkie item. */
    public static final class WalkieScreen extends Screen {
        private static final int WIDTH = 252;
        private static final int HEIGHT = 128;

        private final InteractionHand hand;
        private int frequency;
        private EditBox frequencyBox;
        private int left;
        private int top;

        public WalkieScreen(InteractionHand hand, int frequency) {
            super(Component.translatable("item.mcradio.walkie_talkie"));
            this.hand = hand;
            this.frequency = frequency;
        }

        @Override
        protected void init() {
            left = (width - WIDTH) / 2;
            top = (height - HEIGHT) / 2;
            frequencyBox = new EditBox(font, left + 90, top + 35, 56, 16, Component.translatable("gui.mcradio.frequency"));
            frequencyBox.setMaxLength(4);
            frequencyBox.setFilter(value -> value.matches("\\d{0,4}"));
            frequencyBox.setValue(Integer.toString(frequency));
            addRenderableWidget(frequencyBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.tune"), button -> apply())
                    .bounds(left + 152, top + 34, 88, 18).build());
        }

        private void apply() {
            try {
                frequency = RadioPhysics.clampFrequency(Integer.parseInt(frequencyBox.getValue().trim()));
            } catch (NumberFormatException e) {
                // keep the previous frequency
            }
            frequencyBox.setValue(Integer.toString(frequency));
            RadioNetwork.sendToServer(new WalkieConfigPacket(hand, frequency));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xB0080C12, 0xD0080C12);
            RadioUi.panel(graphics, left, top, WIDTH, HEIGHT, title, font);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, Component.translatable("gui.mcradio.frequency"), left + 12, top + 39, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.walkie.hint",
                    WalkieClientLogic.PUSH_TO_TALK.getTranslatedKeyMessage()), left + 12, top + 66, RadioUi.OK, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.walkie.range", RadioPhysics.WALKIE_RANGE),
                    left + 12, top + 82, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.walkie.repeater"),
                    left + 12, top + 96, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.walkie.warning"),
                    left + 12, top + 110, RadioUi.WARN, false);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                apply();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }


    /**
     * Radio Tablet: a top-down map of the loaded terrain with every repeater, distributor, receiver and jammer
     * the server knows about, and their coverage areas (colour = frequency). Drag to pan, wheel to zoom.
     * Terrain is only drawn where chunks are loaded on the client; devices are listed by the server for the
     * whole dimension as long as their chunks are loaded there.
     */
    public static final class TabletScreen extends Screen {
        private static final int CELLS = 56;
        private static final int CELL_PX = 4;
        private static final int MAP_PX = CELLS * CELL_PX;
        private static final int[] BLOCKS_PER_CELL = {2, 4, 8, 16, 32};
        private static final int PANEL_WIDTH = 346;
        private static final int PANEL_HEIGHT = 272;
        private static final int UNKNOWN_TERRAIN = 0xFF0B0F15;
        private static final int[] KIND_COLORS = {0xFF2EC4B6, 0xFFFF9F1C, 0xFFE6F7F5, 0xFFFF5A5F};

        private final int[] terrain = new int[CELLS * CELLS];
        private List<MapDataPacket.Node> nodes = List.of();
        private int zoom = 2;
        private double centerX;
        private double centerZ;
        private int left;
        private int top;
        private int mapX;
        private int mapY;
        private int ticks;
        private int filter;
        private EditBox filterBox;

        public TabletScreen() {
            super(Component.translatable("item.mcradio.radio_tablet"));
            centerOnPlayer();
        }

        public void setNodes(List<MapDataPacket.Node> newNodes) {
            nodes = List.copyOf(newNodes);
        }

        private void centerOnPlayer() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                centerX = player.getX();
                centerZ = player.getZ();
            }
        }

        @Override
        protected void init() {
            left = (width - PANEL_WIDTH) / 2;
            top = (height - PANEL_HEIGHT) / 2;
            mapX = left + 12;
            mapY = top + 32;
            int columnX = left + 246;

            filterBox = new EditBox(font, columnX, top + 50, 88, 16, Component.translatable("gui.mcradio.map.filter"));
            filterBox.setMaxLength(4);
            filterBox.setFilter(value -> value.matches("\\d{0,4}"));
            filterBox.setValue(filter > 0 ? Integer.toString(filter) : "");
            filterBox.setResponder(value -> {
                try {
                    filter = value.isEmpty() ? 0 : Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    filter = 0;
                }
            });
            addRenderableWidget(filterBox);

            addRenderableWidget(Button.builder(Component.literal("+"), button -> changeZoom(-1))
                    .bounds(columnX, top + 74, 42, 18).build());
            addRenderableWidget(Button.builder(Component.literal("-"), button -> changeZoom(1))
                    .bounds(columnX + 46, top + 74, 42, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mcradio.map.center"), button -> {
                centerOnPlayer();
                resample();
            }).bounds(columnX, top + 96, 88, 18).build());

            resample();
            requestNodes();
        }

        private void changeZoom(int delta) {
            zoom = Mth.clamp(zoom + delta, 0, BLOCKS_PER_CELL.length - 1);
            resample();
        }

        private void requestNodes() {
            RadioNetwork.sendToServer(new MapRequestPacket());
        }

        @Override
        public void tick() {
            ticks++;
            if (ticks % 20 == 0) {
                resample();
            }
            if (ticks % 100 == 0) {
                requestNodes();
            }
        }

        // ------------------------------------------------------------------ terrain

        private void resample() {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            int scale = BLOCKS_PER_CELL[zoom];
            int half = CELLS / 2;
            int originX = (int) Math.floor(centerX);
            int originZ = (int) Math.floor(centerZ);
            for (int cz = 0; cz < CELLS; cz++) {
                for (int cx = 0; cx < CELLS; cx++) {
                    terrain[cz * CELLS + cx] = sample(level, originX + (cx - half) * scale, originZ + (cz - half) * scale);
                }
            }
        }

        private static int sample(ClientLevel level, int x, int z) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) {
                return UNKNOWN_TERRAIN;
            }
            int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            BlockPos pos = new BlockPos(x, height - 1, z);
            BlockState state = chunk.getBlockState(pos);
            MapColor color = state.getMapColor(level, pos);
            if (color == MapColor.NONE) {
                return UNKNOWN_TERRAIN;
            }
            double shade = 0.72 + Mth.clamp((height - level.getSeaLevel()) / 140.0, -0.25, 0.4);
            int red = Mth.clamp((int) (((color.col >> 16) & 0xFF) * shade), 0, 255);
            int green = Mth.clamp((int) (((color.col >> 8) & 0xFF) * shade), 0, 255);
            int blue = Mth.clamp((int) ((color.col & 0xFF) * shade), 0, 255);
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }

        // ------------------------------------------------------------------ coordinates

        private double toScreenX(double worldX) {
            return mapX + MAP_PX / 2.0 + (worldX - centerX) * CELL_PX / BLOCKS_PER_CELL[zoom];
        }

        private double toScreenY(double worldZ) {
            return mapY + MAP_PX / 2.0 + (worldZ - centerZ) * CELL_PX / BLOCKS_PER_CELL[zoom];
        }

        private double pixelsPerBlock() {
            return (double) CELL_PX / BLOCKS_PER_CELL[zoom];
        }

        private boolean insideMap(double x, double y) {
            return x >= mapX && x < mapX + MAP_PX && y >= mapY && y < mapY + MAP_PX;
        }

        // ------------------------------------------------------------------ rendering

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        private boolean visible(MapDataPacket.Node node) {
            if (filter <= 0) {
                return true;
            }
            return switch (node.kind()) {
                case MapDataPacket.KIND_REPEATER -> node.a() == filter || node.b() == filter;
                case MapDataPacket.KIND_JAMMER -> true;
                default -> node.a() == filter;
            };
        }

        private static int frequencyColor(int frequency) {
            return Mth.hsvToRgb((frequency * 37 % 360) / 360.0F, 0.65F, 1.0F);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fillGradient(0, 0, width, height, 0xB0080C12, 0xD0080C12);
            RadioUi.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, title, font);
            RadioUi.inset(graphics, mapX - 2, mapY - 2, MAP_PX + 4, MAP_PX + 4);

            graphics.enableScissor(mapX, mapY, mapX + MAP_PX, mapY + MAP_PX);
            for (int cz = 0; cz < CELLS; cz++) {
                for (int cx = 0; cx < CELLS; cx++) {
                    int x = mapX + cx * CELL_PX;
                    int y = mapY + cz * CELL_PX;
                    graphics.fill(x, y, x + CELL_PX, y + CELL_PX, terrain[cz * CELLS + cx]);
                }
            }

            List<MapDataPacket.Node> shown = new ArrayList<>();
            for (MapDataPacket.Node node : nodes) {
                if (visible(node)) {
                    shown.add(node);
                }
            }
            for (MapDataPacket.Node node : shown) {
                boolean coverage = node.kind() == MapDataPacket.KIND_DISTRIBUTOR || node.kind() == MapDataPacket.KIND_REPEATER
                        || node.kind() == MapDataPacket.KIND_JAMMER;
                if (node.active() && coverage) {
                    int color = node.kind() == MapDataPacket.KIND_JAMMER ? 0xFF5A5F
                            : frequencyColor(node.kind() == MapDataPacket.KIND_REPEATER ? node.b() : node.a());
                    drawCoverage(graphics, toScreenX(node.pos().getX() + 0.5), toScreenY(node.pos().getZ() + 0.5),
                            node.radius() * pixelsPerBlock(), color);
                }
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                double px = toScreenX(player.getX());
                double py = toScreenY(player.getZ());
                drawRing(graphics, px, py, RadioPhysics.WALKIE_RANGE * pixelsPerBlock(), 0xCCFFFFFF);
                MapMarker.draw(graphics, px, py, 0xFF4CD964, 5);
            }

            MapDataPacket.Node hovered = null;
            for (MapDataPacket.Node node : shown) {
                double sx = toScreenX(node.pos().getX() + 0.5);
                double sy = toScreenY(node.pos().getZ() + 0.5);
                int color = KIND_COLORS[Mth.clamp(node.kind(), 0, KIND_COLORS.length - 1)];
                if (!node.active()) {
                    color = (color & 0x00FFFFFF) | 0x80000000;
                }
                MapMarker.draw(graphics, sx, sy, color, node.kind() == MapDataPacket.KIND_REPEATER ? 6 : 5);
                if (insideMap(mouseX, mouseY) && Math.abs(mouseX - sx) <= 4 && Math.abs(mouseY - sy) <= 4) {
                    hovered = node;
                }
            }
            graphics.drawString(font, "N", mapX + MAP_PX / 2 - 2, mapY + 3, RadioUi.TEXT, true);
            graphics.disableScissor();

            super.render(graphics, mouseX, mouseY, partialTick);

            int columnX = left + 246;
            graphics.drawString(font, Component.translatable("gui.mcradio.map.filter"), columnX, top + 38, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.scale", BLOCKS_PER_CELL[zoom] * CELLS),
                    columnX, top + 120, RadioUi.MUTED, false);
            String[] legend = {"gui.mcradio.map.legend.distributor", "gui.mcradio.map.legend.repeater",
                    "gui.mcradio.map.legend.receiver", "gui.mcradio.map.legend.jammer"};
            for (int i = 0; i < legend.length; i++) {
                MapMarker.draw(graphics, columnX + 4, top + 146 + i * 14 + 4, KIND_COLORS[i], 5);
                graphics.drawString(font, Component.translatable(legend[i]), columnX + 14, top + 146 + i * 14, RadioUi.TEXT, false);
            }
            MapMarker.draw(graphics, columnX + 4, top + 146 + 4 * 14 + 4, 0xFF4CD964, 5);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.legend.you"), columnX + 14, top + 146 + 4 * 14, RadioUi.TEXT, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.legend.walkie", RadioPhysics.WALKIE_RANGE),
                    columnX, top + 146 + 5 * 14, RadioUi.MUTED, false);
            graphics.drawString(font, Component.translatable("gui.mcradio.map.hint"), columnX, top + 146 + 6 * 14 + 4, RadioUi.MUTED, false);

            if (hovered != null) {
                graphics.renderComponentTooltip(font, describe(hovered), mouseX, mouseY);
            }
        }

        private static List<Component> describe(MapDataPacket.Node node) {
            List<Component> lines = new ArrayList<>();
            switch (node.kind()) {
                case MapDataPacket.KIND_DISTRIBUTOR -> lines.add(Component.translatable("gui.mcradio.map.distributor", node.a(), node.radius()));
                case MapDataPacket.KIND_REPEATER -> lines.add(Component.translatable("gui.mcradio.map.repeater", node.a(), node.b(), node.radius()));
                case MapDataPacket.KIND_RECEIVER -> lines.add(Component.translatable("gui.mcradio.map.receiver", node.a()));
                default -> lines.add(Component.translatable("gui.mcradio.map.jammer", node.radius()));
            }
            lines.add(Component.literal(node.pos().getX() + ", " + node.pos().getY() + ", " + node.pos().getZ()));
            lines.add(Component.translatable(node.active() ? "gui.mcradio.map.active" : "gui.mcradio.map.inactive"));
            return lines;
        }

        /** Translucent coverage disc plus an outline. */
        private void drawCoverage(GuiGraphics graphics, double cx, double cy, double radius, int rgb) {
            int r = (int) Math.round(radius);
            if (r < 1) {
                return;
            }
            int fromY = Math.max(-r, mapY - (int) cy);
            int toY = Math.min(r, mapY + MAP_PX - (int) cy);
            for (int dy = fromY; dy <= toY; dy++) {
                int halfWidth = (int) Math.sqrt((double) r * r - (double) dy * dy);
                graphics.fill((int) cx - halfWidth, (int) cy + dy, (int) cx + halfWidth, (int) cy + dy + 1, 0x30000000 | rgb);
            }
            drawRing(graphics, cx, cy, radius, 0xFF000000 | rgb);
        }

        private void drawRing(GuiGraphics graphics, double cx, double cy, double radius, int argb) {
            int steps = (int) Mth.clamp(radius * 0.8, 24, 160);
            for (int i = 0; i < steps; i++) {
                double angle = i * Math.PI * 2.0 / steps;
                int x = (int) Math.round(cx + Math.cos(angle) * radius);
                int y = (int) Math.round(cy + Math.sin(angle) * radius);
                graphics.fill(x, y, x + 2, y + 2, argb);
            }
        }

        // ------------------------------------------------------------------ input

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == 0 && insideMap(mouseX, mouseY)) {
                centerX -= dragX / pixelsPerBlock();
                centerZ -= dragY / pixelsPerBlock();
                resample();
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (insideMap(mouseX, mouseY) && scrollY != 0) {
                changeZoom(scrollY > 0 ? -1 : 1);
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        /** Small filled square used for every device icon. */
        private static final class MapMarker {
            private MapMarker() {
            }

            static void draw(GuiGraphics graphics, double x, double y, int argb, int size) {
                int half = size / 2;
                int ix = (int) Math.round(x);
                int iy = (int) Math.round(y);
                graphics.fill(ix - half - 1, iy - half - 1, ix + half + 2, iy + half + 2, 0xFF05080C);
                graphics.fill(ix - half, iy - half, ix + half + 1, iy + half + 1, argb);
            }
        }
    }

}
