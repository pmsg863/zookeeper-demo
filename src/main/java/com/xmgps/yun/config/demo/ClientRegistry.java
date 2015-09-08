package com.xmgps.yun.config.demo;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ClientRegistry {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientRegistry.class);

	private CountDownLatch registryLatch = new CountDownLatch(1);

	private CountDownLatch latch = new CountDownLatch(1);
	private CountDownLatch latch2 = new CountDownLatch(1);
	private volatile List<String> dataList = new ArrayList<>();

	private String registryAddress;

	public ClientRegistry(String registryAddress) {
		this.registryAddress = registryAddress;

		ZooKeeper zk = connectServer();
		if (zk != null) {
			watchNode(zk);
		}
	}

	public void register(String data) {
		if (data != null) {
			ZooKeeper zk = connectServer();
			if (zk != null) {
				updateNode(zk, data);
			}
		}
	}

	private ZooKeeper connectServer() {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {

				public void process(WatchedEvent event) {
					if (event.getState() == Event.KeeperState.SyncConnected) {
						registryLatch.countDown();
					}
				}
			});
			registryLatch.await();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("", e);
		}
		return zk;
	}

	private void updateNode(ZooKeeper zk, String data) {
		try {
			registryLatch.await();

			byte[] bytes = data.getBytes();
			Stat stat = zk.setData(Constant.ZK_MOINTER_PATH, bytes, 0);
		} catch (KeeperException | InterruptedException e) {
			LOGGER.error("", e);
		}
	}

	public List<String> discover() {
		try {
			latch2.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String data = null;
		int size = dataList.size();
		LOGGER.debug("using random data: {}", dataList.toString());
		return dataList;
	}

	private void watchNode(final ZooKeeper zk) {
		try {
			latch.await();

			List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getType() == Event.EventType.NodeChildrenChanged) {
						latch2 = new CountDownLatch(1);
						watchNode(zk);
					}
				}
			});
			List<String> dataList = new ArrayList<>();
			for (String node : nodeList) {
				byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
				dataList.add(new String(bytes));
			}
			LOGGER.debug("node data: {}", dataList);
			this.dataList = dataList;
			latch2.countDown();
		} catch (KeeperException | InterruptedException e) {
			LOGGER.error("", e);
		}
	}

}
