import com.esignal.jstandard.jna.DbcAPI;
import com.esignal.jstandard.managers.ResourceManagerFactory;

public final class LinuxNativeSmoke {
    public static void main(String[] args) throws Exception {
        if (!System.getProperty("os.name").equals("Linux")) {
            throw new IllegalStateException("This validation requires Linux");
        }
        if (DbcAPI.dbcapi == null) {
            throw new IllegalStateException("ICE native API failed to load");
        }
        var manager = ResourceManagerFactory.getFactory().createQuoteManager();
        if (manager == null) {
            throw new IllegalStateException("ICE QuoteManager initialization failed");
        }
        System.out.println("PASS: ICE native API loaded and QuoteManager created; no ICE connection attempted");
        System.exit(0);
    }
}
