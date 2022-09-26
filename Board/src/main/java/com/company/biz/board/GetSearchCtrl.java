package com.company.biz.board;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.company.jdbc.JDBCConnection;
import com.company.vo.BoardVO;

@WebServlet("/GetSearchCtrl")
public class GetSearchCtrl extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("/GetSearchCtrl");
	
		
		
		int page;
		if(request.getParameter("page")==null)
			page=1;
		else
			page=Integer.parseInt(request.getParameter("page"));

		// 1. 접속한 유저 이름 추출
		// 로그인을 안했으면 로그인 페이지로 이동시킨다.
		HttpSession session = request.getSession();
		String name = (String) session.getAttribute("name");
		if (name == null)
			response.sendRedirect("login.jsp");

		String searchCondition = request.getParameter("searchCondition");
		String searchKeyword = request.getParameter("searchKeyword");

		System.out.println(searchCondition);
		System.out.println(searchKeyword);
	
		Connection conn=null;
		PreparedStatement stmt=null;
		ResultSet rs=null;
		
		
		try {
			conn=JDBCConnection.getConnection();
			
			stmt=search(conn,searchCondition,searchKeyword,page);

			rs=stmt.executeQuery();
			
			System.out.println("검색쿼리 문제없이 수행");
			
			ArrayList<BoardVO> boardList=new ArrayList<BoardVO>();
			while(rs.next()) {
				BoardVO vo=new BoardVO();
				vo.setSeq(rs.getInt("seq"));
				vo.setTitle(rs.getString("title"));
				vo.setNickname(rs.getString("nickname"));
				vo.setContent(rs.getString("content"));
				vo.setRegdate(rs.getString("regdate"));
				vo.setCnt(rs.getInt("cnt"));
				vo.setUserid(rs.getString("userid"));
				
				boardList.add(vo);
			}
			
			
			stmt.close();
			rs.close();
			
//			검색이 된 쿼리에서 전체 레코드 개수를 구해야 한다.
			String sql="select count(*) from board where "+searchCondition+" like '%'||?||'%'";
			stmt=conn.prepareStatement(sql);
			stmt.setString(1, searchKeyword);
			
			rs=stmt.executeQuery();
			int totalRows=0;
			if(rs.next())
				totalRows=rs.getInt(1);
			
			System.out.println(totalRows);
			
			request.setAttribute("boardList", boardList);
			
			request.setAttribute("totalRows", totalRows);
			request.setAttribute("searchCondition", searchCondition);
			request.setAttribute("searchKeyword", searchKeyword);
			
			
			
			RequestDispatcher dispatcher=request.getRequestDispatcher("getSearchBoardList.jsp");
			dispatcher.forward(request, response);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			JDBCConnection.close(rs, stmt, conn);
		}
	
	}

	private PreparedStatement search(Connection conn, String searchCondition, String searchKeyword,int page) throws SQLException {
		PreparedStatement stmt=null;
		System.out.println(searchCondition+" "+searchKeyword);
		String sql="select * from\r\n"
				+ "(select rownum as rnum,B.* from (select seq,title,nickname,content,to_char(regdate,'yyyy-mm-dd') \r\n"
				+ "as regdate,cnt,userid from board \r\n"
				+ "where "+searchCondition+" like '%' || ? || '%' order by seq desc) B)\r\n"
				+ "where rnum between ? and ?";	
		stmt=conn.prepareStatement(sql);
		stmt.setString(1, searchKeyword);
		stmt.setInt(2, page*10-9);
		stmt.setInt(3, page*10);
		
		return stmt;
	}

}
