import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import java.lang.reflect.Method;
public class TestPE {
    public static void main(String[] args) throws Exception {
        for (Method m : ProtocolPacketEvent.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("player") || m.getName().toLowerCase().contains("user")) {
                System.out.println(m);
            }
        }
    }
}
