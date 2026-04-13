package com.yuluo.yuluoaiagent.agent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent{
    /**
     * 思考
     *
     * @return true表示需要执行； false表示不需要执行
     */
    public abstract boolean think();
    /**
     * 执行
     *
     * @return 执行结果
     */
    public abstract String act();

    /**
     * 步骤
     *
     * @return 步骤结果
     */
    @Override
    public String step() {
        try {
            // 先思考
            boolean thinkResult = think();
            if (!thinkResult){
                return "No need to think.";
            }
            // 再行动
            return act();
        } catch (Exception e) {
            // 记录异常日志
            return "Steps Executing Error: " + e.getMessage();
        }
    }






















}
