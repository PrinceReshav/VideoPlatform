import com.platform.livetv.LiveTvServiceApplication;
import com.platform.livetv.service.IptvOrgSyncService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class SyncSmokeMain {
  public static void main(String[] args) {
    System.setProperty("server.port", "8095");
    System.setProperty("livetv.iptv-org.local-repo-path", "/tmp/v11/testiptv");
    System.setProperty("livetv.iptv-org.local-streams-path", "/tmp/v11/testiptv/streams");
    System.setProperty("livetv.health-check.enabled", "false");
    System.setProperty("livetv.sync.enabled", "true");
    ConfigurableApplicationContext ctx = SpringApplication.run(LiveTvServiceApplication.class);
    IptvOrgSyncService s = ctx.getBean(IptvOrgSyncService.class);
    s.syncChannels();
    s.syncChannels();
    System.out.println("DOUBLE_SYNC_SMOKE_OK");
    ctx.close();
  }
}
