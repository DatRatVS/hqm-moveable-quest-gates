package datrat.hqmmoveableqgs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import datrat.hqmmoveableqgs.piston.HqmPortalHooks;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = HQMMoveableQgs.MODID,
    name = HQMMoveableQgs.NAME,
    version = HQMMoveableQgs.VERSION,
    dependencies = "required-after:HardcoreQuesting"
)
public final class HQMMoveableQgs {
    public static final String MODID = "hqmmoveableqgs";
    public static final String NAME = "Hardcore Questing Mode: Moveable Quest Gates";
    public static final String VERSION = "1.0.0";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        HqmPortalHooks.registerPendingRestoreTicker();
        logger.info("Loaded {}", NAME);
    }
}
