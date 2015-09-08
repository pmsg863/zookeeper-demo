package com.xmgps.yun.config.demo;

import com.thoughtworks.xstream.XStream;
import com.xmgps.yun.communication.ypt.client.GatewayClient;
import com.xmgps.yun.configuration.config.BindingAddressConfig;
import com.xmgps.yun.configuration.config.ClientConnectionConfig;
import com.xmgps.yun.configuration.param.NetworkConnection;
import com.xmgps.yun.configuration.param.NetworkConnections;

import java.util.List;

/**
 * Created by huangwb on 2015/4/2.
 */
public class ClientDiscovery implements ServiceDiscovery.Notify{

    ServiceDiscovery serviceDiscovery ;

    ClientRegistry clientRegistry;

    GatewayClient client;

    public ClientDiscovery(String registryAddress) {
        serviceDiscovery = new ServiceDiscovery(registryAddress);
        clientRegistry = new ClientRegistry(registryAddress);
        serviceDiscovery.setNotify(this);
    }


    public ClientConnectionConfig discoveryClientConnection( ){
        List<String> discover = serviceDiscovery.discover();
        return setPriority(discover);
    }

    public ClientConnectionConfig setPriority(List<String> dataList){
        ClientConnectionConfig clientConnection = new ClientConnectionConfig("test");
        NetworkConnections connections = new NetworkConnections();
        XStream xStream = new XStream();
        xStream.processAnnotations(BindingAddressConfig.class);
        for(String add : dataList){
            BindingAddressConfig address = ( (BindingAddressConfig) xStream.fromXML(add) ) ;
            //todo priority
            connections.add( new NetworkConnection(address.getLocalHostID(),address.getHost(),address.getPort(),0));
            System.out.println("Connect "+address.getHost()+" "+address.getPort());
        }
        clientConnection.setConnections(connections);
        //TODO OBJECT
        List<String> priority = clientRegistry.discover();
        if(priority==null || priority.size()<=0){
            clientConnection.setLocalHostID(10011001);//TODO 自动生成
            for(int i = 0 ; i<clientConnection.getConnections().size();i++){
                NetworkConnection networkConnection = clientConnection.getConnections().get(i);
                networkConnection.setPriority(i);
            }
        }else{
            //[ID1,priorityx][ID2,priorityx]
            priority.get(0).split(",");

        }

        //cacl priority
        clientRegistry.register("test");

        return  null;
    }

    @Override
    public void process(List<String> dataList) {

        ClientConnectionConfig clientConnectionConfig = setPriority(dataList);
        client.setGatewayConnections(0,clientConnectionConfig.toString());

    }
}
