package com.xmgps.yun.config.zookeeper;

import com.xmgps.yun.configuration.config.BindingAddressConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;

/**
 * Created by huangwb on 2015/5/4.
 */
public class ServiceRegister {
    CuratorFramework client;
    String path = "/gateway";
    String name = "config";

    ServiceDiscovery<BindingAddressConfig> serviceDiscovery = null;
    ServiceInstance<BindingAddressConfig> instance = null;

    public ServiceRegister(String host, String path, String name) {
        this.path = path;
        this.name = name;

        client = CuratorFrameworkFactory.newClient(host, new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    public ServiceRegister() {
        client = CuratorFrameworkFactory.newClient("10.50.1.47:4180", new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    public boolean init() throws Exception {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(BindingAddressConfig.class).client(client)
                .basePath(path)
                .serializer(new XStreamSerializer()).build();
        try {
            serviceDiscovery.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean registerConfig(BindingAddressConfig config) throws Exception {
        instance = ServiceInstance.<BindingAddressConfig>builder()
                .name(name)
                .payload(config)
                .build();


        serviceDiscovery.registerService(instance);

        return true;
    }

    public boolean updateConfig(BindingAddressConfig config) throws Exception {

        instance = ServiceInstance.<BindingAddressConfig>builder()
                .id( instance.getId())
                .registrationTimeUTC( instance.getRegistrationTimeUTC() )
                .name(name)
                .payload(config)
                .build();


        serviceDiscovery.updateService(instance);

        return true;
    }
}
