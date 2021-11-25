/*
 * Copyright 2013-2021 Sir丶雨轩
 *
 * This file is part of Sir丶雨轩/eve-corp-api.

 * Sir丶雨轩/eve-corp-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.

 * Sir丶雨轩/eve-corp-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Sir丶雨轩/eve-corp-api.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.yuxuan66.bot.support;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yuxuan66.bot.entity.BotMessage;
import com.yuxuan66.bot.entity.BotMessageData;
import com.yuxuan66.bot.entity.BotMsgType;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 机器人消息分发器
 *
 * @author Sir丶雨轩
 * @since 2021/7/30
 */
public class Dispenser {


    /**
     * 群消息
     *
     * @param event 群消息事件
     */
    public static void distribute(GroupMessageEvent event) {

        if (event.getGroup().getId() == 155057693 || event.getMessage().contentToString().startsWith("YXTEST::")) {
            String result = "消息来至：《" + event.getGroup().getName() + "》(" + event.getGroup().getId() + ")\r\n发送人：" + event.getSenderName() + "(" + event.getSender().getId() + ")\r\n" + "消息内容：\r\n";
            MessageChain messages = MessageUtils.newChain();
            messages = messages.plus(result).plus(event.getMessage());
            event.getBot().getGroup(797215188L).sendMessage(messages);
            event.getBot().getGroup(143477610L).sendMessage(messages);
            return;
        }




    }



}
