package com.filiphsandstrom.mineiago;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logging {
    private File loggingFile;

    public Logging() {
        MineiaGo instance = MineiaGo.getInstance();

        // Create plugin folder if it doesn't exist
        if (!instance.getDataFolder().exists())
            instance.getDataFolder().mkdir();

        loggingFile = new File(instance.getDataFolder(), "MineiaGo.log");
        if (!loggingFile.exists()) {
            try {
                loggingFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
    
    private void Print(String msg) {
        if(MineiaGo.getInstance().getConfig().getLoglevel() >= 3)
            return;
            
        String file_content;
        PrintWriter printer;
        try {
            printer = new PrintWriter(new FileWriter(loggingFile));
        } catch (Exception e) {
            e.printStackTrace();
            
            MineiaGo.getInstance().getLogger()
                    .warning("Failed to log message. Do we have the right directory permissions or is the disk full?");
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        String output = "[" + dateFormat.format(date) + "]" + msg + "\n";
        printer.print(output);
        printer.close();
    }

    public void Debug(String msg) {
        if (MineiaGo.getInstance().getConfig().getLoglevel() <= -1) {
            Print("[DEBUG]: " + msg);
            MineiaGo.getInstance().getLogger().fine(msg);
        }
    }

    public void Info(String msg) {
        if (MineiaGo.getInstance().getConfig().getLoglevel() <= 0) {
            Print("[INFO]: " + msg);
            MineiaGo.getInstance().getLogger().info(msg);
        }
    }

    public void Warning(String msg) {
        if (MineiaGo.getInstance().getConfig().getLoglevel() <= 1) {
            Print("[WARNING]: " + msg);
            MineiaGo.getInstance().getLogger().warning(msg);
        }
    }

    public void Fatal(String msg) {
        if (MineiaGo.getInstance().getConfig().getLoglevel() <= 2) {
            Print("[FATAL]: " + msg);
            MineiaGo.getInstance().getLogger().severe(msg);
        }
    }
}
