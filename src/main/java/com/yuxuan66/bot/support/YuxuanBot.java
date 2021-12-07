package com.yuxuan66.bot.support;

import cn.hutool.core.lang.Singleton;
import cn.hutool.setting.Setting;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.utils.BotConfiguration;

/**
 * 雨轩机器人系统
 *
 * @author Sir丶雨轩
 * @since 2021/11/17
 */
public class YuxuanBot {

    private final Bot bot;

    private YuxuanBot() {

        Setting setting = new Setting("config.setting");

        bot = BotFactory.INSTANCE.newBot(setting.getLong("user"), setting.getStr("pass"), botConfiguration -> {
            botConfiguration.fileBasedDeviceInfo("info" + setting.getLong("user") + ".json");
            botConfiguration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_PAD);
        });

        // 注册机器人事件

        bot.getEventChannel().subscribeAlways(GroupMessageEvent.class, Dispenser::distribute);
        bot.getEventChannel().subscribeAlways(FriendMessageEvent.class, Dispenser::distribute);
        bot.getEventChannel().subscribeAlways(GroupTempMessageEvent.class, Dispenser::distribute);
        bot.getEventChannel().subscribeAlways(MemberJoinRequestEvent.class, Dispenser::distribute);
        bot.getEventChannel().subscribeAlways(BotOnlineEvent.class, Dispenser::distribute);
    }

    public static YuxuanBot getInstance(){
        return Singleton.get(YuxuanBot.class);
    }

    public Bot getBot() {
        return this.bot;
    }
}
