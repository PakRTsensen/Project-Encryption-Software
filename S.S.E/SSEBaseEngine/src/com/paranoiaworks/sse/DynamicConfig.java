package com.paranoiaworks.sse;


/**
 * Configuration that requires some "Current State / System Depended Adjustment"
 *
 * @author Paranoia Works
 * @version 1.0.0
 */
public class DynamicConfig {

    private static final int MAX_CTR_PARALLELIZATION_PI = 8; // 1, 2, 4, 8, ...
    private static Integer cpuCores = null;
    private static Integer ctrParallelizationPI = null;

    public static synchronized int getCTRParallelizationPI()
    {
    	if(ctrParallelizationPI != null) return ctrParallelizationPI;
        else {
            int cores = getNumberOfCPUCores();
            if(cores >= 8) ctrParallelizationPI = 8;
            else if(cores >= 4) ctrParallelizationPI = 4;
            else if(cores >= 2) ctrParallelizationPI = 2;
            else ctrParallelizationPI = 1;
            if(MAX_CTR_PARALLELIZATION_PI < ctrParallelizationPI) ctrParallelizationPI = MAX_CTR_PARALLELIZATION_PI;
            return ctrParallelizationPI;
        }
    }

    public static synchronized int getNumberOfCPUCores()
    {

        if (cpuCores != null) return cpuCores;
        else {
            try {
            	cpuCores = Runtime.getRuntime().availableProcessors();
            } catch (Exception e) {
            	cpuCores = 1;
            }
        }
        if(cpuCores == null || cpuCores < 1) cpuCores = 1;
        return cpuCores;
    }
}
