package com.yuxuan66.bot;

import cn.hutool.core.convert.Convert;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.yuxuan66.bot.support.YuxuanBot;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.utils.BotConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CoreBot {

    public static void main(String[] args) {

        YuxuanBot yuxuanBot = YuxuanBot.getInstance();

        yuxuanBot.getBot().login();


        yuxuanBot.getBot().join();
    }
}
