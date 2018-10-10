package com.travelsky.quick.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.util.Base64;
import com.travelsky.quick.util.helper.APIFormHelper;
import com.travelsky.quick.util.helper.TipMessager;

public class UploadFile extends ApiServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 7537442593711543756L;
	private static Logger logger = LoggerFactory.getLogger(UploadFile.class);
	private static String language = ApiServletHolder.getApiContext().getLanguage();

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		context.getResponse().setContentType("application/json; charset=utf-8");
		CommandRet l_ret = new CommandRet("");
		ApiContext apiContext = context.getContext();
		Collection<FileItem> attachs = apiContext.getFiles().values();
		List<FileItem> fileItems = new ArrayList<FileItem>();
		fileItems.addAll(attachs);
        boolean file_one = false;
        try{
            for(FileItem fileItem :fileItems){
            	if(!fileItem.isFormField()){
            		if(!file_one){
            			String l_fileinfo =  new String(Base64.encode(fileItem.get()));
            			String filename =fileItem.getName();
            			CommandInput l_input = new CommandInput("com.cares.sh.file.set");
            			l_input.addParm("fileinfo", l_fileinfo);
            			l_input.addParm("fullname", filename);
            			l_ret = context.doOther(l_input,false);
            			l_ret.addParm("filename", filename);
            		}
            	}
            }
        }catch(Exception e1){
            l_ret.setError("9998","系统出错！");
        }
        if(l_ret != null){
			try {
				context.getResponse().getWriter().write(JsonUnit.toJson(l_ret));
			} catch (IOException e) {
				//TODO
				logger.error("",e);
			}
		}
	}
	/**
	 * 初始化
	 * @param json
	 */
	@Override
	protected void initContext(String json) {
		try {
			// true 解密  false不解密
			ApiServletHolder.getAndInit(false);
		} catch (Exception ex) {
			// TODO: handle exception
			Unit.process(ex);
		}
	}

	@Override
	protected CommandRet parseInputJson(HttpServletRequest request, CommandData input) {
		CommandRet ret = new CommandRet("");
		try{
			//form
			APIFormHelper.formType();

			SelvetContext<ApiContext> context = ApiServletHolder.get();
			request = context.getRequest();
			ret.addParm("reqJson",  request.getAttribute(ApiContext.REQ_PARAM_XML).toString());
		}
		catch (APIException e) {
			String errCode = e.getErrorCode();
			ret.setError(errCode, TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		catch(Exception ex){
			Unit.process(ex);
			ret.setError(ErrCodeConstants.API_PARAM_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_PARAM_ERROR,
							language));
		}

		return ret;
	}

	@Override
	protected boolean isEncrypt() {
		return false;
	}

	@Override
	public boolean isJsonInput(){
		return false;
	}

	@Override
	public boolean isJsonOutput(){
		return false;
	}
}
