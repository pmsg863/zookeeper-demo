package com.xmgps.yun.config.zookeeper;

import com.xmgps.yun.communication.statistics.ServerStatistics;
import com.xmgps.yun.configuration.config.BindingAddressConfig;
import com.xmgps.yun.configuration.config.ClientConnectionConfig;
import com.xmgps.yun.configuration.param.NetworkConnection;
import com.xmgps.yun.configuration.param.NetworkConnections;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by huangwb on 2015/5/12.
 */
public class ServiceDiscovery {
    CuratorFramework client;
    String path = "/gateway";
    String name = "config";
    String monitor = "jmx";

    org.apache.curator.x.discovery.ServiceDiscovery<BindingAddressConfig> serviceDiscovery = null;
    org.apache.curator.x.discovery.ServiceDiscovery<ServerStatistics> monitorDiscovery = null;

    public ServiceDiscovery(String host, String path, String name,String monitor) {
        this.path = path;
        this.name = name;
        this.monitor = monitor;

        client = CuratorFrameworkFactory.newClient(host, new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    public ServiceDiscovery() {
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

        monitorDiscovery = ServiceDiscoveryBuilder.builder(ServerStatistics.class).client(client)
                .basePath(path)
                .serializer(new XStringSerializer()).build();
        try {
            monitorDiscovery.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public ClientConnectionConfig discoveryConfig() throws Exception {
        ClientConnectionConfig clientConnection = new ClientConnectionConfig("GW");
        clientConnection.setLocalHostID(generateHostID());
        //注册某个name下面的所有配置项
        NetworkConnections connections = new NetworkConnections();
        Collection<ServiceInstance<BindingAddressConfig>> serviceInstances = serviceDiscovery.queryForInstances(name);
        //计算优先级，根据监控信息的 平均数据流量来排序 得出优先级
        Collection<ServiceInstance<ServerStatistics>> statisticsInstances = monitorDiscovery.queryForInstances(monitor);
        List<XStringSerializer.PriorityRank> prioritys = new ArrayList<>();
        for (ServiceInstance<ServerStatistics> configInstance : statisticsInstances) {
            ServerStatistics payload = configInstance.getPayload();
            long localHostID = payload.getLocalHostID();
            int currentSessionCount = payload.getCurrentSessionCount();
            double recvBytesThroughput = payload.getRecvBytesThroughput();
            double sendBytesThroughput = payload.getSendBytesThroughput();

            prioritys.add(new XStringSerializer.PriorityRank(localHostID, recvBytesThroughput + sendBytesThroughput));
        }
        //按理 应该是要一一对应的= =  prioritys 和  serviceInstances
        Collections.sort(prioritys);
        //生成 网关连接配置信息
        for (ServiceInstance<BindingAddressConfig> configInstance : serviceInstances) {
            XStringSerializer.PriorityRank isMe = null;
            for (int i = 0; i < prioritys.size(); i++) {
                XStringSerializer.PriorityRank priorityRank = prioritys.get(i);
                if (priorityRank.getLocalHostID() == configInstance.getPayload().getLocalHostID()) {
                    isMe = priorityRank;
                    isMe.setPriority(i);
                    break;
                }

            }
            connections.add(
                    new NetworkConnection(
                            configInstance.getPayload().getLocalHostID(),
                            configInstance.getPayload().getHost(), configInstance.getPayload().getPort(),
                            (isMe != null ? isMe.getPriority() : -1)
                    ));
        }
        clientConnection.setConnections(connections);
        //修改优先级为-1的情况
        if(prioritys.size()!=connections.size()){
            int priority = prioritys.size();
            for(int i = 0 ;i<connections.size();i++){
                NetworkConnection networkConnection = connections.get(i);
                if(networkConnection.getPriority()==-1)
                    networkConnection.setPriority(priority++);
            }
        }


        return clientConnection;
    }

    private int generateHostID() throws Exception {
        Stat stat1 = client.setData().forPath("/gateway");
        return stat1.getVersion();
    }
}
