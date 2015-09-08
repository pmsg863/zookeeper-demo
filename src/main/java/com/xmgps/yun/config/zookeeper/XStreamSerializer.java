package com.xmgps.yun.config.zookeeper;

import com.thoughtworks.xstream.XStream;
import com.xmgps.yun.configuration.core.ConfigBase;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;

import java.util.Map;

/**
 * Created by huangwb on 2015/4/24.
 */
public class XStreamSerializer implements InstanceSerializer {

    private static XStream xstream = new XStream();

    static {
        try {
            Map<String, Class> stringClassMap = ReflectHelper.loadAllParsers("com.xmgps");
            for (Class configClass : stringClassMap.values())
                xstream.processAnnotations(configClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getXml(Object obj) {
        return xstream.toXML(obj);
    }

    @Override
    public byte[] serialize(ServiceInstance serviceInstance) throws Exception {
        String xml = xstream.toXML(serviceInstance.getPayload());
        return xml.getBytes();
    }

    @Override
    public ServiceInstance deserialize(byte[] bytes) throws Exception {
        ConfigBase value = (ConfigBase) xstream.fromXML(new String(bytes));
        return ServiceInstance.<ConfigBase>builder()
                .name(value.getName())
                .payload(value)
                .build();
    }

}
