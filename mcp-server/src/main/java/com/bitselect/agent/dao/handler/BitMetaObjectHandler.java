

package com.bitselect.agent.mcp.dao.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.sql.Timestamp;

/**
 * 创建与更新时间的自动填充
 * <p>
 * 建表脚本里不给这两列 DEFAULT now()：时间由谁写死在一处，要么全归数据库、要么全归应用，
 * 两边各写一半就会出现「有的行是库时间、有的行是应用时间」，排查时序问题时最误导
 */
public class BitMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        strictInsertFill(metaObject, "createTime", () -> now, Timestamp.class);
        strictInsertFill(metaObject, "updateTime", () -> now, Timestamp.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateTime", new Timestamp(System.currentTimeMillis()), metaObject);
    }
}
