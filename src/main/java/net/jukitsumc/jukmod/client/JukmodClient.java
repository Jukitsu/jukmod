package net.jukitsumc.jukmod.client;

import net.fabricmc.api.ClientModInitializer;

public class JukmodClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JukmodHUD.register();
    }
}
