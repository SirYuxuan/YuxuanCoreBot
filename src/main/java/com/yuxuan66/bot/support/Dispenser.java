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

    private static List<Long> checkGroups = new ArrayList<>();

    // 军团的QQ群
    static {
        checkGroups.add(822397335L);
        checkGroups.add(797215188L);
    }

    public static final String BASE_URL = "http://api.hd-eve.com/";

    private static final String URL = BASE_URL + "botApi/dispenser";

    /**
     * 机器人消息对象转换为可读的
     *
     * @param messages 消息
     * @return 转换后
     */
    private static List<BotMessageData> messageConvert(MessageChain messages) {

        List<BotMessageData> messageDataList = new ArrayList<>();

        for (SingleMessage singleMessage : messages) {

            if (OnlineMessageSource.class.isAssignableFrom(singleMessage.getClass())) {
                continue;
            }
            // 文本消息
            if (PlainText.class.isAssignableFrom(singleMessage.getClass())) {
                if (StrUtil.isBlank(singleMessage.contentToString())) {
                    continue;
                }
                BotMessageData botMessageData = new BotMessageData();
                botMessageData.setType(0);
                botMessageData.setMsg(singleMessage.contentToString());
                messageDataList.add(botMessageData);
            }

            if (Image.class.isAssignableFrom(singleMessage.getClass())) {
                BotMessageData botMessageData = new BotMessageData();
                botMessageData.setType(1);
                botMessageData.setMsg(Image.queryUrl((Image) singleMessage));
                messageDataList.add(botMessageData);
            }

            if (At.class.isAssignableFrom(singleMessage.getClass())) {
                BotMessageData botMessageData = new BotMessageData();
                botMessageData.setType(2);
                botMessageData.setMsg(Convert.toStr(((At) singleMessage).getTarget()));
                messageDataList.add(botMessageData);
            }

            if (AtAll.class.isAssignableFrom(singleMessage.getClass())) {
                BotMessageData botMessageData = new BotMessageData();
                botMessageData.setType(3);
                messageDataList.add(botMessageData);
            }

        }
        return messageDataList;
    }


    /**
     * 群消息
     *
     * @param event 群消息事件
     */
    public static void distribute(GroupMessageEvent event) {


        List<Long> blacklist = new ArrayList<>();
        blacklist.add(352355075L);



        BotMessage botMessage = new BotMessage();
        botMessage.setGroup(event.getGroup().getId());
        botMessage.setQq(event.getSender().getId());
        botMessage.setMessageDataList(messageConvert(event.getMessage()));

        String body = "";
        try {
            body = HttpUtil.post(URL, JSON.toJSONString(botMessage));
        } catch (Exception e) {
        }


        JSONObject data = JSONObject.parseObject(body);

        if (data.containsKey("data")) {
            JSONObject apiResultInfo = data.getJSONObject("data");

            if (apiResultInfo == null) {
                return;
            }

            List<BotMessageData> messageDataList = apiResultInfo.getJSONArray("messageDataList").toJavaList(BotMessageData.class);

            MessageChain messageChain = MessageUtils.newChain();

            // 构建消息链子
            for (BotMessageData botMessageData : messageDataList) {
                if (botMessageData.getType() == BotMsgType.TEXT) {
                    messageChain = messageChain.plus(botMessageData.getMsg());
                } else if (botMessageData.getType() == BotMsgType.AT) {
                    messageChain = messageChain.plus(new At(Convert.toLong(botMessageData.getMsg())));
                } else if (botMessageData.getType() == BotMsgType.AT_ALL) {
                    messageChain = messageChain.plus(AtAll.INSTANCE);
                } else if (botMessageData.getType() == BotMsgType.IMG) {
                    byte[] bytes = HttpUtil.downloadBytes(botMessageData.getMsg());
                    if(new String(bytes, Charset.defaultCharset()).contains("code")){
                        JSONObject jsonObject = JSON.parseObject(new String(bytes, Charset.defaultCharset()));
                        messageChain = messageChain.plus(jsonObject.get("msg").toString());
                    }else{
                        Image image = event.getGroup().uploadImage(ExternalResource.create(bytes));
                        messageChain = messageChain.plus(image);
                    }

                }
            }

            event.getGroup().sendMessage(messageChain);

        }


    }


    /**
     * 好友消息
     *
     * @param event 好友消息事件
     */
    public static void distribute(FriendMessageEvent event) {

    }

    /**
     * 群临时消息
     *
     * @param event 临时消息事件
     */
    public static void distribute(GroupTempMessageEvent event) {

    }

    /**
     * 申请加群消息
     *
     * @param event 申请加群消息事件
     */
    public static void distribute(MemberJoinRequestEvent event) {

        // 指定群在进行校验
        if (checkGroups.contains(event.getGroupId())) {
            JSONObject data = JSON.parseObject(HttpUtil.get(BASE_URL + "corp/checkQQDoesItExist/" + event.getFromId()));
            boolean isExist = data.getBoolean("data");
            if (isExist) {
                event.accept();
            } else {
                event.reject(false, "您还尚未注册军团网站，无法加入此群");
            }
        }
    }

    private static Boolean isStartCheck = false;

    private static final Object lock = new Object();

    /**
     * 机器人人上线，启动一个线程用来定时检测群员是否存在与军团系统
     *
     * @param event
     */
    public static void distribute(BotOnlineEvent event) {
        synchronized (lock) {
            if (!isStartCheck) {
              /*  CronUtil.setMatchSecond(true);
                CronUtil.schedule("0 0/30 0/1 * * ?", new Task() {
                    @Override
                    public void execute() {
                        List<String> allQQ = JSON.parseObject(HttpUtil.get(BASE_URL + "corp/getAllQQ")).getJSONArray("data").toJavaList(String.class);

                        for (Long checkGroup : checkGroups) {
                            Group group = event.getBot().getGroup(checkGroup);
                            assert group != null;

                            for (NormalMember member : group.getMembers()) {
                                if (!allQQ.contains(Convert.toStr(member.getId()))) {
                                    // 踢掉此成员
                                    member.kick("对不起，您已经离开军团，或军团系统授权失效，请联系管理或重新授权系统后加入");
                                }
                            }
                        }
                    }
                });
                CronUtil.start();
                isStartCheck = true;*/

            }
        }

    }


}
