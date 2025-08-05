package com.gg.SaltDiscordPlugin;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import org.pf4j.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class MainPlugin extends Plugin {
    @Override
    public void start() {
        super.start();
        DiscordRichPresence.getInstance().initialize();
//        steamAPI();
    }

    private void steamAPI() {
        new Thread(() -> {
            try {
                SteamAPI.loadLibraries();
                int appId = 3009140;
                boolean needRestartA = SteamApiJNA.INSTANCE.SteamAPI_RestartAppIfNecessary(appId);
                boolean needRestartB = SteamApi4JJNA.INSTANCE.Java_com_codedisaster_steamworks_SteamAPI_nativeRestartAppIfNecessary(appId);

//                Class<SteamAPI> steamAPIClass = SteamAPI.class;
//                Method restartMethod = steamAPIClass.getDeclaredMethod("nativeRestartAppIfNecessary", int.class);
//                restartMethod.setAccessible(true);
//                boolean needRestartC = (boolean) restartMethod.invoke(null, 3009140);

                boolean needRestartC = SteamAPI.restartAppIfNecessary(appId);


                Thread.sleep(3000);
                File file = new File("C:/Users/zil07/Desktop/test.txt");
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.append("Time : " + new Date().toString() + "\n");
//                fileWriter.append("Need restart: " + String.valueOf(needRestart));
                fileWriter.append("SteamWorks API : " + needRestartA + "\n");
                fileWriter.append("SteamWorks4J DLL: " + needRestartB + "\n");
                fileWriter.append("SteamWorks4J API: " + needRestartC + "\n");
//                fileWriter.append("JNI API: " + needRestartD + "\n");
                fileWriter.flush();
                fileWriter.close();


                // 初始化Steam API
//                if (!SteamApiJNA.INSTANCE.SteamAPI_Init()) {
//                    System.out.println("Failed to initialize Steam API");
//                    return;
//                }
//
//                // 检查Steam是否运行
//                while (!SteamApiJNA.INSTANCE.SteamAPI_IsSteamRunning()) {
//                    Thread.sleep(5000);
//                    System.out.println("Waiting for Steam to be running...");
//                }

                System.out.println("Steam is running and API initialized successfully");

            } catch (IOException | SteamException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}