package com.yuxuan66.bot;

import com.yuxuan66.bot.support.YuxuanBot;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;
import org.jetbrains.annotations.NotNull;

public class CoreBot {

    public static void main(String[] args) {

        YuxuanBot yuxuanBot = YuxuanBot.getInstance();

        yuxuanBot.getBot().login();

        yuxuanBot.getBot().join();
    }
}
