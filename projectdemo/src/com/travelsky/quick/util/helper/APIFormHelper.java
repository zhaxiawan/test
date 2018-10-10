package com.travelsky.quick.util.helper;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.SelvetContext;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;

public class APIFormHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(APIFormHelper.class);

	/**
	 *
	 * @param req
	 *            HttpServletRequest
	 * @throws UnsupportedEncodingException
	 * @throws APIException
	 */
	public static void formType()
			throws UnsupportedEncodingException, APIException {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		ApiContext apiContext = context.getContext();
		HttpServletRequest req = context.getRequest();
		req.setAttribute("flag", true);
		DiskFileItemFactory dff = new DiskFileItemFactory();
		// 设置文件最大大小
		APICacheHelper helper = APICacheHelper.getInstance();
		// 设置文件阈值(超过此值，缓存到硬盘)
		dff.setSizeThreshold(helper.getMaxFileMemory());
		ServletFileUpload fu = new ServletFileUpload(dff);
		long sizeMax = 1024 * 1024 * 10;
//		fu.setSizeMax(helper.getMaxFileSize());
		fu.setSizeMax(helper.getMaxFileSize());
		if(apiContext.getFiles().size() > helper.getMaxFileSize()){
			throw APIException.getInstance(ErrCodeConstants.API_DATA_F_MAX);
		}
		List<FileItem> li = null;
		try {
			li = fu.parseRequest(req);
			Iterator<FileItem> iter = li.iterator();
			while (iter.hasNext()) {
				FileItem item = iter.next();
				// 此处是判断非文件域，即不是<input type="file"/>的标签
				String str = item.getFieldName();
				if (item.isFormField()) {
					// 获取form表单中name的id
					req.setAttribute(str, item.getString("utf-8"));
				} else {
					// 获取form表单中name的id
					apiContext.addFile(str, item);
				}
			}
		} catch (SizeLimitExceededException e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_DATA_FILE_SIZEMAX,
					ApiServletHolder.getApiContext().getLanguage()),e);
			throw APIException.getInstance(ErrCodeConstants.API_DATA_FILE_SIZEMAX);
		} catch (FileUploadException e1) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_PARSE_FILE_STREAM,
					ApiServletHolder.getApiContext().getLanguage()),e1);
			throw APIException.getInstance(ErrCodeConstants.API_PARSE_FILE_STREAM);
		}

	}
}
