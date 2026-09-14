package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.model.dto.chat.ChatHistoryVO;
import com.aicoding.service.AppService;
import com.aicoding.service.ChatHistoryService;
import com.aicoding.service.impl.AiCodeGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话与 AI 生成接口（SSE）
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiCodeGenerationService aiCodeGenerationService;
    private final ChatHistoryService chatHistoryService;
    private final AppService appService;

    /**
     * 对话生成（SSE 流式）：事件流 router / delta / tool / done / error / ping
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatToGenerate(@RequestParam long appId, @RequestParam String message) {
        return aiCodeGenerationService.chatToGenerate(appId, message, UserContext.get());
    }

    /**
     * 对话历史（游标分页，cursor 为上一页最早一条消息的 createTime）
     */
    @GetMapping("/history")
    public BaseResponse<List<ChatHistoryVO>> listHistory(@RequestParam long appId,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        appService.checkAppOwner(appService.getAppById(appId), UserContext.get());
        return ResultUtils.success(chatHistoryService.listByCursor(appId, cursor, pageSize));
    }
}
