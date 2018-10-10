package com.travelsky.quick.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class APIGetData
 */
public class APIGetData extends HttpServlet {
	private static final long serialVersionUID = 1L;
     private  Map<String, String>map=new HashMap<String, String>();
    /**
     * @see HttpServlet#HttpServlet()
     */
    public APIGetData() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String filename = (String) request.getParameter("p");
		try {
		if (filename.endsWith("rq")) {
			
		}else if (filename.endsWith("rs")) {
			
		}else {
			String file = "/WEB-INF/data/"+filename+"/"+filename+".txt";
			//getResult(file, response);
		}	
		} catch (Exception e) {
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	private void  getResult(String file,HttpServletResponse response) throws Exception {
		 String strR="";
		if (map.containsKey(file)) {
			strR=map.get(file);
		}else {
			InputStream data = getServletContext().getResourceAsStream(file);
			byte[] b = new byte[data.available()];
			data.read(b);
			//关闭流
			data.close();
			//String(byte[])把字节数组转成字符串
			strR= new String(b);
			map.put(file, strR);
		}
		response.setCharacterEncoding("utf-8");
		response.getWriter().write(strR);
	}

}
