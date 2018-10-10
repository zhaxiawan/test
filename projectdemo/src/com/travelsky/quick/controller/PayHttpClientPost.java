package com.travelsky.quick.controller;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.cares.sh.comm.Md5;
import com.cares.sh.parm.CommandData;


public class PayHttpClientPost {
	public static String setHttpClientPost(String reqUrl, CommandData cmdData)
			throws Exception {
		
		/**
		 * 首先要和URL下的URLConnection对话。 URLConnection可以很容易的从URL得到。比如： // Using
		 * java.net.URL and //java.net.URLConnection
		 * 
		 * 使用页面发送请求的正常流程：在页面http://www.faircanton.com/message/loginlytebox.
		 * asp中输入用户名和密码，然后按登录，
		 * 跳转到页面http://www.faircanton.com/message/check.asp进行验证 验证的的结果返回到另一个页面
		 * 
		 * 使用java程序发送请求的流程：使用URLConnection向http://www.faircanton.com/message/
		 * check.asp发送请求 并传递两个参数：用户名和密码 然后用程序获取验证结果
		 * URL=http://localhost:8080/api/ndc
		 */
		reqUrl=reqUrl+"?"+PayHttpClientPost.getTimeAll();
		
		URL url = new URL(reqUrl);
		URLConnection connection = url.openConnection();

		/**
		 * 最后，为了得到OutputStream，简单起见，把它约束在Writer并且放入POST信息中，例如： ...
		 */
		OutputStream os = connection.getOutputStream();
		String orgid = cmdData.getParm("orgid").getStringColumn();// "1.0";
		String apptype = cmdData.getParm("apptype").getStringColumn();// "B2C";
		String bankid = cmdData.getParm("bankid").getStringColumn();// "CHINAPNRMID";
		String billno = cmdData.getParm("billno").getStringColumn();// "orderno";
		String orderno = cmdData.getParm("orderno").getStringColumn();//orderno 
		String paydate = getTime();
		String paytime = getmillTime();
		String ordercurtype = cmdData.getParm("ordercurtype").getStringColumn();//CNY
		String ordertype = cmdData.getParm("ordertype").getStringColumn();//1
		String lan = cmdData.getParm("lan").getStringColumn();//cn
		String msg = cmdData.getParm("msg").getStringColumn();//
		String ext1 = cmdData.getParm("ext1").getStringColumn(); // 
		String ext2 = cmdData.getParm("ext2").getStringColumn(); // 
		String paystatus = cmdData.getParm("paystatus").getStringColumn();//1
		String returntype = cmdData.getParm("returntype").getStringColumn();//暂时为server
		String payAmount = cmdData.getParm("payAmount").getStringColumn();


		String sign = signature(
				payAmount,
				apptype, 
				bankid,
				billno,
				ext1, 
				ext2, 
				lan, 
				msg, 
				ordercurtype, 
				ordertype,
				orgid,
				paydate, 
				paytime, 
				orderno,
				paystatus);
		StringBuffer buffer = new StringBuffer("payamount=" + payAmount)
				.append("&orgid=" + orgid)
				.append("&apptype=" + apptype)
				.append("&bankid=" + bankid)
				.append("&billno=" + billno)
				.append("&orderno=" + orderno)
				.append("&paydate=" + paydate)
				.append("&paytime=" + paytime)
				.append("&ordercurtype=" + ordercurtype)
				.append("&ordertype=" + ordertype)
				.append("&lan=" + lan)
				.append("&msg=" + msg)
				.append("&ext1=" + ext1)
				.append("&ext2=" + ext2)
				.append("&paystatus=" + paystatus)
				.append("&returntype=" + returntype)
				.append("&sign=" + sign);

		os.write(buffer.toString().getBytes("UTF-8"));
		os.flush();
		os.close();
		
		 // 这样就可以发送一个看起来象这样的POST： POST /jobsearch/jobsearch.cgi HTTP 1.0 ACCEPT:
		  //text/plain Content-type: application/x-www-form-urlencoded
		 // Content-length: 99 username=bob password=someword
		 
		// 一旦发送成功，用以下方法就可以得到服务器的回应：
		String sCurrentLine;
		StringBuffer sTotalString = new StringBuffer();
		sCurrentLine = "";
		InputStream lUrlStream;
		lUrlStream = connection.getInputStream();
		// 传说中的三层包装阿！
		BufferedReader lReader = new BufferedReader(new InputStreamReader(
				lUrlStream));
		while ((sCurrentLine = lReader.readLine()) != null) {
			sTotalString.append(sCurrentLine).append("\r\n");
		}
		lReader.close();
		
		return sTotalString.toString();
		
	}

	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String time = sdf.format(new Date());
		return time;
	}
	
	public static String getmillTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
		String time = sdf.format(new Date());
		return time;
	}
	
	public static String getTimeAll() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String time = sdf.format(new Date());
		return time;
	}

	public static String signature(
			String payamount,
			String apptype, 
			String bankid,
			String billno,
			String ext1, 
			String ext2, 
			String lan, 
			String msg,
			String ordercurtype, 
			String ordertype,
			String orgid,
			String paydate, 
			String paytime, 
			String orderno, 
			String paystatus)
			throws Exception {
		// 签名串
		String signature = new StringBuilder(payamount)
				.append("&")
				.append(apptype)
				.append("&")
				.append(bankid)
				.append("&")
				.append(billno)
				.append("&")
				.append(ext1)
				.append("&")
				.append(ext2)
				.append("&")
				.append(lan)
				.append("&")
				.append(msg)
				.append("&")
				.append(ordercurtype)
				.append("&")
				.append(ordertype)
				.append("&")
				.append(orgid)
				.append("&")
				.append(paydate)
				.append("&")
				.append(paytime)
				.append("&")
				.append(orderno)
				.append("&")
				.append(paystatus).toString();
		// 验签
		return (new Md5()).Encrypt(signature).toLowerCase();
	}
}

