package blockchain.net.visualization;

import blockchain.config.Configuration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class VisualizationClient {

    private final int visualizationServerPort;
    private final URL url;

    public VisualizationClient() throws MalformedURLException {
        this.visualizationServerPort = Configuration.getInstance().getVisualizationPort();
        this.url = new URL("http://localhost:" + visualizationServerPort);
        System.out.println(visualizationServerPort);
    }

    public void postToServer(String urlParameters) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length",
                    Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            System.out.println(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
