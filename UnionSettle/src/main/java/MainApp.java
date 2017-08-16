import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unionpay.UnionPay;

import java.io.IOException;
import java.util.Properties;

public class MainApp {
    Logger logger = LoggerFactory.getLogger(MainApp.class);
    public static void Main(String[] args) {
        Properties props = new Properties();
        String merId = props.getProperty("merId");
        String outPutDirectory = props.getProperty("outPutDirectory");
        try {
            props.load(MainApp.class.getClassLoader().getResourceAsStream("alipay.properties"));
            if (args.length > 1) {
                UnionPay.settleFileDownLoad(merId,args[0].toString().trim(),outPutDirectory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
