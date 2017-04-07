package cqk;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.DocFlavor.STRING;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import Database.DBCon;

@WebServlet("/result")
public class Result extends HttpServlet {
	public native  int Math(String cNum,String startTime,String nowTime,String leftT,String rightB);
	private int resultID = 0;
	private List<Url> list = new ArrayList<Url>();

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
		String startTime = req.getParameter("starttime");
		String nowTime = req.getParameter("nowtime");
		String leftT = req.getParameter("leftT");
		String rightB = req.getParameter("rightB");
		resultID=1;
		System.loadLibrary("mytracker");
		Result nativeCode = new Result();
		 resultID=nativeCode.Math(cNum,startTime,nowTime,leftT,rightB);
		 System.out.println("resultID: "+resultID);
		 try {
		 selectDB(resultID);
		 } catch (SQLException e) {
		 // TODO 自动生成的 catch 块
		 e.printStackTrace();
		 }

	}

	private void doResponse(HttpServletResponse resp) {
		resp.setContentType("text/html;charset=UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setCharacterEncoding("utf-8");
		Map<String, List<Url>> map = new HashMap<String, List<Url>>();
		map.put("url", list);

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

	private void selectDB(int resultID) throws SQLException {
		list.clear();
		String sql = "";
		DBCon dbCon = new DBCon("cv");
		ResultSet ret = null;
		sql = "SELECT * FROM resultTable WHERE resultID=" + resultID + " ;";
		ret = dbCon.pst.executeQuery(sql);
		while (ret.next()) {

			String vUrl = ret.getInt(4) + "";
			String vPath = ret.getString(5);
			String pPath = ret.getString(3);

			//System.out.println(vUrl+" "+vPath+" "+pPath);
			Url url = new Url(vUrl, vPath, pPath);
			list.add(url);
		}
	}


}
