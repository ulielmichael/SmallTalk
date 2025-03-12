package mu;



import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;

@Push
@PWA(name = "SmallTalk", shortName = "ST")
public class AppShell implements AppShellConfigurator {
}

