import com.interezen.t24.redis_finder.MainProcess;
import org.junit.Test;

public class RunTest {
    @Test
    public void run() throws Exception {
        new com.interezen.t24.redis_finder.MainProcess();
        while (true) {
            Thread.sleep(1000);
        }
    }
}
