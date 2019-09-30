package de.jojomodding.itemtextureexport.render;

import de.jojomodding.itemtextureexport.ExportingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Processing {

    private static final Logger LOGGER = Logger.getLogger("ItemTextureExporter");

    private ExportingScreen exportingScreen;
    private int texSize;
    private File outputFolder;
    private ItemTextureRenderer renderer;
    private Iterator<Map.Entry<ItemStack, Tuple<ResourceLocation, Integer>>> toProcess;
    private int alreadyProcessed;
    private float totalNumber;

    public Processing(ExportingScreen exportingScreen) {
        this.exportingScreen = exportingScreen;
        try {
            texSize = Integer.parseInt(exportingScreen.getTextureSize());
            outputFolder = new File(Minecraft.getInstance().gameDir, exportingScreen.getOutputFolder());
            if (outputFolder.isFile() || !(outputFolder.mkdirs() || outputFolder.isDirectory())) {
                throw new IOException(I18n.format("gui.itemtextureexporter.error.notadir", exportingScreen.getOutputFolder()));
            }
            exportingScreen.setProcessing(true);
            exportingScreen.receiveUpdate(0);
            renderer = new ItemTextureRenderer(Minecraft.getInstance().getItemRenderer());
            Set<ItemStack> toRender = exportingScreen.stacksToRender();
            //putting this whole thing in 1 expression overwhelmes the type checker
            Map<ResourceLocation, Set<WrappedItemStack>> intermediary = toRender.stream().
                    map(WrappedItemStack::new).
                    collect(Collector.of(HashMap::new,
                                         (map, is) -> map.computeIfAbsent(is.stack.getItem().getRegistryName(), $ -> new HashSet<>()).add(is),
                                         (map1, map2) -> {map1.forEach((k,v) -> map2.computeIfAbsent(k, $ -> new HashSet<>()).addAll(v)); return map2;}));

            toProcess = intermediary.entrySet().stream().
                    flatMap(e -> {
                        int i = 0;
                        Set<Tuple<ItemStack, Tuple<ResourceLocation, Integer>>> result = new HashSet<>();
                        for (WrappedItemStack is : e.getValue()) {
                            result.add(new Tuple<>(is.stack, new Tuple<>(e.getKey(), i++)));
                        };
                        return result.stream();
                    }).
                    collect(Collectors.toMap(Tuple::getA, Tuple::getB)).entrySet().iterator();
            totalNumber = intermediary.values().stream().mapToInt(Set::size).sum();
            alreadyProcessed = 0;
        } catch (NumberFormatException e) {
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.notanum", exportingScreen.getTextureSize()));
        } catch (IOException e) {
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.io", e.getMessage()));
        }
    }

    private void processSingeItemstack(Map.Entry<ItemStack, Tuple<ResourceLocation, Integer>> stackData) throws IOException {
        LOGGER.info("Exporting Itemstack " + stackData.getValue().getA() + "_" + stackData.getValue().getB() + ": " + stackData.getKey().toString() + " " + Objects.toString(stackData.getKey().getTag()));
        BufferedImage bi = new BufferedImage(texSize, texSize, BufferedImage.TYPE_4BYTE_ABGR);
        renderer.renderItemstack(stackData.getKey(), bi, false);
        File outputResLoc = new File(outputFolder, stackData.getValue().getA().getNamespace());
        if (!outputResLoc.mkdir() && !outputResLoc.isDirectory())
            throw new IOException(I18n.format("gui.itemtextureexporter.error.notadir", outputResLoc.getPath()));
        int i = stackData.getValue().getB();
        ImageIO.write(bi, "png", new File(outputResLoc, stackData.getValue().getA().getPath() + (i == 0 ? "" : "_" + i) + ".png"));
    }

    public void continueProcessing() {
        try {
            for (int i = 0; i < 1; i++) {
                if (!toProcess.hasNext()) {
                    stopProcessing(alreadyProcessed);
                    return;
                }
                processSingeItemstack(toProcess.next());
                exportingScreen.receiveUpdate(100 * (alreadyProcessed++) / totalNumber);
            }
        } catch (IOException e) {
            stopProcessing(0);
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.io", e.getMessage()));
        }
    }

    private void stopProcessing(int totalImgs){
        exportingScreen.setProcessing(false);
        exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.processing.done", totalImgs));
    }

}
