package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AcknowledgementDocument;
import org.iata.iata.edist.AcknowledgementDocument.Acknowledgement;
import org.iata.iata.edist.InvReleaseNotifDocument;
import org.iata.iata.edist.InvReleaseNotifDocument.InvReleaseNotif;
import org.iata.iata.edist.InvReleaseNotifDocument.InvReleaseNotif.Query.Guarantee;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 取消库存
 * @author lizhi
 *
 */
@Service("LCC_INVRELEASENOTIF_SERVICE")
public class APIInvReleaseNotifNDCBusiness  extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		InvReleaseNotifDocument rootDoc = null;
		rootDoc = InvReleaseNotifDocument.Factory.parse(xmlInput);
		InvReleaseNotif irnArry = rootDoc.getInvReleaseNotif();
		Guarantee[] guarantees = irnArry.getQuery().getGuaranteeArray();
		Table guanranteenoTable = new Table(new String[]{"guanranteeno"});
		if(null != guarantees && guarantees.length>0){
			for(Guarantee guarantee:guarantees){
				Row row= guanranteenoTable.addRow();
				String guaranteeid = guarantee.getInvGuaranteeID();
				row.addColumn("guanranteeno", guaranteeid);
			}
		}
		input.addParm("guanrantee", guanranteenoTable);
		OrderOpManager orderOpManager = new OrderOpManager();
		// 获取ResponseBean
		context.setRet(orderOpManager.invReleaseNotif(input, context));
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		CommandRet xmlOutput = (CommandRet) commandRet;
		AcknowledgementDocument  doc = AcknowledgementDocument.Factory.newInstance();
		Acknowledgement rs=doc.addNewAcknowledgement();
		String errorcode = xmlOutput.getErrorCode();
		String errordesc = xmlOutput.getErrorDesc();
		rs.setStatusCode(StringUtils.hasLength(errorcode) ? TipMessager.getErrorCode(errorcode) : "OK");
		rs.setStatusMessage(StringUtils.hasLength(errordesc)?(TipMessager.getMessage(errorcode,
				ApiServletHolder.getApiContext().getLanguage())):"");
		return doc;
	}
}
