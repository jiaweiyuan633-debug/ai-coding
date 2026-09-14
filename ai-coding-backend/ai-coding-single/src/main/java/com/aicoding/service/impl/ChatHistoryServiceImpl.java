package com.aicoding.service.impl;

import com.aicoding.mapper.ChatHistoryMapper;
import com.aicoding.model.dto.chat.ChatHistoryVO;
import com.aicoding.model.entity.ChatHistory;
import com.aicoding.model.enums.MessageTypeEnum;
import com.aicoding.service.ChatHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 对话历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private static final DateTimeFormatter CURSOR_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public void addChatMessage(long appId, long userId, String message, MessageTypeEnum type) {
        ChatHistory history = new ChatHistory();
        history.setAppId(appId);
        history.setUserId(userId);
        history.setMessage(message);
        history.setMessageType(type.getValue());
        chatHistoryMapper.insert(history);
    }

    @Override
    public List<ChatHistoryVO> listRecentByApp(long appId, int lastN) {
        List<ChatHistory> records = chatHistoryMapper.selectList(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getAppId, appId)
                .orderByDesc(ChatHistory::getCreateTime)
                .last("limit " + lastN));
        return records.reversed().stream().map(this::toVO).toList();
    }

    @Override
    public List<ChatHistoryVO> listByCursor(long appId, String cursor, int pageSize) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getAppId, appId)
                .orderByDesc(ChatHistory::getCreateTime)
                .last("limit " + Math.min(pageSize, 50));
        if (cursor != null && !cursor.isBlank()) {
            wrapper.lt(ChatHistory::getCreateTime, LocalDateTime.parse(cursor, CURSOR_FORMAT));
        }
        return chatHistoryMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    private ChatHistoryVO toVO(ChatHistory history) {
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }
}
