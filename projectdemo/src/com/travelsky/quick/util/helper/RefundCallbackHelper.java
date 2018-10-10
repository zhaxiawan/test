package com.travelsky.quick.util.helper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.util.Base64;

public class RefundCallbackHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(RefundCallbackHelper.class);

	/**
	 * 退款回调
	 *
	 * @param context
	 * @param payLogId
	 * @return
	 */
	public CommandRet refundCallback(SelvetContext<ApiContext> context) {
		CommandData input = context.getInput();
		// 只获取底层需要的参数
		String orderno = input.getParm("orderno").getStringColumn();
		long money = input.getParm("refundamount").getLongColumn();
//		String paytype = input.getParm("paytype").getStringColumn();
		String bankid = input.getParm("bankid").getStringColumn();
		String billno = input.getParm("billno").getStringColumn();
		String orgid = input.getParm("orgid").getStringColumn();
		String apptype=input.getParm("apptype").getStringColumn();
		if("".equals(apptype)){
			apptype=input.getParm("AppType").getStringColumn();
		}
		String msg=input.getParm("msg").getStringColumn();
		String[] msgs = msg.split("\\|");
		String refundid=null;
		String refundmoneyid=null;
		if(msgs.length==1) {
			refundid=input.getParm("refundno").getStringColumn();
			refundmoneyid=input.getParm("msg").getStringColumn();
		}
		// 如果为固定格式:refundid|refundmoneyid
		else {
			refundid = msgs[0];
			refundmoneyid = msgs[1];
		}
		String paysucno = refundmoneyid;
		String refunddate = input.getParm("refunddate").getStringColumn();
		String refundstatus = input.getParm("refundstatus").getStringColumn();

		// 调底层获取结果. 如果失败，尝试不间断调用5次，只要有一次成功，则返回，否则认为失败
		CommandRet ret = new CommandRet("");
		for (int i = 0; i < 5; i++) {
			CommandInput input0 = new CommandInput("com.cares.sh.order.order.refundpay");

			input0.addParm("orderno", orderno);
			input0.addParm("money", money);
//			input0.addParm("paytype", paytype);
			input0.addParm("paysucno", paysucno);
			input0.addParm("bankid", bankid);
			input0.addParm("billno", billno);
			input0.addParm("orgid", orgid);
			input0.addParm("apptype", apptype);
			input0.addParm("refundid", refundid);
			input0.addParm("refundmoneyid", refundmoneyid);
			input0.addParm("refunddate", refunddate);
			input0.addParm("refundstatus", refundstatus);
			input0.addParm("paytype", "refund");
			input0.addParm("nomember", "Y");

			ret = context.doOther(input0, false);
			if (!ret.isError()) {
				LOGGER.info("Refund success.");
				return ret;
			}
		}

		LOGGER.info(String.format("Call refund failed. orderno:%s,money:%s,paysucno:%s,error:%s", orderno,
				input.getParm("refundamount").getStringColumn(), input.getParm("msg").getStringColumn(),
				ret.getErrorDesc()));

		return ret;
	}

	/**
	 * easypay验签
	 *
	 * @param context
	 * @return
	 */
	public boolean verfiySign(SelvetContext<ApiContext> context) {
		String isDebug =ParmExUtil.getDebug();
		LOGGER.debug("Start verfiy sign. the debug mode is:{}",isDebug);
		if("true".equalsIgnoreCase(isDebug)) {
			return true;
		}
		CommandData input = context.getInput();

		String apptype=input.getParm("apptype").getStringColumn();
		if("".equals(apptype)){
			apptype=input.getParm("AppType").getStringColumn();
		}

		String data = input.getParm("refundamount").getStringColumn() + "&" + apptype
				+ "&" + input.getParm("bankid").getStringColumn() + "&" + input.getParm("billno").getStringColumn()
				+ "&" + input.getParm("ext1").getStringColumn() + "&" + input.getParm("ext2").getStringColumn() + "&"
				+ input.getParm("lan").getStringColumn() + "&" + input.getParm("msg").getStringColumn() + "&"
				+ input.getParm("ordercurtype").getStringColumn() + "&" + input.getParm("orderno").getStringColumn()
				+ "&" + input.getParm("orgid").getStringColumn() + "&" + input.getParm("refunddate").getStringColumn()
				+ "&" + input.getParm("refundno").getStringColumn() + "&"
				+ input.getParm("refundstatus").getStringColumn() + "&" + input.getParm("refundtype").getStringColumn()
				+ "&";
		String sign = context.getInput().getParm("signature").getStringColumn();

		boolean ret = false;
		try {
			Path path = Paths.get(CommonConstants.CONF_PATH + "CAcert.pem");
			InputStream in = Files.newInputStream(path);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Certificate cacClient = cf.generateCertificate(in);
			PublicKey pubClient = cacClient.getPublicKey();
			Signature rsa = Signature.getInstance("MD5withRSA");
			rsa.initVerify(pubClient);
			rsa.update(data.getBytes());
			ret = rsa.verify(Base64.decode(sign.toCharArray()));

			if (!ret) {
				LOGGER.error("Easypay refund callback {}. JSON is:{}",
						TipMessager.getInfoMessage(ErrCodeConstants.API_DATA_SIGN, context.getContext().getLanguage()),
						JsonUnit.toJson(context.getInput()));
			}
		} catch (Exception ex) {
			Unit.process(ex);
		}
		return ret;
	}
}
