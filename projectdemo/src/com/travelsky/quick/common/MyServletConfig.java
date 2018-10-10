package com.travelsky.quick.common;


import com.cares.sh.main.ServletConfig;
import com.travelsky.quick.controller.AdyenOnlinePayCallback;
import com.travelsky.quick.controller.AdyenOnlinePayResult;
import com.travelsky.quick.controller.ApiServlet;
import com.travelsky.quick.controller.DownloadFile;
import com.travelsky.quick.controller.InsuCallBack;
import com.travelsky.quick.controller.OnlinePayCallback;
import com.travelsky.quick.controller.RefundCallbackServlet;
import com.travelsky.quick.controller.UploadFile;
import com.travelsky.quick.controller.WorldPayCallBack;

/**
 *
 * @author ZHANGJIABIN
 *
 */
public class MyServletConfig extends ServletConfig {
	@Override
	public void init() {
		this.addServlet(ApiServlet.class, "/api/base");
		this.addServlet(UploadFile.class, "/api/uploadfile");
		this.addServlet(DownloadFile.class, "/api/downloadfile");
		this.addServlet(OnlinePayCallback.class, "/api/olpaycall");
		this.addServlet(RefundCallbackServlet.class, "/api/refundcall");
		this.addServlet(InsuCallBack.class, "/api/insucall");
		this.addServlet(AdyenOnlinePayCallback.class, "/api/pay/adyencall");
		this.addServlet(AdyenOnlinePayResult.class, "/api/pay/payresult");
		this.addServlet(WorldPayCallBack.class, "/api/pay/worldpaycall");
	}
}
