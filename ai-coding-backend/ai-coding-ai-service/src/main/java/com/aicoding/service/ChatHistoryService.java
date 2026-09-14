package com.aicoding.service;

import com.aicoding.model.dto.chat.ChatHistoryVO;
import com.aicoding.model.enums.MessageTypeEnum;

import java.util.List;

/**
 * 对话历史服务
 */
public interface ChatHistoryService {

    void addChatMessage(long appId, long userId, String message, MessageTypeEnum type);

    /**
     * 加载应用最近 lastN 条对话（升序，用于重建 AI 上下文）
     */
    List<ChatHistoryVO> listRecentByApp(long appId, int lastN);

    /**
     * 游标分页查询（createTime 倒序，cursor 为上一页最后一条的 createTime）
     */
    List<ChatHistoryVO> listByCursor(long appId, String cursor, int pageSize);
}
