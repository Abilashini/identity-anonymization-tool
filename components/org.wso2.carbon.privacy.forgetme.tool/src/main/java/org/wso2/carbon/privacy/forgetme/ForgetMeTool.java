package org.wso2.carbon.privacy.forgetme;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.wso2.carbon.privacy.forgetme.api.runtime.Environment;
import org.wso2.carbon.privacy.forgetme.api.runtime.ForgetMeResult;
import org.wso2.carbon.privacy.forgetme.api.user.UserIdentifier;
import org.wso2.carbon.privacy.forgetme.config.SystemConfig;
import org.wso2.carbon.privacy.forgetme.runtime.ForgetMeExecutionException;
import org.wso2.carbon.privacy.forgetme.runtime.SystemEnv;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Processes the forget Me request from the external user.
 * Delegates the forget me tasks to different subsystems.
 *
 */
public class ForgetMeTool {

    private static final String CMD_OPTION_CONFIG_DIR = "d";
    private static final String CMD_OPTION_CONFIG_CARBON_HOME = "carbon";
    private static final String CMD_OPTION_CONFIG_USER_NAME = "U";
    private static final String CMD_OPTION_CONFIG_USER_DOMAIN = "D";
    private static final String CMD_OPTION_CONFIG_TENANT_DOMAIN = "T";

    private static final String DEFAULT_TENANT_DOMAIN = "-1234";
    private static final String DEFAULT_USER_DOMAIN = "PRIMARY";

    private static final String COMMAND_NAME = "forget-me";
    private static final String CONFIG_FILE_NAME = "config.json";

    private ForgetMeExecutionEngine forgetMeExecutionEngine;

    public static void main(String[] args) throws Exception {

        Options options = new Options();

        options.addOption(CMD_OPTION_CONFIG_DIR, true, "Directory to scan");
        options.addOption(CMD_OPTION_CONFIG_CARBON_HOME, true, "Carbon Home");
        options.addOption(CMD_OPTION_CONFIG_USER_NAME, true, "User Name");
        options.addOption(CMD_OPTION_CONFIG_USER_DOMAIN, true, "User Domain");
        options.addOption(CMD_OPTION_CONFIG_TENANT_DOMAIN, true, "Tenant Domain");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String homeDir;
        if (cmd.hasOption(CMD_OPTION_CONFIG_DIR)) {
            homeDir = cmd.getOptionValue(CMD_OPTION_CONFIG_DIR);
        } else {
            printError(options);
            return;
        }
        UserIdentifier userIdentifier;
        if (cmd.hasOption(CMD_OPTION_CONFIG_USER_NAME)) {
            String userName = cmd.getOptionValue(CMD_OPTION_CONFIG_USER_NAME);
            String domainName = cmd.getOptionValue(CMD_OPTION_CONFIG_USER_DOMAIN, DEFAULT_USER_DOMAIN);
            String tenantName = cmd.getOptionValue(CMD_OPTION_CONFIG_TENANT_DOMAIN, DEFAULT_TENANT_DOMAIN);
            userIdentifier = createUserIdentifier(userName, domainName, tenantName);
        } else {
            printError(options);
            return;
        }
        ForgetMeTool forgetMeTool = new ForgetMeTool();
        forgetMeTool.process(homeDir, userIdentifier);
    }

    private static void printError(Options options) {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(COMMAND_NAME, options);
    }

    public ForgetMeResult process(String homeDir, UserIdentifier userIdentifier) throws ForgetMeExecutionException {

        ForgetMeResult forgetMeResult;
        ConfigReader configReader = ConfigReader.getInstance();
        Environment environment = new SystemEnv();
        try {
            File home = new File(homeDir).getAbsoluteFile().getCanonicalFile();
            SystemConfig systemConfig = configReader.readSystemConfig(new File(home, CONFIG_FILE_NAME));
            forgetMeExecutionEngine = new ForgetMeExecutionEngine(userIdentifier, environment, systemConfig);
            forgetMeResult = forgetMeExecutionEngine.execute();
        } catch (IOException e) {
            throw new ForgetMeExecutionException("Could not load config from directory: " + homeDir, e, "E_INIT", null);
        }
        return forgetMeResult;
    }

    private static UserIdentifier createUserIdentifier(String userName, String domainName, String tenantName) {

        UserIdentifier userIdentifier = new UserIdentifier();
        userIdentifier.setUsername(userName);
        userIdentifier.setUserStoreDomain(domainName);
        userIdentifier.setTenantDomain(tenantName);
        userIdentifier.setPseudonym(UUID.randomUUID().toString());
        return userIdentifier;
    }

}
