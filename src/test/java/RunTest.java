import com.interezen.t24.redis_finder.MainProcess;

public class RunTest {
    public static void main(String[] args) throws InterruptedException {
        new MainProcess();
        while(true) {
            Thread.sleep(1000);
        }
    }
}
