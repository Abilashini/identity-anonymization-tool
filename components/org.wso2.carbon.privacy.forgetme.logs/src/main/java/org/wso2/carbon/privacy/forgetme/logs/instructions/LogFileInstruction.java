package org.wso2.carbon.privacy.forgetme.logs.instructions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.privacy.forgetme.api.report.ReportAppender;
import org.wso2.carbon.privacy.forgetme.api.runtime.Environment;
import org.wso2.carbon.privacy.forgetme.api.runtime.ForgetMeInstruction;
import org.wso2.carbon.privacy.forgetme.api.runtime.ForgetMeResult;
import org.wso2.carbon.privacy.forgetme.api.runtime.InstructionExecutionException;
import org.wso2.carbon.privacy.forgetme.api.runtime.ProcessorConfig;
import org.wso2.carbon.privacy.forgetme.api.user.UserIdentifier;
import org.wso2.carbon.privacy.forgetme.logs.beans.Patterns;
import org.wso2.carbon.privacy.forgetme.logs.processor.LogFileProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements forget-me log re-writing instruction.
 *
 */
public class LogFileInstruction implements ForgetMeInstruction {

    private static final Logger logger = LoggerFactory.getLogger(LogFileInstruction.class);

    private List<Patterns.Pattern> patterns;
    private File logFile;

    public LogFileInstruction(List<Patterns.Pattern> patterns, File logFile) {
        this.patterns = patterns;
        this.logFile = logFile;
    }

    @Override
    public ForgetMeResult execute(UserIdentifier userIdentifier, ProcessorConfig processorConfig,
            Environment environment) throws InstructionExecutionException {
        logger.info("Executing LogFileInstruction");
        List<File> logFiles = new ArrayList<>();
        logFiles.add(logFile);
        ReportAppender reportAppender = new ReportAppender() {

            @Override
            public void appendSection(String format, Object... data) {
                System.out.printf(format, data);
                System.out.println("-------------");
                System.out.println();

            }

            @Override
            public void append(String format, Object... data) {
                System.out.printf(format, data);
            }
        };
        LogFileProcessor.processFiles(userIdentifier, reportAppender, patterns, logFiles);
        return null;
    }
}
