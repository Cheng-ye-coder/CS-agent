

package com.bitselect.agent.infra.model;

/**
 * 模型调用器函数式接口
 *
 * @param <C> 客户端类型
 * @param <T> 返回值类型
 */
@FunctionalInterface
public interface ModelCaller<C, T> {

    T call(C client, ModelTarget target) throws Exception;
}