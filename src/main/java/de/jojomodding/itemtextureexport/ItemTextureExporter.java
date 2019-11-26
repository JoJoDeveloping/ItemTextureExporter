package de.jojomodding.itemtextureexport;

import com.mojang.blaze3d.platform.GLX;
import de.jojomodding.itemtextureexport.gui.ExportingScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("itemtextureexporter")
public class ItemTextureExporter
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public ItemTextureExporter() {
        //Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        DistExecutor.callWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            LOGGER.error("The Item Render Exporter mod can only work on the client because the server does not have the required assets!");
            return null;
        });
        DistExecutor.callWhenOn(Dist.CLIENT, () -> () -> {
            if(!GLX.isUsingFBOs()){
                LOGGER.error("The Item Render Exporter mod requires FBOs, your PC doesn't support them.");
                return null;
            }

            ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (minecraft, screen) -> new ExportingScreen(minecraft, screen, this));

            //ugly stuff until displaying the mod config works properly
            ModInfo mi = ModList.get().getMods().stream().filter(min -> min.getModId().equals("itemtextureexporter")).findAny().orElseThrow(() -> new IllegalStateException("We couldn't find ourselves in the mods list!"));
            List<ModInfo> mli = ModList.get().getMods();
            for(int i = 0; i < mli.size(); i++){
                if(mli.get(i) == mi)
                    mli.set(i, new ModInfo(mi.getOwningFile(), mi.getModConfig()){
                        @Override
                        public boolean hasConfigUI() {
                            return true;
                        }
                    });
            }
            return null;
        });
    }

    public Logger getLogger() {
        return LOGGER;
    }

}
