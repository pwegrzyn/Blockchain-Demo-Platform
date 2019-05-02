import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class App {

    public static void main(String[] args) {

        Properties properties = loadProperties();

    }

    private static Properties loadProperties() {
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.err.println("Unable to find config.properties!");
                return null;
            }
            prop.load(input);
            return  prop;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
