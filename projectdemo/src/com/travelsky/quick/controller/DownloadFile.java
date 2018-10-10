package com.travelsky.quick.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.helper.APIFormHelper;
import com.travelsky.quick.util.helper.FileManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 *
 * @author ZHANGJIABIN
 *
 */
public class DownloadFile extends ApiServlet {

	private static final long serialVersionUID = -6625906214366547227L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFile.class);
	private static String language = ApiServletHolder.getApiContext().getLanguage();

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		String lFileid =context.getInput().getParm("fileid").getStringColumn();
		if(lFileid.equals("")){
			return;
		}
		String lFileinfo = FileManager.getFile(this,context, lFileid);
		if(!lFileinfo.equals("")){
			try{
				byte[] lByte =Base64.decodeBase64(lFileinfo);
				context.getResponse().setContentType("image/jpeg");
				context.getResponse().getOutputStream().write(lByte);
			}
			catch(Exception ex){
				LOGGER.error("",ex);
			}
		}
	}
	@Override
	protected CommandRet parseInputJson(HttpServletRequest request,
			CommandData input) {
		CommandRet ret = new CommandRet("");
		try{
			//form
			APIFormHelper.formType();

			SelvetContext<ApiContext> context = ApiServletHolder.get();
			request = context.getRequest();
			String json=request.getAttribute(ApiContext.REQ_PARAM_XML).toString();
			if (StringUtils.hasLength(json)) {
				JsonUnit.fromJson(input, json);
				ret.addParm("reqJson", json);
			}
			else {
				ret.setError(ErrCodeConstants.API_PARAM_ERROR, 
						TipMessager.getMessage(ErrCodeConstants.API_PARAM_ERROR, 
								language));
			}
		}catch(Exception ex){
			Unit.process(ex);
			ret.setError(ErrCodeConstants.API_PARAM_ERROR, 
					TipMessager.getMessage(ErrCodeConstants.API_PARAM_ERROR, 
							language));
		}

		return  ret;
	}

}
