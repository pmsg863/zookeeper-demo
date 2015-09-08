package com.xmgps.yun.config.zookeeper;

import com.xmgps.yun.communication.statistics.ServerStatistics;
import com.xmgps.yun.config.zookeeper.XStringSerializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;

/**
 * Created by huangwb on 2015/5/4.
 */
public class ServiceJmxRegister {
    CuratorFramework client;
    String path = "/gateway";
    String name = "jmx";

    ServiceDiscovery<ServerStatistics> serviceDiscovery = null;
    ServiceInstance<ServerStatistics> instance = null;

    public ServiceJmxRegister(String host, String path, String name) {
        client = CuratorFrameworkFactory.newClient(host, new ExponentialBackoffRetry(1000, 3));
        client.start();

        this.path = path;
        this.name = name;
    }

    public ServiceJmxRegister() {
        client = CuratorFrameworkFactory.newClient("10.50.1.47:4180", new ExponentialBackoffRetry(1000, 3));
        client.start();

    }

    public boolean init() throws Exception {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServerStatistics.class).client(client)
                .basePath(path)
                .serializer(new XStringSerializer()).build();
        try {
            serviceDiscovery.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean registerConfig(ServerStatistics config) throws Exception {
        instance = ServiceInstance.<ServerStatistics>builder()
                .name(name)
                .payload(config)
                .build();


        serviceDiscovery.registerService(instance);

        return true;
    }

    public boolean updateConfig(ServerStatistics config) throws Exception {

        instance = ServiceInstance.<ServerStatistics>builder()
                .id( instance.getId())
                .registrationTimeUTC( instance.getRegistrationTimeUTC() )
                .name(name)
                .payload(config)
                .build();


        serviceDiscovery.updateService(instance);

        return true;
    }
}
