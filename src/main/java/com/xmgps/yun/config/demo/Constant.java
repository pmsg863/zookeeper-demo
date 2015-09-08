package com.xmgps.yun.config.demo;

public interface Constant {
	int ZK_SESSION_TIMEOUT = 5000;

	String ZK_REGISTRY_PATH = "/gateway/config";
	String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/server";
	String ZK_MOINTER_PATH = ZK_REGISTRY_PATH + "/clientPriority";
}
