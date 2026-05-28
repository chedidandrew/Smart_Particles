package sp;

import net.fabricmc.api.ClientModInitializer;

public class SPMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SPConfig.load();
	}
}