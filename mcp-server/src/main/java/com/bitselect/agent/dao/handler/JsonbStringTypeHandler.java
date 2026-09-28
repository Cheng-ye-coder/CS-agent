

package com.bitselect.agent.mcp.dao.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 原样进出的 jsonb 列处理器
 * <p>
 * 规格是键值不定的自由结构，反序列化成固定对象反而会把它框死，所以业务层就持有 JSON 原文。
 * 但写入必须包成 PGobject：直接 setString 到 jsonb 列，PostgreSQL 会报
 * 「column is of type jsonb but expression is of type character varying」
 * <p>
 * 只能按字段挂 {@code @TableField(typeHandler = ...)}，绝不能标 {@code @MappedTypes(String.class)}
 * 再全局注册——那等于宣布全应用的 String 参数都按 jsonb 发出去，
 * 随便一句 {@code WHERE sku_code = ?} 都会炸成「operator does not exist: character varying = jsonb」
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    private static final String JSONB = "jsonb";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject json = new PGobject();
        json.setType(JSONB);
        json.setValue(parameter);
        ps.setObject(i, json);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
