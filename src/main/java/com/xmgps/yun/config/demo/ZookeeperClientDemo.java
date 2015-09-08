package com.xmgps.yun.config.demo;

import com.xmgps.yun.config.zookeeper.XStreamSerializer;
import com.xmgps.yun.configuration.config.BindingAddressConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangwb on 2015/4/23.
 */
public class ZookeeperClientDemo {

    static CuratorFramework client;

    static String testData = "<com.xmgps.yun.configuration.config.BindingAddressConfig>\n" +
            "  <name>test</name>\n" +
            "  <version>0</version>\n" +
            "  <localHostID>10010002</localHostID>\n" +
            "  <host>10.10.16.172</host>\n" +
            "  <port>7766</port>\n" +
            "</com.xmgps.yun.configuration.config.BindingAddressConfig>";

    public static void main(String[] args) throws Exception {
        client = CuratorFrameworkFactory.newClient("10.50.1.47:4180", new ExponentialBackoffRetry(1000, 3));
        client.start();

        //修改信息
        Stat stat1 = client.setData().forPath("/gateway");
        int version = stat1.getVersion();
        System.out.println(version);
        stat1 = client.setData().forPath("/gateway");
        version = stat1.getVersion();
        System.out.println(version);

        ServiceDiscovery<BindingAddressConfig> serviceDiscovery = null;
        serviceDiscovery = ServiceDiscoveryBuilder.builder(BindingAddressConfig.class).client(client).basePath("/gateway").serializer(new XStreamSerializer()).build();
        serviceDiscovery.start();
        //获取某个配置项
        ServiceInstance<BindingAddressConfig> configWrapServiceInstance = serviceDiscovery.queryForInstance("config", "server0000000005");
        BindingAddressConfig payload = configWrapServiceInstance.getPayload();
        //注册某个配置项
        payload.setHost("haha2");
        serviceDiscovery.registerService(ServiceInstance.<BindingAddressConfig>builder()
                .name("servertest")
                .id("server11257")
                .payload(payload)
                .build());

        //获取某一目录下的所有配置，会定时更新？
        ServiceCache<BindingAddressConfig> cache2 = serviceDiscovery.serviceCacheBuilder().name("config").build();
        cache2.addListener(new ServiceCacheListener() {
            @Override
            public void cacheChanged() {
                System.out.println("cacheChanged");
            }

            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                System.out.println("stateChanged");
            }
        });
        cache2.getInstances();

        serviceDiscovery.serviceProviderBuilder();

        //注册某个name下面的所有配置项
        Collection<ServiceInstance<BindingAddressConfig>> config = serviceDiscovery.queryForInstances("config");
        System.out.println(config.toString());

        /**
         * pathChildrenCache  NodeCache
         */
        PathChildrenCache cache = new PathChildrenCache(client, "/gateway/config", false);
        cache.start();
        //注册监听
        cache.getListenable().addListener(new PathChildrenCacheListener() {

            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
                    throws Exception {
                switch (event.getType()) {
                    case CHILD_ADDED: {
                        System.out.println("Node added: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        System.out.println(new String(client.getData().forPath(event.getData().getPath())));
                        break;
                    }

                    case CHILD_UPDATED: {
                        System.out.println("Node changed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        break;
                    }

                    case CHILD_REMOVED: {
                        System.out.println("Node removed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        break;
                    }
                }


            }
        });

        //判断节点是否存在
        Stat stat = client.checkExists().forPath("/gateway/config/mytest");
        if (stat == null)
            //常住节点
            client.create().forPath("/gateway/config/mytest", testData.getBytes());
        //修改信息
        client.setData().forPath("/gateway/config/mytest", testData.getBytes());
        //临时节点
        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/gateway/config/temptest2", "test2".getBytes());

        PersistentEphemeralNode node = new PersistentEphemeralNode(client, PersistentEphemeralNode.Mode.EPHEMERAL, "/gateway/config/temptest", "test".getBytes());
        node.start();
        node.waitForInitialCreate(3, TimeUnit.SECONDS);
        node.close();

        while (true) {
            ServiceInstance<BindingAddressConfig> bindingAddressConfigServiceInstance = serviceDiscovery.queryForInstance("config", "server0000000005");
            payload = configWrapServiceInstance.getPayload();

            BindingAddressConfig tempConfit = null;
            try {
                tempConfit = new BindingAddressConfig("GW");
            } catch (Exception ex) {
            }
            tempConfit.setLocalHostID(payload.getLocalHostID());
            tempConfit.setVersion(payload.getLocalHostID());
            tempConfit.setPort(payload.getPort() + 1);
            tempConfit.setHost(payload.getHost());

            System.out.println("-----------------------------------------------------------");
            System.out.println(XStreamSerializer.getXml(tempConfit));
            System.out.println("-----------------------------------------------------------");
            client.setData().forPath("/gateway/config/server0000000005", XStreamSerializer.getXml(tempConfit).getBytes());
            TimeUnit.SECONDS.sleep(5);
        }
    }


}
