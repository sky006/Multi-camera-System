package Database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.mysql.jdbc.Statement;

public class DBCon {
	// MySQL的JDBC URL编写方式：jdbc:mysql://主机名称：连接端口/数据库的名称?参数=值
	// 避免中文乱码要指定useUnicode和characterEncoding
	// 执行数据库操作之前要在数据库管理系统上创建一个数据库，名字自己定，
	// 下面语句之前就要先创建javademo数据库
	public static final String url = "jdbc:mysql://localhost:3306/";
	public static final String name = "com.mysql.jdbc.Driver";
	public static final String password="?user=root&password=root&useUnicode=true&characterEncoding=UTF8&useSSL=true";

	public Connection conn = null;
	public java.sql.Statement pst = null;

	public DBCon(String db) {
		try {
			Class.forName(name);// 指定连接类型
			conn = DriverManager.getConnection(url+db+password);// 获取连接
			pst = conn.createStatement();// 准备执行语句
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			this.conn.close();
			this.pst.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
