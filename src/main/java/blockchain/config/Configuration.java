package blockchain.config;

import blockchain.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private static Configuration instance;

    private Mode nodeRunningMode;
    private String mcast_addr;
    private String version;
    private boolean shouldAutoGenerateKeys;
    private String publicKey;
    private String privateKey;
    private int visualizationPort;
    private String networkInterfaceName;

    private Configuration() {
        Properties properties = loadProperties();
        initMode(properties);
        initMcastAddr(properties);
        initVersion(properties);
        initVisualizationPort(properties);
        initNetworkInterfaceName(properties);
    }

    public static Configuration getInstance() {
        if (instance == null)
            instance = new Configuration();
        return instance;
    }

    public Mode getNodeRunningMode() {
        return nodeRunningMode;
    }

    public void setNodeRunningMode(Mode nodeRunningMode) {
        this.nodeRunningMode = nodeRunningMode;
    }

    public String getMcast_addr() {
        return mcast_addr;
    }

    public void setMcast_addr(String mcast_addr) {
        this.mcast_addr = mcast_addr;
    }

    public String getVersion() {
        return version;
    }

    public boolean isShouldAutoGenerateKeys() {
        return shouldAutoGenerateKeys;
    }

    public void setShouldAutoGenerateKeys(boolean shouldAutoGenerateKeys) {
        this.shouldAutoGenerateKeys = shouldAutoGenerateKeys;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public int getVisualizationPort() {
        return visualizationPort;
    }

    public void setVisualizationPort(int visualizationPort) {
        this.visualizationPort = visualizationPort;
    }

    public String getNetworkInterfaceName() {
        return this.networkInterfaceName;
    }

    private Properties loadProperties() {
        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                LOGGER.severe("Unable to find config.properties!");
                return null;
            }
            prop.load(input);
            LOGGER.config("Properties successfully loaded from config.properties.");
            return prop;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void initMode(Properties properties) {
        String nodeRunningModeStr = properties.getProperty("node.mode").toLowerCase().trim();
        switch (nodeRunningModeStr) {
            case "full":
                this.nodeRunningMode = Mode.FULL;
                break;
            case "wallet":
                this.nodeRunningMode = Mode.WALLET;
                break;
            default:
                LOGGER.warning("Invalid node.mode property value! Setting default mode: FULL");
                this.nodeRunningMode = Mode.FULL;
                break;
        }
    }

    private void initMcastAddr(Properties properties) {
        String mcastAddr = properties.getProperty("node.mcast_addr").toLowerCase().trim();
        if (Utils.validIP(mcastAddr)) {
            this.mcast_addr = mcastAddr;
        } else {
            LOGGER.severe("Invalid IP address property! Aborting initialization process...");
            System.exit(1);
        }
    }

    private void initVersion(Properties properties) {
        String version = properties.getProperty("version").toLowerCase().trim();
        if (Pattern.matches("\\d\\.\\d\\.\\d", version)) {
            this.version = version;
        } else {
            LOGGER.warning("Invalid version number provided. Setting default value (1.0.0).");
            this.version = "1.0.0";
        }
    }

    private void initVisualizationPort(Properties properties) {
        String port = properties.getProperty("vis.visualization_port").toLowerCase().trim();
        this.visualizationPort = Integer.parseInt(port);
    }

    private void initNetworkInterfaceName(Properties properties) {
        String intName = properties.getProperty("networkInterfaceName").trim();
        this.networkInterfaceName = intName;
    }

}
