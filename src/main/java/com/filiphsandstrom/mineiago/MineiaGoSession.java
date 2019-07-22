package com.filiphsandstrom.mineiago;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.filiphsandstrom.mineiago.*;
import com.filiphsandstrom.mineiago.packets.JavaPackets;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nukkitx.protocol.bedrock.BedrockServerSession;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class MineiaGoSession {
    public static final String MOJANG_PUBLIC_KEY_BASE64 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V";
    public static PublicKey MOJANG_PUBLIC_KEY;
    static {
        try {
            MOJANG_PUBLIC_KEY = KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(MOJANG_PUBLIC_KEY_BASE64)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MineiaGoSession(BedrockServerSession bedrock_session) {
        setBedrockSession(bedrock_session);
    }

    public void onDisconnect(String reason) {
        // FIXME
        MineiaGo.getInstance().getLogging().Info("Player " + getPlayerInfo().getUsername() + " left (" + reason + ")!");
        return;
    }

    public void setChainData(String chain_token) {
        JWSObject jws;
        JWSVerifier verifier;

        JsonArray chain = new JsonParser().parse(chain_token).getAsJsonObject().get("chain").getAsJsonArray();
        for (JsonElement chain_element : chain) {
            String token = chain_element.getAsString();

            try {
                jws = JWSObject.parse(token);
                verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jws.getHeader(), MOJANG_PUBLIC_KEY);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            try {
                JsonObject data = new JsonParser().parse(jws.getPayload().toString()).getAsJsonObject().get("extraData")
                        .getAsJsonObject();
                MineiaGo.getInstance().getLogging().Debug(data.toString());

                if (playerInfo == null)
                    playerInfo = new BedrockPlayerInfo(data.get("displayName").getAsString(),
                            data.get("identity").getAsString(), data.get("XUID").getAsString());
            } catch (Exception e) {
                MineiaGo.getInstance().getLogging().Debug(jws.getPayload().toString());
            }
        }

        MineiaGo instance = MineiaGo.getInstance();
        if (!instance.getDataFolder().toPath().resolve("players").toFile().exists())
            instance.getDataFolder().toPath().resolve("players").toFile().mkdir();

        File playerFile = new File(instance.getDataFolder().toPath().resolve("players").toFile(),
                getPlayerInfo().getXuid() + ".yml");
        if (!playerFile.exists()) {
            try (InputStream in = instance.getResourceAsStream("player.yml")) {
                Files.copy(in, playerFile.toPath());
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(playerFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        config.set("username", getPlayerInfo().getUsername());
        config.set("identity", getPlayerInfo().getUuid());
        config.set("xbox_identity", getPlayerInfo().getXuid());
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, playerFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        MineiaGo.getInstance().getLogging().Info("Player " + getPlayerInfo().getUsername() + " connected!");
        createJavaClient();
    }

    @Getter
    private BedrockPlayerInfo playerInfo;

    @Getter
    @Setter
    private BedrockServerSession bedrockSession;

    @Getter
    @Setter
    private Session javaSession;

    public void createJavaClient() {
        MineiaGoSession session = this;

        // FIXME this should probably be handled completely differently!
        // Maybe we should even directly send packets ourself since the sever will be in
        // offlinemode anyways.
        try {
            JavaPackets packets = new JavaPackets();
            MinecraftProtocol protocol;
            Client client;

            // FIXME: get default serveer
            InetSocketAddress address = ProxyServer.getInstance().getConfig().getServers().get(ProxyServer.getInstance()
                    .getConfig().getListeners().stream().findFirst().orElseGet(null).getDefaultServer()).getAddress();

            try {
                protocol = new MinecraftProtocol(getPlayerInfo().getUsername());
                client = new Client(address.getHostName(), address.getPort(), protocol,
                        new TcpSessionFactory(Proxy.NO_PROXY));
            } catch (Exception e) {
                bedrockSession.disconnect(e.getMessage());
                return;
            }

            MineiaGo.getInstance().getLogging()
                    .Debug("[" + bedrockSession.getAddress() + "] Connecting to main server...");

            try {
                client.getSession().connect();
            } catch (Exception e) {
                bedrockSession.disconnect(e.getMessage());
                return;
            }

            client.getSession().addListener(new SessionAdapter() {
                @Override
                public void packetReceived(PacketReceivedEvent event) {
                    // TODO
                    // MineiaGo.getInstance().getLogging().Debug("[JAVA] " + event.getPacket().toString());
                    packets.handlePacket(event.getPacket().getClass().getSimpleName(), event.getPacket(), session);
                }

                @Override
                public void disconnected(DisconnectedEvent event) {
                    bedrockSession.disconnect(event.getReason());
                }

                @Override
                public void connected(ConnectedEvent event) {
                    setJavaSession(event.getSession());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
