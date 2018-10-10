package com.travelsky.quick.common;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class SpringApplicationContext extends XmlWebApplicationContext {

	@Override
	protected Resource getResourceByPath(String path) {
		// TODO Auto-generated method stub
		Resource resource = super.getResourceByPath(path);
		if (!resource.exists()) {
			resource = new FileSystemResource(path);
		}

		return resource;
	}

}
