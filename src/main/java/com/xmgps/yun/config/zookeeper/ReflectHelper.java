package com.xmgps.yun.config.zookeeper;

import com.xmgps.yun.configuration.core.ConfigBase;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Created by huangwb on 2015/5/4.
 */
public class ReflectHelper {

    /**
     * 加载所有车辆监控调度消息体解析器
     * @throws Exception 解析器加载异常
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, Class> loadAllParsers(String packname)  {
        Map<String,Class> parsers = new HashMap<>();

        List<String> classList = getClassesInPackage(packname);
        for (String className : classList) {
            Class classType = null;
            try {
                classType = Class.forName(className);
            } catch (Throwable e) {
                System.out.println(e.toString());
               continue;
            }
            Class superclass = classType.getSuperclass();

            if( superclass!=null && superclass.getSimpleName().equals(ConfigBase.class.getSimpleName()) ){
                System.out.println(className);
                parsers.put(className,classType);
            }
        }
        return parsers;
    }

    /**
     * 获取指定包名下的所有类
     *
     * @param packageName 指定包名
     * @return 指定包名下的所有类
     */
    public static List<String> getClassesInPackage(String packageName) {
        List<String> classList = new ArrayList<String>();
        String packagePath = packageName.replace('.', '/');
        try {
            String delim = ":";
            if (System.getProperty("os.name").indexOf("Windows") != -1) {
                delim = ";";
            }
            String[] paths = System.getProperty("java.class.path").split(delim);

            for (String path : paths) {
                File classPath = new File(path);

                if (!classPath.exists()) {
                    continue;
                }
                if (classPath.isDirectory()) {
                    File dir = new File(classPath, packagePath);
                    if (!dir.exists()) {
                        continue;
                    }
                    for (File file : dir.listFiles()) {
                        // 只加载当前路径下的类
                        if (file.isFile()) {
                            String classFileName = file.getName();
                            if (classFileName.length() > 6 &&
                                    classFileName.substring(classFileName.length() - 6).equalsIgnoreCase(".class")) {
                                // 去掉".class"
                                String className = classFileName.substring(0, classFileName.length() - 6);
                                classList.add(packageName + "." + className);
                            }
                        }
                    }
                } else {
                    FileInputStream fis = new FileInputStream(classPath);
                    JarInputStream jis = new JarInputStream(fis, false);
                    JarEntry e = null;
                    while ((e = jis.getNextJarEntry()) != null) {
                        String entryName = e.getName();
                        if (entryName.startsWith(packagePath) &&
                                entryName.length() > 6 &&
                                entryName.substring(entryName.length() - 6).equalsIgnoreCase(".class")) {
                            // 替换"/"为"."，去掉".class"
                            classList.add(entryName.replace('/', '.').substring(0, entryName.length() - 6));
                        }
                        jis.closeEntry();
                    }
                    jis.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return classList;
    }

/*    public static void main(String[] args) throws Exception {
        ReflectHelper ins = new ReflectHelper();
        ins.loadAllParsers("com.xmgps");

        System.out.println("---------------------END---------------------");
    }*/
}
