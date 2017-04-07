package cqk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import Database.DBCon;
import Database.DBTools;

@WebServlet("/login")
public class Login extends HttpServlet {

	private String status = "0";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO 自动生成的方法存根
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO 自动生成的方法存根
		doRequest(req);
		doResponse(resp);
	}

	private void doRequest(HttpServletRequest req) {
		try {
			req.setCharacterEncoding("utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			getDate(req);
		} catch (UnknownHostException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}

		String useName = req.getParameter("usename");
		String password = req.getParameter("password");

		System.out.println(useName + " " + password);

		try {
			selectDB(useName, password);
		} catch (SQLException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}

	}

	private void doResponse(HttpServletResponse resp) {
		resp.setContentType("text/html;charset=UTF-8");
		resp.setCharacterEncoding("utf-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		Map<String, String> map = new HashMap<String, String>();
		map.put("status", status);

		Gson gson = new Gson();
		String json = gson.toJson(map);

		try {
			PrintWriter out = resp.getWriter();
			out.print(json);
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getDate(HttpServletRequest req) throws UnknownHostException {

		System.out.println("Servlet=" + req.getServletPath());
		System.out.println("method=" + req.getMethod());

		InetAddress iA = null;
		iA = InetAddress.getLocalHost();

		String localName = iA.getHostName();
		String localIp = iA.getHostAddress();
		System.out.println("localName=" + localName);
		System.out.println("localIp=" + localIp);
	}

	private void selectDB(String usename, String password) throws SQLException {
		String sql = "";
		DBCon dbCon = new DBCon("cv");
		ResultSet ret = null;
		sql = "SELECT * FROM USERTABLE WHERE userName='" + usename + "' AND Password='" + password + "' ;";
		ret = dbCon.pst.executeQuery(sql);
		if (ret.next()) {
			status = "1";
		} else {
			status = "-1";
		}
	}

}
