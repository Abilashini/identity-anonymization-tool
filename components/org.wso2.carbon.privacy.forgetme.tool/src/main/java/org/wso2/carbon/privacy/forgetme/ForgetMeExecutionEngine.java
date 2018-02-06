package org.wso2.carbon.privacy.forgetme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.privacy.forgetme.api.runtime.Environment;
import org.wso2.carbon.privacy.forgetme.api.runtime.ForgetMeInstruction;
import org.wso2.carbon.privacy.forgetme.api.runtime.ForgetMeResult;
import org.wso2.carbon.privacy.forgetme.api.runtime.InstructionExecutionException;
import org.wso2.carbon.privacy.forgetme.api.runtime.InstructionReader;
import org.wso2.carbon.privacy.forgetme.api.runtime.ModuleException;
import org.wso2.carbon.privacy.forgetme.api.runtime.ProcessorConfig;
import org.wso2.carbon.privacy.forgetme.api.user.UserIdentifier;
import org.wso2.carbon.privacy.forgetme.config.InstructionReaderConfig;
import org.wso2.carbon.privacy.forgetme.config.SystemConfig;
import org.wso2.carbon.privacy.forgetme.processor.ForgetMeCompositeResult;
import org.wso2.carbon.privacy.forgetme.runtime.ForgetMeExecutionException;
import org.wso2.carbon.privacy.forgetme.runtime.SystemEnv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes Forget Me tasks.
 *
 */
public class ForgetMeExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(ForgetMeExecutionEngine.class);
    private static final String EXECUTOR_THREAD_PREFIX = "ProcessorExec-";
    private SystemConfig systemConfig;
    private Map<String, ExecutorService> executors = new HashMap<>();
    private Set<Future<ForgetMeResult>> submittedJobs = new HashSet<>();
    private UserIdentifier userIdentifier;
    private Environment systemEnv;

    public ForgetMeExecutionEngine(UserIdentifier userIdentifier, Environment systemEnv, SystemConfig systemConfig) {

        this.systemConfig = systemConfig;
        this.userIdentifier = userIdentifier;
        this.systemEnv = systemEnv;
    }

    /**
     * Executes the engine.
     * This will start multiple processors in parallel threads.
     *
     * @return
     * @throws ForgetMeExecutionException
     */
    public ForgetMeResult execute() throws ForgetMeExecutionException {

        ForgetMeCompositeResult forgetMeResult = new ForgetMeCompositeResult();
        createExecutors();

        startExecutors();
        waitForCompletion(forgetMeResult);
        return forgetMeResult;
    }

    /**
     * Waits for the completion of all the processors.
     *
     * @param compositeResult
     */
    private void waitForCompletion(ForgetMeCompositeResult compositeResult) {

        for (ExecutorService executorService : executors.values()) {
            //Causes shutdown when the current job finishes.
            executorService.shutdown();
        }

        for (Future<ForgetMeResult> future : submittedJobs) {
            try {
                ForgetMeResult result = future.get(1, TimeUnit.HOURS);
                compositeResult.addEntry(result);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.error("Interrupted while executing the processor thread : " + future, e);
            }
        }
        log.info("All processors has been properly shut-down");
    }

    private void startExecutors() throws ForgetMeExecutionException {

        for (String processorName : executors.keySet()) {
            List<ForgetMeInstruction> instructions = getInstructions(processorName, systemEnv);
            ProcessorConfig processorConfig = systemConfig.getProcessorConfigMap().get(processorName);
            ProcessorPipeline processorPipeline = new ProcessorPipeline(userIdentifier, processorConfig, instructions);
            ExecutorService executorService = executors.get(processorName);
            Future<ForgetMeResult> future = executorService.submit(processorPipeline);
            submittedJobs.add(future);
        }
    }

    private List<ForgetMeInstruction> getInstructions(String processorName, Environment environment)
            throws ForgetMeExecutionException {

        List<ForgetMeInstruction> result = new ArrayList<>();
        for (Map.Entry<Path, InstructionReaderConfig> entry : systemConfig.getDirectoryToInstructionReaderMap()
                .entrySet()) {
            InstructionReaderConfig readerConfig = entry.getValue();
            InstructionReader reader = readerConfig.getInstructionReader();
            Path path = entry.getKey();
            if (reader.getType().equals(processorName)) {
                List<ForgetMeInstruction> instructions = null;
                try {
                    instructions = reader.read(path, readerConfig.getProperties(), environment);
                } catch (ModuleException e) {
                    throw new ForgetMeExecutionException(
                            "Unable to get instructions for the processor : " + processorName, e);
                }
                result.addAll(instructions);
            }
        }

        return result;
    }

    private void createExecutors() {

        for (String name : systemConfig.getProcessors()) {
            ExecutorService executorService = Executors
                    .newSingleThreadExecutor(new SimpleThreadFactory(EXECUTOR_THREAD_PREFIX + name));
            executors.put(name, executorService);
        }
    }

    private static class SimpleThreadFactory implements ThreadFactory {

        private String threadNamePrefix;

        public SimpleThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, threadNamePrefix);
        }
    }

    /**
     * Class implements instruction execution thread.
     * Each processor has its own instruction pipeline.
     * Pipelines can run in parallel.
     * Instructions in each pipeline executes sequentially.
     *
     */
    private static class ProcessorPipeline implements Callable<ForgetMeResult> {

        private ProcessorConfig processorConfig;
        private List<ForgetMeInstruction> instructionList = new ArrayList<>();
        private UserIdentifier userIdentifier;

        public ProcessorPipeline(UserIdentifier userIdentifier, ProcessorConfig processorConfig,
                List<ForgetMeInstruction> instructionList) {

            this.userIdentifier = userIdentifier;
            this.processorConfig = processorConfig;
            this.instructionList = instructionList;
        }

        @Override
        public ForgetMeResult call() throws InstructionExecutionException {

            ForgetMeCompositeResult forgetMeResult = new ForgetMeCompositeResult();
            Environment environment = new SystemEnv();

            for (ForgetMeInstruction instruction : instructionList) {
                instruction.execute(userIdentifier, processorConfig, environment);
            }
            return forgetMeResult;
        }
    }
}
