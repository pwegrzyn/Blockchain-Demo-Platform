package blockchain.util;

public class Utils {

    public static String generateRandomString(int length) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder();
        int count = length;
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public static String bytesToHexStr(byte[] in) {
        StringBuilder sb = new StringBuilder();
        for (byte b : in) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }
            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }
            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

}
