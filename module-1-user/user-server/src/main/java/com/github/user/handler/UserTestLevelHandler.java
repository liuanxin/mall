package com.github.user.handler;

import com.github.common.util.U;
import com.github.user.enums.UserTestLevel;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 当前 handle 将会被装载进 mybatis 的运行上下文中去.
 *
 * @see org.apache.ibatis.type.TypeHandlerRegistry
 * @see org.apache.ibatis.type.EnumTypeHandler
 * @see org.apache.ibatis.type.EnumOrdinalTypeHandler
 */
public class UserTestLevelHandler extends BaseTypeHandler<UserTestLevel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UserTestLevel parameter,
                                    JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public UserTestLevel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return U.toEnum(UserTestLevel.class, rs.getObject(columnName));
    }

    @Override
    public UserTestLevel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return U.toEnum(UserTestLevel.class, rs.getObject(columnIndex));
    }

    @Override
    public UserTestLevel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return U.toEnum(UserTestLevel.class, cs.getObject(columnIndex));
    }
}
