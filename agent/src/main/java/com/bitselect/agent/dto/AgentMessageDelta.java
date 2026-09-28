

package com.bitselect.agent.dto;

/**
 * message 事件载荷：type 取 response / think / error，error 是中断提示不是模型输出
 */
public record AgentMessageDelta(String type, String delta) {
}
