package com.travelsky.quick.util.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.cares.sh.main.Center;
import com.travelsky.quick.QuickEnv;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.MyServletConfig;
import com.travelsky.quick.log.log4j.DisruptorAppender;
/**
 *  ServletContextListener implementation class PssInitListener
 * @author MaRuifu 2016年5月10日下午2:34:05
 * @version 0.1
 */
public class PssInitListener  implements ServletContextListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(PssInitListener.class);
	/**
	 * @see  PssInitListener()
	 * 初始化
	 */
    public PssInitListener() {}


	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     * @param sce ServletContextEvent
     */
    @Override
    public void contextInitialized(ServletContextEvent sce){
		final ServletContext svc = sce.getServletContext();
		WebApplicationContext ctx =
				  WebApplicationContextUtils.getWebApplicationContext(svc);
		// Init Spring ApplicationContext
		ApiContext.setApplicationContext(ctx);
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     * @param sce ServletContextEvent
     */
    public void contextDestroyed(ServletContextEvent sce)  {
    	DisruptorAppender.closeDisruptor();
    	Center.getCenter().stopApplication();
    }
}
