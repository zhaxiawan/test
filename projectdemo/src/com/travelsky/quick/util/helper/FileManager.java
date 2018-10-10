package com.travelsky.quick.util.helper;

import java.util.Date;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.controller.ApiServlet;

/**
 * @author ZHANGJIABIN
 */
public class FileManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileManager.class);
	private static Object mlock = new Object();
	private static String mDate = "";
	private static HashMap<String,String> mMap = new HashMap<String,String>();
	/** 
	* @Title: getFile 
	* @Description: 获取文件信息 
	* @param servlet 当前接口处理类
	* @param context 上下文
	* @param fileid 文件名
	* @return
	*/
	public static String getFile(ApiServlet servlet,SelvetContext<ApiContext> context,String fileid){
		synchronized(mlock){
			String lDate = Unit.getString(new Date());
			if(lDate.equals(mDate)){
				mMap.clear();
			}
			mDate = lDate;
		}
		if(mMap.containsKey(fileid)){
			return mMap.get(fileid);
		}
		CommandInput lInput = new CommandInput("com.cares.sh.file.get");
		lInput.addParm("fileid", fileid);
		CommandRet lRet = context.doOther(lInput,false);
		String lFileinfo = lRet.getParm("fileinfo").getStringColumn();
		if(!"".equals(lFileinfo)){ 
			mMap.put(fileid, lFileinfo);
		}
		if(lRet.isError()){
			LOGGER.error("code is {}, desc is {}",lRet.getErrorCode(),lRet.getErrorDesc());
		}
		return lFileinfo;
	}
}
