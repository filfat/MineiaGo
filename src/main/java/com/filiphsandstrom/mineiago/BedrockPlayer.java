package com.filiphsandstrom.mineiago;

import java.net.Proxy;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nukkitx.protocol.bedrock.BedrockServerSession;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ServerPing.PlayerInfo;

public class BedrockPlayer {
    public BedrockPlayer() {
        super();
    }

    public BedrockPlayer(BedrockServerSession bedrock_session) {
        super();

        setBedrockSession(bedrock_session);

        // TODO: offline mode, direct connect to child server if enabled.
    }

    public void setChainData(String chain) {
        // FIXME
        MineiaGo.getInstance().getLogger().warning("chainData validation is not implemented!");
        return;

        JsonObject data = new JsonParser().parse(chain).getAsJsonObject().get("extraData").getAsJsonObject();
        if(!data.isJsonObject())
            return;

        player = new PlayerInfo(data.get("displayName").getAsString(), data.get("identity").getAsString());

        MineiaGo.getInstance().getLogger().warning("Player:" + player.getName());
    }

    @Getter
    private PlayerInfo player;

    @Getter
    @Setter
    private BedrockServerSession bedrock_session;

    @Getter
    @Setter
    private Session java_session;

    @Getter
    @Setter
    private String username = "";

    @Getter
    @Setter
    private String password = "";

    // FIXME: only run once we've gotten auth details from client
    public void createJavaClient() {
        try {
            MinecraftProtocol protocol = new MinecraftProtocol(username, password);
            Client client = new Client("0.0.0.0", 25565, protocol, new TcpSessionFactory(Proxy.NO_PROXY));
            setJavaSession(client.getSession());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAuthenticated() {
        return (!password.isEmpty() && !username.isEmpty());
    }
}
