package com.bitselect.agent.rag.core.prompt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.bitselect.agent.rag.dao.entity.AgentProfileDO;
import com.bitselect.agent.rag.dao.entity.AgentPromptDO;
import com.bitselect.agent.rag.dao.mapper.AgentProfileMapper;
import com.bitselect.agent.rag.dao.mapper.AgentPromptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】智能体提示词解析器：解析"激活智能体的槽位，空白回落内置智能体"。
 *
 * 【为什么存在】
 * - 面向终端用户的提示词一律从此处读取，保证回落逻辑唯一
 * - 先铺内置为基线，再让激活智能体的非空槽位覆盖，实现回落
 *
 * 【被谁引用】RAGPromptService / ConversationTitleGenerator / RecommendedQuestionGenerator
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPromptResolver {

    private final AgentProfileMapper agentProfileMapper;
    private final AgentPromptMapper agentPromptMapper;
    private final AgentPromptCacheManager cacheManager;

    /**
     * 【方法用途】解析指定槽位的最终提示词，内置智能体也没配时返回空串。
     */
    public String resolve(AgentPromptSlot slot) {
        if (slot == null) {
            return "";
        }
        return StrUtil.emptyIfNull(resolveAll().get(slot.name()));
    }

    /**
     * 【方法用途】填充占位符并清理格式，语义与 PromptTemplateLoader#render 一致。
     */
    public String render(AgentPromptSlot slot, Map<String, String> slots) {
        return PromptTemplateUtils.cleanupPrompt(PromptTemplateUtils.fillSlots(resolve(slot), slots));
    }

    /**
     * 【方法用途】返回全部槽位的最终生效内容，缺失的槽位不出现在 map 中。
     */
    public Map<String, String> resolveAll() {
        Map<String, String> cached = cacheManager.getFromCache();
        if (cached != null) {
            return cached;
        }
        Map<String, String> resolved = loadFromDb();
        cacheManager.saveToCache(resolved);
        return resolved;
    }

    /**
     * 【方法用途】读取某个智能体自身配置的槽位，不做回落，供控制台编辑态展示。
     */
    public Map<String, String> loadOwnPrompts(String agentId) {
        Map<String, String> own = new HashMap<>();
        if (StrUtil.isBlank(agentId)) {
            return own;
        }
        List<AgentPromptDO> prompts = agentPromptMapper.selectList(
                Wrappers.lambdaQuery(AgentPromptDO.class).eq(AgentPromptDO::getAgentId, agentId));
        for (AgentPromptDO prompt : prompts) {
            own.put(prompt.getSlotKey(), StrUtil.emptyIfNull(prompt.getContent()));
        }
        return own;
    }

    private Map<String, String> loadFromDb() {
        AgentProfileDO builtin = firstByFlag(AgentProfileDO::getBuiltin);
        if (builtin == null) {
            log.warn("未找到内置智能体，空槽位将无提示词可回落");
        }

        Map<String, String> resolved = new HashMap<>();
        putNonBlank(resolved, builtin);
        putNonBlank(resolved, firstByFlag(AgentProfileDO::getActive));
        return resolved;
    }

    private AgentProfileDO firstByFlag(SFunction<AgentProfileDO, Integer> flag) {
        List<AgentProfileDO> profiles = agentProfileMapper.selectList(
                Wrappers.lambdaQuery(AgentProfileDO.class)
                        .eq(flag, 1)
                        .orderByAsc(AgentProfileDO::getCreateTime)
                        .orderByAsc(AgentProfileDO::getId));
        return CollUtil.isEmpty(profiles) ? null : profiles.get(0);
    }

    /**
     * 后写入者覆盖前者，空白内容不参与覆盖，以此实现回落。
     */
    private void putNonBlank(Map<String, String> target, AgentProfileDO profile) {
        if (profile == null) {
            return;
        }
        loadOwnPrompts(profile.getId()).forEach((slotKey, content) -> {
            if (StrUtil.isNotBlank(content)) {
                target.put(slotKey, content);
            }
        });
    }
}