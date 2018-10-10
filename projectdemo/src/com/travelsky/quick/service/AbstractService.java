package com.travelsky.quick.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.log.ats.AtsLogHelper;
import com.travelsky.quick.util.DESUtil;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.ConfigurationManager;
import com.travelsky.quick.util.helper.TipMessager;
/**
 * @author Administrator
 * @version 0.1
 * @param <T> 带量
 */
public abstract class AbstractService<T extends ApiContext> implements Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 1282561126124221172L;
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);
	private static final int FT = 5000;

	/**
	 * @throws Exception
	 */
	protected abstract void doServlet() throws Exception;

	/**
	 * 转换ResponseBean-->XmlBean
	 * @param commandRet 请求数据
	 * @param input XML
	 * @return XmlBean
	 */
	public abstract XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input);

	/**
	 * 处理请求
	 * @param req 请求
	 * @param resp 处理
	 * @param context 数据
	 * @throws IOException Io异常
	 */
	public void processRequest()
			throws IOException {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		context.getResponse().setContentType("application/xml; charset=utf-8");
		context.setRet(new CommandRet(""));

		//将B2C的请求xml 调用接口保存 (直接写入固定队列 不接收返回)
		saveXMLfromB2C();

		context.begin();

		try {
			this.doServlet();
		}
		catch (APIException e) {
			String errCode = e.getErrorCode();
			CommandRet ret = new CommandRet("");
			ret.setError(errCode, TipMessager.getMessage(errCode, context.getContext().getLanguage()));
			context.setRet(ret);
		}
		catch (Exception ex) {
		 context.getRet().setError(ErrCodeConstants.API_SYSTEM,
			TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
				context.getContext().getLanguage()));
		}
		context.commit();
	}

	/**
	 * 将B2C的请求xml 直接写入固定队列 不接收返回
	 * @param apiContext 数据
	 *
	 */
	public void saveXMLfromB2C(){
		ApiContext apiContext=ApiServletHolder.getApiContext();
		String xmlInput = apiContext.getReqXML();

		CommandInput input=new CommandInput("");
		input.addParm("UserId", apiContext.getUserID());
		input.addParm("APPID", apiContext.getAppID());
		input.addParm("version", apiContext.getVersion());
		input.addParm("ServiceName", apiContext.getServiceName());
		input.addParm("Language", apiContext.getLanguage());
		input.addParm("timestamp", apiContext.getTimestamp());
		input.addParm("xml", xmlInput);
	}

	/**
	 * 响应
	 * @param req 请求
	 * @param resp 返回
	 * @param context 数据
	 * @throws IOException IO异常
	 */
	public void processResponse()
			throws IOException {
		SelvetContext<ApiContext> context = ApiServletHolder.get();

		XmlObject xmlBean = this.transResponseBeanToXmlBean(context.getRet(), context.getInput());
		// 将xmlbean 转为 xml
		String xml = transObjectToOutputXML(xmlBean, "UTF-8");
		HttpServletResponse resp = context.getResponse();
		resp.setContentType("text/html;charset=utf-8");

		//将api返回给B2C的xml 调用接口保存
		saveXMLtoB2C(xml);
		// 返回xml
		PrintWriter writer = resp.getWriter();
		try {
			String res=xml;
			int len=res.getBytes().length;
			resp.setHeader("Content-Length", String.valueOf(len));
			writer.write(res);
		} catch (Exception e) {
			writer.close();
		}finally{
			writer.close();
		}
	}

	/**
	 * 将api返回给B2C的xml 直接写入固定队列 不接收返回
	 * @param apiContext 数据
	 * @param xmloutput  返回xml
	 */
	public void saveXMLtoB2C(String xmloutput){
		ApiContext apiContext=ApiServletHolder.getApiContext();
		CommandInput input=new CommandInput("");
		input.addParm("UserId", apiContext.getUserID());
		input.addParm("APPID", apiContext.getAppID());
		input.addParm("version", apiContext.getVersion());
		input.addParm("ServiceName", apiContext.getServiceName());
		input.addParm("Language", apiContext.getLanguage());
		input.addParm("timestamp", apiContext.getTimestamp());
		input.addParm("xml", xmloutput);
	}

	/**
	 *
	 * @param responseObj responseObj
	 * @param applicationEncode applicationEncode
	 * @return string类型值
	 */
	public static String transObjectToOutputXML(Object responseObj, String applicationEncode) {
		return formatXml((org.apache.xmlbeans.XmlObject) responseObj, applicationEncode);
	}

	/**
	 * 将documentObj转成xml
	 *
	 * @param doc
	 * @param applicationEncode
	 *            XML转换的编码，需要与应用部署服务器的实际编码一致
	 * @return
	 */
	private static String formatXml(org.apache.xmlbeans.XmlObject doc, String applicationEncode) {
		String xml = null;
		ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
		String xmlEncode = StringUtils.hasLength(applicationEncode)? applicationEncode : "GBK";
		boolean format;
		format =true;
		try {
			// 是否格式化XML输出0/1:否/是
//			boolean format = true;
			XmlOptions xmlOptions = new XmlOptions();
			// 是否格式化输出XML
			if (format) {
				xmlOptions.setSaveCDataEntityCountThreshold(FT);
				xmlOptions.setSavePrettyPrint();
			}
			xmlOptions.setUseDefaultNamespace();
			xmlOptions.setCharacterEncoding(xmlEncode);
			doc.save(bufferOut, xmlOptions);
			xml = bufferOut.toString(xmlEncode);
		} catch (IOException ex) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_ERROR_XML_FORMAT,
					ApiServletHolder.getApiContext().getLanguage()));
		} finally {
			try {
				bufferOut.close();
			} catch (IOException e) {
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_ERROR_CLOSE_STREAM,
						ApiServletHolder.getApiContext().getLanguage()));
			}
		}
		return xml;
	}
}