package org.openkoala.gqc.core.domain.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.openkoala.gqc.core.domain.DataSource;
import org.openkoala.gqc.core.domain.DataSourceType;

import com.dayatang.domain.InstanceFactory;

/**
 * 抽象查询器
 * @author xmfang
 *
 */
public abstract class Querier {

	/**
	 * 查询SQL语句
	 */
	private String querySql;
	
	/**
	 * 数据源
	 */
	private DataSource dataSource;

	public String getQuerySql() {
		return querySql;
	}

	public void setQuerySql(String querySql) {
		this.querySql = querySql;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public Querier(){}
	
	Querier(String querySql, DataSource dataSource) {
		this.querySql = querySql;
		this.dataSource = dataSource;
	}
	
	/**
	 * 生成查询SQL语句
	 * @return
	 */
	public abstract String generateQuerySql();

	/**
	 * 查询
	 * @return
	 * @throws SQLException 
	 */
	public List<Map<String, Object>> query() {
		List<Map<String, Object>> results = null;
		Connection connection = null;
        
        try {
        	connection = getConnection();
            QueryRunner queryRunner = new QueryRunner();
            results = (List<Map<String, Object>>) queryRunner.query(connection, generateQuerySql(), new ResultSetHandler<List<Map<String, Object>>>() {
				public List<Map<String, Object>> handle(ResultSet rs)
						throws SQLException {
					List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
					while (rs.next()) {
						ResultSetMetaData resultSetMetaData = rs.getMetaData();
						Map<String, Object> map = new HashMap<String, Object>();
						for (int i = 0; i < resultSetMetaData.getColumnCount(); i++) {
							if (rs.getObject(i + 1) instanceof java.sql.Date) {
								map.put(resultSetMetaData.getColumnName(i + 1), new SimpleDateFormat("yyyy-MM-dd").format(rs.getDate(i + 1)));
							} else if (rs.getObject(i + 1) instanceof Timestamp) {
								map.put(resultSetMetaData.getColumnName(i + 1), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(rs.getTimestamp(i + 1)));
							} else {
								map.put(resultSetMetaData.getColumnName(i + 1), rs.getObject(i + 1));
							}
						}
						result.add(map);
					}
					return result;
				}
            });
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	closeConnection(connection);
        }
        
        return results;
	}
	
	final Connection getConnection() throws SQLException {
		Connection result = null;
		
		if (dataSource.getDataSourceType().equals(DataSourceType.SYSTEM_DATA_SOURCE)) {
		    try {
				javax.sql.DataSource systemDataSource = InstanceFactory
						.getInstance(javax.sql.DataSource.class,
								dataSource.getDataSourceId());
				result = systemDataSource.getConnection();
			} catch (Exception e) {
				throw new SystemDataSourceNotExistException("系统数据源不存在！",e);
			}
		} else {
			DbUtils.loadDriver(dataSource.getJdbcDriver());
			result = DriverManager.getConnection(dataSource.getConnectUrl(), dataSource.getUsername(), dataSource.getPassword());
		}
		
		return result;
	}
	
	final void closeConnection(Connection connection) {
		if (dataSource.getDataSourceType().equals(DataSourceType.CUSTOM_DATA_SOURCE)) {
    		DbUtils.closeQuietly(connection);
    	}
	}
}