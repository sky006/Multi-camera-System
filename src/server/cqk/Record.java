package cqk;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import Database.DBCon;

@WebServlet("/record")
public class Record extends HttpServlet {

	private String url;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO 自动生成的方法存根
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO 自动生成的方法存根
		getDate(req);
		System.out.println("doReq!!");
		doRequest(req);
		System.out.println("doResp!!");
		doResponse(resp);
	}

	private void doRequest(HttpServletRequest req) {
		try {
			req.setCharacterEncoding("utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String cNum = req.getParameter("cnum");
		String time = req.getParameter("time");

		System.out.println(cNum + " " + time);

		try {
			selectDB(cNum, time);
		} catch (SQLException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}

	}

	private void doResponse(HttpServletResponse resp) {
		resp.setContentType("text/html;charset=UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setCharacterEncoding("utf-8");
		Map<String, String> map = new HashMap<String, String>();
		map.put("url", url);

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

	private void selectDB(String cnum, String time) throws SQLException {

		String[] ary = time.split("/");
		System.out.println("///"+ary[0]+" "+ary[1]+" "+ary[2]+ " "+ary[3]+""+ary[4]);
		String startTime = ary[0] + ary[1] + ary[2] + ary[3];
		String endTime = ary[0] + ary[1] + ary[2] + ary[4];

		String sql = "Select path from Camera1";
		DBCon dbCon = new DBCon("cv");
		ResultSet ret = null;
		sql = "SELECT path FROM Camera" + cnum + " WHERE startTime BETWEEN STR_TO_DATE ('" + startTime
				+ "','%Y%m%d%H:%i:%s') and STR_TO_DATE('" + startTime
				+ "','%Y%m%d%H:%i:%s') AND endTime BETWEEN STR_TO_DATE ('" + endTime
				+ "','%Y%m%d%H:%i:%s') and STR_TO_DATE('" + endTime + "','%Y%m%d%H:%i:%s')  ;";
		System.out.println("sql: "+sql);
		ret = dbCon.pst.executeQuery(sql);
		if (ret.next()) {
			url = ret.getString(1);
		} else {
			url = "-1";
		}
	}


}
