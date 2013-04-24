package me.schiz.jmeter.functions;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * This is simple JMeter function for cache read files and increase performance of FileToString/StringFromFile
 * Author: schizophrenia Epikhin Mikhail
 */
public class ReadNCacheFunction extends AbstractFunction{
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final List<String> desc = new LinkedList<String>();
    private static final String KEY = "__ReadNCache";
    protected static int CAPACITY = 1000;
    protected static int CONCURRENCY_LEVEL = 16;

    protected static volatile ConcurrentLinkedHashMap<String, String> cache;

    static {
        desc.add("Filepath");
    }
    private Object[] values;

    public ReadNCacheFunction() {
        if (cache == null) {
            synchronized (this) {
                if(cache == null) {
                    CAPACITY = JMeterUtils.getPropDefault("jmeter.readncache.capacity", 1000); //default is 1000 files
                    CONCURRENCY_LEVEL = JMeterUtils.getPropDefault("jmeter.readncache.concurrency_level", 16); //default is 1000 files
                    cache = new ConcurrentLinkedHashMap.Builder<String, String>().maximumWeightedCapacity(CAPACITY).concurrencyLevel(CONCURRENCY_LEVEL).build();
                }
            }
        }
    }

    public synchronized String execute(SampleResult previousResult, Sampler currentSampler)
            throws InvalidVariableException {
        String filename = ((CompoundVariable) values[0]).execute();

        String key = cache.get(filename);
        if (key != null) return key;
        try {
            String value = FileUtils.readFileToString(new File(filename), "UTF-8");
            cache.put(filename, value);
            return value;
        } catch (IOException e) {
            log.warn("Could not read file: "+filename+" "+e.getMessage(), e);
            throw new JMeterStopThreadException("End of sequence", e);
        }
    }

    public synchronized void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkMinParameterCount(parameters, 1);
        values = parameters.toArray();
    }

    public String getReferenceKey() {
        return KEY;
    }

    public List<String> getArgumentDesc() {
        return desc;
    }

}
