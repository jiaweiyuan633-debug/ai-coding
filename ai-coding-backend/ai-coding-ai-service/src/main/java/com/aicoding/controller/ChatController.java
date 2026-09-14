package com.aicoding.controller;

import com.aicoding.api.AppRpcService;
import com.aicoding.common.BaseResponse;
import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.model.dto.chat.ChatHistoryVO;
import com.aicoding.model.entity.User;
import com.aicoding.service.ChatHistoryService;
import com.aicoding.service.impl.AiCodeGenerationService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话与 AI 生成接口（SSE，微服务版：应用归属校验走 Dubbo）
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiCodeGenerationService aiCodeGenerationService;
    private final ChatHistoryService chatHistoryService;

    @DubboReference(check = false)
    private AppRpcService appRpcService;

    /**
     * 对话生成（SSE 流式）：事件流 router / workflow / delta / tool / done / error / ping
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatToGenerate(@RequestParam long appId, @RequestParam String message) {
        return aiCodeGenerationService.chatToGenerate(appId, message, UserContext.get());
    }

    /**
     * 对话历史（游标分页）
     */
    @GetMapping("/history")
    public BaseResponse<List<ChatHistoryVO>> listHistory(@RequestParam long appId,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        User user = UserContext.get();
        if (appRpcService.getApp(appId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        boolean admin = "admin".equals(user.getUserRole());
        if (!admin && !appRpcService.isOwner(appId, user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该应用");
        }
        return ResultUtils.success(chatHistoryService.listByCursor(appId, cursor, pageSize));
    }
}
