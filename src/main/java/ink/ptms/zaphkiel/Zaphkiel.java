package ink.ptms.zaphkiel;

import io.izzel.taboolib.loader.Plugin;
import io.izzel.taboolib.module.config.TConfig;
import io.izzel.taboolib.module.inject.TInject;
import io.izzel.taboolib.module.inject.TSchedule;
import io.izzel.taboolib.module.locale.logger.TLogger;

import java.io.File;

/**
 * @Author sky
 * @Since 2019-12-15 20:09
 */
@Plugin.Version(5.13)
public class Zaphkiel extends Plugin {

    @TInject
    public static final TLogger LOGS = null;
    @TInject
    public static final TConfig CONF = null;

    public static final ZaphkielAPI API = ZaphkielAPI.INSTANCE;

    @TSchedule(delay = 20)
    public static void reload() {
        if (!API.getFolderItem().exists()) {
            getPlugin().saveResource("item/def.yml", true);
        }
        if (!API.getFolderDisplay().exists()) {
            getPlugin().saveResource("display/def.yml", true);
        }
        ZaphkielAPI.INSTANCE.reloadItem();
        ZaphkielAPI.INSTANCE.reloadDisplay();
    }
}
