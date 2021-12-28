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
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
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
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

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
        if (event.getGroup().getId() == 1007805049L || event.getGroup().getId() == 822397335L) {
            return;
        }

        if (event.getMessage().contentToString().contains("有人收") || event.getMessage().contentToString().contains("出点")) {
            String result = "消息来至：《" + event.getGroup().getName() + "》(" + event.getGroup().getId() + ")\r\n发送人：" + event.getSenderName() + "(" + event.getSender().getId() + ")\r\n" + "消息内容：\r\n";
            MessageChain messages = MessageUtils.newChain();
            messages = messages.plus(result).plus(event.getMessage());
            Objects.requireNonNull(event.getBot().getGroup(726098712L)).sendMessage(messages);
            return;

        }


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
                    if (new String(bytes, Charset.defaultCharset()).contains("code")) {
                        JSONObject jsonObject = JSON.parseObject(new String(bytes, Charset.defaultCharset()));
                        messageChain = messageChain.plus(jsonObject.get("msg").toString());
                    } else {
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


        if (event.getMessage().contentToString().startsWith("混沌Push")) {
            MessageChain messages = MessageUtils.newChain();

            for (SingleMessage item : event.getMessage()) {
                if (item.contentToString().startsWith("混沌Push@")) {
                    messages = messages.plus(AtAll.INSTANCE).plus(item.contentToString().replace("混沌Push@", " "));
                } else if (item.contentToString().startsWith("混沌Push")) {
                    messages = messages.plus(item.contentToString().replace("混沌Push", " "));
                } else {
                    if (!(item instanceof Image)) {
                        messages = messages.plus(item);
                    } else {
                        messages = messages.plus("【图片】");
                    }

                }

            }
            // 工具人群
            List<Long> bL = new ArrayList<>();
            // 群发白名单
            bL.add(1718018032L);
            bL.add(303968745L);
            bL.add(642884652L);
            bL.add(437310811L);
            bL.add(2998461262L);
            if (bL.contains(event.getSender().getId())) {
                event.getBot().getGroup(822397335L).sendMessage(messages);
                // 市场群
                event.getBot().getGroup(985570381L).sendMessage(messages);
                // 大群
                event.getBot().getGroup(1007805049L).sendMessage(messages);
                System.out.println(event.getSender().getId());
                if (Convert.toInt(event.getSender().getId()) == 1718018032) {
                    // 鱼老板
                    event.getBot().getGroup(224563313L).sendMessage(messages);
                    // LSP
                    event.getBot().getGroup(143477610L).sendMessage(messages);
                } else {
                    // 季节群
                    event.getBot().getGroup(797215188L).sendMessage(messages);
                }
            }
            return;
        }


        List<Long> blacklist = new ArrayList<>();
        blacklist.add(352355075L);


        BotMessage botMessage = new BotMessage();
        botMessage.setQq(event.getSender().getId());
        botMessage.setMessageDataList(messageConvert(event.getMessage()));
        botMessage.setGroup(0L);
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
                } else if (botMessageData.getType() == BotMsgType.IMG) {
                    byte[] bytes = HttpUtil.downloadBytes(botMessageData.getMsg());
                    if (new String(bytes, Charset.defaultCharset()).contains("code")) {
                        JSONObject jsonObject = JSON.parseObject(new String(bytes, Charset.defaultCharset()));
                        messageChain = messageChain.plus(jsonObject.get("msg").toString());
                    } else {
                        Image image = event.getSender().uploadImage(ExternalResource.create(bytes));
                        messageChain = messageChain.plus(image);
                    }

                }
            }

            event.getSender().sendMessage(messageChain);

        }


    }

    /**
     * 群临时消息
     *
     * @param event 临时消息事件
     */
    public static void distribute(GroupTempMessageEvent event) {
        List<Long> blacklist = new ArrayList<>();
        blacklist.add(352355075L);


        BotMessage botMessage = new BotMessage();
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
                } else if (botMessageData.getType() == BotMsgType.IMG) {
                    byte[] bytes = HttpUtil.downloadBytes(botMessageData.getMsg());
                    if (new String(bytes, Charset.defaultCharset()).contains("code")) {
                        JSONObject jsonObject = JSON.parseObject(new String(bytes, Charset.defaultCharset()));
                        messageChain = messageChain.plus(jsonObject.get("msg").toString());
                    } else {
                        Image image = event.getBot().getGroup(event.getGroup().getId()).uploadImage(ExternalResource.create(bytes));
                        messageChain = messageChain.plus(image);
                    }

                }
            }

            event.getSender().sendMessage(messageChain);

        }
    }

    /**
     * 申请加群消息
     *
     * @param event 申请加群消息事件
     */
    public static void distribute(MemberJoinRequestEvent event) {
        List<Long> checkGroups111 = new ArrayList<>();
        checkGroups111.addAll(checkGroups);
        checkGroups111.add(822397335L);
        // 指定群在进行校验
        if (checkGroups111.contains(event.getGroupId())) {
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
                System.out.println("定时器已启动");
                System.out.println("开始获取军团QQ");
                List<String> allQQ = JSON.parseObject(HttpUtil.get("http://115.29.203.165:10002/corp/getAllQQ")).getJSONArray("data").toJavaList(String.class);
                System.out.println("军团QQ获取完毕：" + allQQ.size() + "人");
                for (Long checkGroup : checkGroups) {
                    Group group = event.getBot().getGroup(checkGroup);
                    assert group != null;

                    for (NormalMember member : group.getMembers()) {
                        if (!allQQ.contains(Convert.toStr(member.getId()))) {
                            // 踢掉此成员

                            try {
                                Thread.sleep(10000);
                                member.kick("对不起，您已经离开军团，或军团系统授权失效，请在user.hd-eve.com完成注册并绑定QQ", false);
                            } catch (Exception e) {
                                System.out.println("踢人失败，QQ:" + member.getId() + "====>" + e.getMessage());
                            }
                        }
                    }
                }
                CronUtil.setMatchSecond(true);
                CronUtil.schedule("0 0/30 0/1 * * ?", new Task() {
                    @Override
                    public void execute() {
                        System.out.println("开始获取军团QQ");
                        List<String> allQQ = JSON.parseObject(HttpUtil.get("http://115.29.203.165:10002/corp/getAllQQ")).getJSONArray("data").toJavaList(String.class);
                        System.out.println("军团QQ获取完毕：" + allQQ.size() + "人");
                        for (Long checkGroup : checkGroups) {
                            Group group = event.getBot().getGroup(checkGroup);
                            assert group != null;

                            for (NormalMember member : group.getMembers()) {
                                if (!allQQ.contains(Convert.toStr(member.getId()))) {
                                    // 踢掉此成员
                                    try {
                                        Thread.sleep(10000);
                                        member.kick("对不起，您已经离开军团，或军团系统授权失效，请在user.hd-eve.com完成注册并绑定QQ");
                                    } catch (Exception e) {
                                        System.out.println("踢人失败，QQ:" + member.getId() + "====>" + e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                });
                CronUtil.start();
                isStartCheck = true;

            }
        }

    }


}
