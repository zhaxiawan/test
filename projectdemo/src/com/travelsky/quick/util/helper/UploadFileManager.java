package com.travelsky.quick.util.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.framework.spring.SpringContextHolder;
import com.travelsky.quick.util.upload.FileModel;
import com.travelsky.quick.util.upload.IFileUpload;
import com.travelsky.quick.util.upload.UploadLocationEnum;

/**
 *  文件上传管理
 * @author Administrator
 *
 */
public class UploadFileManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadFileManager.class);

	/**
	 *
	 * @param context  请求的XML数据
	 * @param attachments 附件
	 * @throws  APIException APIException
	 * @return  CommandRet 后台查询结果
	 */
	public CommandRet uploadFile(SelvetContext<ApiContext> context, List<FileItem> attachments)
		throws APIException {
        CommandRet ret = new CommandRet("");
        
        String requestUrl = context.getRequest().getRequestURL().toString();
        String sessionId = context.getRequest().getSession().getId();
        
        for(FileItem fileItem :attachments){
        	if(!fileItem.isFormField()){
				try {
					List<FileItem> itemList = new ArrayList<FileItem>();
             		itemList.add(fileItem);
             		FileModel fm = new FileModel();
                	fm.setFileItems(itemList);
                	fm.setFileKey(UUID.randomUUID().toString().replaceAll("-", ""));
                	fm.setLocationEnum(UploadLocationEnum.REFUND);
                	fm.setRequestUrl(requestUrl);
                	fm.setSessionId(sessionId);
                	
                	ApplicationContext ac = SpringContextHolder.getApplicationContext();
                	IFileUpload fileUpload = (IFileUpload)ac.getBean(com.travelsky.quick.util.upload.IFileUpload.class);
                	if(!fileUpload.upload(fm)){
                		ret.setError(ErrCodeConstants.API_PARSE_FILE_STREAM);
                		return ret;
                	}
             			
         			CommandInput input = new CommandInput("com.cares.sh.file.set");
         			
         			input.addParm("fullname", fileItem.getName());
         			input.addParm("urlhead", fm.getUrlHead());
         			input.addParm("urltail", fm.getUrlTail());
         			input.addParm("fileid", fm.getFileKey());
         			input.addParm("contenttype", fileItem.getContentType());
         			ret = context.doOther(input,false);
         			ret.addParm("clientfilename", fileItem.getName());
         			ret.addParm("fileid", fm.getFileKey());
				} catch (Exception e) {
					LOGGER.error(TipMessager.getInfoMessage(
							ErrCodeConstants.API_PARSE_FILE_STREAM,
							ApiServletHolder.getApiContext().getLanguage()),e);
					throw new APIException(ErrCodeConstants.API_PARSE_FILE_STREAM,e);
				}
        	}
        }
        return ret;
	}

	/**
	 * 获取上传文件大小限制
	 * @return
	 * @throws APIException
	 */
	public long getMaxFileSize() throws APIException {
		long result;
		
		try {
			String size = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_code.toKey("filesizemax"));
			if (!StringUtils.hasLength(size)) {
				// 调底层获取
				size = new ConfigurationManager().getAppCacheValue(
						"API_F_MAX", APICacheHelper.APP_TYPE_COMMON);
				if (!StringUtils.hasLength(size)) {
					// 默认5M
					size = "10485760";
				}
				RedisManager.getManager().set(RedisNamespaceEnum.api_cache_code.toKey("filesizemax"), size, APICacheHelper.CACHE_TIMEOUT);
			}
			
			result = Long.parseLong(size);
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_DATA_F_MAX,
					ApiServletHolder.getApiContext().getLanguage()), e);
			result = 1024 * 1024 * 10;
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_code.toKey("filesizemax"), String.valueOf(result), APICacheHelper.CACHE_TIMEOUT);
		}

		return result;
	}

	/**
	 * 获取上传文件最大阈值。上传文件时超过此值，则会暂时缓存到硬盘
	 * @return
	 * @throws APIException
	 */
	public int getMaxFileMemory() throws APIException {
		int result;
		
		try {
			// 缓存中获取
			String size = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_code.toKey("filesizethread"));
			if (!StringUtils.hasLength(size)) {
				// 调底层获取
				size = new ConfigurationManager().getAppCacheValue(
						"API_F_THRESHOLD",APICacheHelper.APP_TYPE_COMMON);
				if (!StringUtils.hasLength(size)) {
					size = "1024 * 10";
				}
				RedisManager.getManager().set(RedisNamespaceEnum.api_cache_code.toKey("filesizethread"), size, APICacheHelper.CACHE_TIMEOUT);
			}

			result = Integer.parseInt(size);
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_DATA_F_THRESHOLD,
					ApiServletHolder.getApiContext().getLanguage()), e);
			result = 1024 * 10;
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_code.toKey("filesizethread"), String.valueOf(result), APICacheHelper.CACHE_TIMEOUT);
		}

		return result;
	}
}
