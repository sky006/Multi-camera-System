package Database;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBTools {
	static String sql = null;
	static DBCon dbc = null;
	private String dbName = null;

	public DBTools(String db) {
		dbName = db;
	}

	// 更新数据库
	public void UpdateTable(String sql) {
		dbc = new DBCon(dbName);// 创建DBHelper对象
		try {
			int ret = dbc.pst.executeUpdate(sql);// 执行语句，得到结果集
			dbc.close();// 关闭连接
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
