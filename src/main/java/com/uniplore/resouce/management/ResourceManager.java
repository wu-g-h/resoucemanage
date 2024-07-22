package com.uniplore.resouce.management;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * 资源管理类，单例模式。
 */
public class ResourceManager {
    private int totalCpu;
    private int totalMemory;
    private int availableCpu;
    private int availableMemory;
    private static ResourceManager instance;

    private ResourceManager() {
        loadResourceConfig();
    }

    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    private void loadResourceConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            this.totalCpu = Integer.parseInt(properties.getProperty("cpu.total", "20"));
            this.totalMemory = Integer.parseInt(properties.getProperty("memory.total", "20480"));
            this.availableCpu = totalCpu;
            this.availableMemory = totalMemory;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized boolean allocateResources(int cpu, int memory) {
        if (availableCpu >= cpu && availableMemory >= memory) {
            availableCpu -= cpu;
            availableMemory -= memory;
            System.out.println("Resources allocated: CPU=" + cpu + ", Memory=" + memory + "MB");
            System.out.println("Remaining resources: CPU=" + availableCpu + ", Memory=" + availableMemory + "MB");
            checkResourceWarning();
            return true;
        } else {
            System.out.println("Insufficient resources to allocate: CPU=" + cpu + ", Memory=" + memory + "MB");
            return false;
        }
    }

    public synchronized void releaseResources(int cpu, int memory) {
        availableCpu += cpu;
        availableMemory += memory;
        System.out.println("释放资源: CPU=" + cpu + ", 内存=" + memory + "MB");
        System.out.println("剩余资源: CPU=" + availableCpu + ", 内存=" + availableMemory + "MB");
    }

    public synchronized int getAvailableCpu() {
        return availableCpu;
    }

    public synchronized int getAvailableMemory() {
        return availableMemory;
    }

    private void checkResourceWarning() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            int cpuThreshold = Integer.parseInt(properties.getProperty("resource.warning.threshold", "95"));
            int memoryThreshold = Integer.parseInt(properties.getProperty("resource.warning.threshold", "95"));

            int usedCpuPercentage = (int) ((1 - ((double) availableCpu / totalCpu)) * 100);
            int usedMemoryPercentage = (int) ((1 - ((double) availableMemory / totalMemory)) * 100);

            if (usedCpuPercentage >= cpuThreshold || usedMemoryPercentage >= memoryThreshold) {
                System.out.println("Resource warning: Available CPU=" + availableCpu + ", Available Memory=" + availableMemory + "MB");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
