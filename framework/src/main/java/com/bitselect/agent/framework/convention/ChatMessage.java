

package com.bitselect.agent.framework.convention;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话消息实体
 *
 * <p>
 * 用于统一抽象「大模型对话」中的一条消息，包含角色和消息内容：
 * <ul>
 *   <li>{@link Role#SYSTEM}：系统提示词，用于为大模型设定行为、规则</li>
 *   <li>{@link Role#USER}：用户输入消息</li>
 *   <li>{@link Role#ASSISTANT}：大模型（助手）回复内容</li>
 * </ul>
 * 该结构适合在不同模型/厂商之间做一层通用抽象
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息角色类型
     */
    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT;

        public static Role fromString(String value) {
            for (Role role : Role.values()) {
                if (role.name().equalsIgnoreCase(value)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("无效的角色类型: " + value);
        }
    }

    /**
     * 消息结束状态
     */
    public enum MessageStatus {
        NORMAL,
        INTERRUPTED,
        REJECTED
    }

    /**
     * 当前消息的角色（系统 / 用户 / 助手）
     */
    private Role role;

    /**
     * 消息的具体文本内容
     */
    private String content;

    /**
     * 深度思考内容（仅 ASSISTANT 角色可能携带）
     */
    private String thinkingContent;

    /**
     * 深度思考耗时（秒，仅 ASSISTANT 角色可能携带）
     */
    private Integer thinkingDuration;

    /**
     * 回答来源（文档级来源列表，仅 ASSISTANT 角色可能携带）
     */
    private List<SourceRef> sources;

    /**
     * 推荐问题 grounding 片段（仅 ASSISTANT 角色可能携带，随消息落库供推荐追问生成 grounding，不参与模型上下文）
     */
    private List<GroundingChunk> retrievedChunks;

    /**
     * 当前助手消息对应的用户消息 ID
     */
    private String replyToMessageId;

    /**
     * 消息结束状态
     */
    private MessageStatus messageStatus = MessageStatus.NORMAL;

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }

    public static ChatMessage assistant(String content, String thinkingContent) {
        return assistant(content, thinkingContent, null);
    }

    public static ChatMessage assistant(String content, String thinkingContent, Integer thinkingDuration) {
        ChatMessage message = new ChatMessage(Role.ASSISTANT, content);
        message.setThinkingContent(thinkingContent);
        message.setThinkingDuration(thinkingDuration);
        return message;
    }
}