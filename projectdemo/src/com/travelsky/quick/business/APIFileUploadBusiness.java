package com.travelsky.quick.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FileRetrieveRSDocument;
import org.iata.iata.edist.FileRetrieveRSDocument.FileRetrieveRS;
import org.iata.iata.edist.FileRetrieveRSDocument.FileRetrieveRS.Files.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.APIFormHelper;
import com.travelsky.quick.util.helper.TipMessager;
import com.travelsky.quick.util.helper.UploadFileManager;

/**
 * 类说明:文件上传
 *
 * @author huxizhun
 *
 */
@Service("LCC_FILEUPLOAD_SERVICE")
public class APIFileUploadBusiness extends AbstractService<ApiContext> {
	/**
	 *
	 */
	private static final long serialVersionUID = -2595881636351455263L;

	private static final Logger LOGGER = LoggerFactory.getLogger(APIFileUploadBusiness.class);

	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			APIFormHelper.formType();
			// 获取ResponseBean
			context.setRet(getResult(context, context.getContext().getFiles().values()));
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_FILE_UPLOAD, ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

//	/**
//	 * 设置xml输入，并返回附件
//	 * @param context
//	 * @return  返回附件
//	 */
//	private List<FileItem> setXMLInput(SelvetContext<ApiContext> context, CommandData input)
//		throws APIException {
//		List<FileItem> attachs = new ArrayList<FileItem>();
//		input.addParm("xmlInput", (String) context.getRequest().getAttribute("ReqXML"));
//		attachs.add((FileItem)context.getRequest().getAttribute("uploadFile"));
//		return attachs;
//	}

	private CommandRet getResult(SelvetContext<ApiContext> context, Collection<FileItem> attachs)
			 throws APIException {
		List<FileItem> attachments = new ArrayList<FileItem>();
		attachments.addAll(attachs);
		UploadFileManager manager = new UploadFileManager();
		return manager.uploadFile(context, attachments);
	}

	/**
	 *
	 * @param context SelvetContext
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {

	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		FileRetrieveRSDocument doc = FileRetrieveRSDocument.Factory.newInstance();
		FileRetrieveRS root = doc.addNewFileRetrieveRS();
		try {
			if (processError(commandRet, root)) {
				return doc;
			}
			root.addNewSuccess();
			File newFile = root.addNewFiles().addNewFile();
			//文件编号
			String fileid = commandRet.getParm("fileid").getStringColumn();
			if(StringUtils.hasLength(fileid)){
				newFile.addNewAttachmentURI().setStringValue(fileid);
			}
			//文件名称
			String filename = commandRet.getParm("clientfilename").getStringColumn();
			if(StringUtils.hasLength(filename)){
				newFile.setDescription(filename);
			}
		}
		catch (Exception e) {
			doc = FileRetrieveRSDocument.Factory.newInstance();
			root = doc.addNewFileRetrieveRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM,
				TipMessager.getInfoMessage(ErrCodeConstants.API_SYSTEM,
						ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}
		return doc;
	}

	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * @param ret
	 * @param root
	 * @return
	 */
	private boolean processError(CommandRet ret, FileRetrieveRS root) {
		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}

		return false;
	}
}
