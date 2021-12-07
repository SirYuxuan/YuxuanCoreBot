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
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yuxuan66.bot.entity.BotMessage;
import com.yuxuan66.bot.entity.BotMessageData;
import com.yuxuan66.bot.entity.BotMsgType;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
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


    private static Object lock = new Object();
    private static  Boolean isStartCheck = false;
    private static List<Long> checkGroups = new ArrayList<>();

    static {
        checkGroups.add(797215188L);
    }

    public static void distribute(BotOnlineEvent event) {
        synchronized (lock) {
            if (!isStartCheck) {
                System.out.println("定时器已启动");
                System.out.println("开始获取军团QQ");
                List<String> allQQ = JSON.parseObject(HttpUtil.get("http://115.29.203.165:10002/corp/getAllQQ")).getJSONArray("data").toJavaList(String.class);
                System.out.println("军团QQ获取完毕：" + allQQ.size()+"人");
                List<Long> re = new ArrayList<>();
                for (Long checkGroup : checkGroups) {
                    Group group = event.getBot().getGroup(checkGroup);
                    assert group != null;

                    for (NormalMember member : group.getMembers()) {
                        if (!allQQ.contains(Convert.toStr(member.getId()))) {
                            // 踢掉此成员
                          re.add(member.getId());
                        }
                    }
                }
                System.out.println(JSON.toJSONString(re));
                CronUtil.setMatchSecond(true);
                CronUtil.start();
                isStartCheck = true;

            }
        }

    }


}
