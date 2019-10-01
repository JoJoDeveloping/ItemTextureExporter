package de.jojomodding.itemtextureexport.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import de.jojomodding.itemtextureexport.ExportingScreen;
import de.jojomodding.itemtextureexport.ItemTextureExporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Processing {
    private final ItemTextureExporter mod;
    private ExportingScreen exportingScreen;
    private Predicate<ItemStack> oversize;
    private File outputFolder;
    private ItemTextureRenderer renderer;
    private Iterator<Map.Entry<ItemStack, Tuple<ResourceLocation, Integer>>> toProcess;
    private int alreadyProcessed;
    private float totalNumber;
    private JsonArray descriptorList;

    public Processing(ExportingScreen exportingScreen, ItemTextureExporter mod) {
        this.exportingScreen = exportingScreen;
        this.mod = mod;
        try {
            int texSize = Integer.parseInt(exportingScreen.getTextureSize());
            outputFolder = new File(Minecraft.getInstance().gameDir, exportingScreen.getOutputFolder());
            if (outputFolder.isFile() || !(outputFolder.mkdirs() || outputFolder.isDirectory())) {
                throw new IOException(I18n.format("gui.itemtextureexporter.error.notadir", exportingScreen.getOutputFolder()));
            }
            Pattern pattern = Pattern.compile(exportingScreen.getRegistryNameRegex());
            Pattern overPattern = Pattern.compile(exportingScreen.getOversizeRegex());
            oversize = is -> overPattern.asPredicate().test(is.getItem().getRegistryName().toString());
            renderer = new ItemTextureRenderer(Minecraft.getInstance().getItemRenderer(), texSize);
            alreadyProcessed = 0;

            Set<ItemStack> toRender = stacksToRender();
            //putting this whole thing in a single expression overwhelms the type checker
            Map<ResourceLocation, Set<WrappedItemStack>> intermediary = toRender.stream().
                    filter(is -> pattern.asPredicate().test(is.getItem().getRegistryName().toString())).
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

            descriptorList = new JsonArray();
            exportingScreen.setProcessing(this);
            exportingScreen.receiveUpdate(0);
        } catch (NumberFormatException e) {
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.notanum", exportingScreen.getTextureSize()));
        } catch (IOException e) {
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.io", e.getMessage()));
        } catch (PatternSyntaxException e) {
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.notaregex", e.getMessage()));
        }
    }

    private void processSingeItemstack(Map.Entry<ItemStack, Tuple<ResourceLocation, Integer>> stackData) throws IOException {
        final ResourceLocation name = stackData.getValue().getA();
        final int subscript = stackData.getValue().getB();
        final ItemStack stack = stackData.getKey();
        final String fqName = name.toString() + (subscript == 0 ? "" : "_" + subscript);
        final boolean oversized = oversize.test(stack);
//        final BufferedImage bi = new BufferedImage(texSize, texSize, BufferedImage.TYPE_4BYTE_ABGR);

        mod.getLogger().debug("Exporting Itemstack " + fqName + " with NBT:" + stackData.getKey().getTag());


        File outputResLoc = new File(outputFolder, name.getNamespace());
        if (!outputResLoc.mkdir() && !outputResLoc.isDirectory())
            throw new IOException(I18n.format("gui.itemtextureexporter.error.notadir", outputResLoc.getPath()));
        File outputFile = new File(outputResLoc, fqName.substring(fqName.indexOf(':') + 1) + ".png");
        renderer.renderItemstack(stack, outputFile, oversized);
//        ImageIO.write(bi, "png", outputFile);
        JsonObject object = new JsonObject();
        object.addProperty("registry_name", name.toString());
        object.addProperty("file_name", fqName);
        if (stack.getTag() != null) {
            JsonElement tagSerialized = new JsonParser().parse(stack.getTag().toString());
            object.add("nbt", tagSerialized);
        }
        object.addProperty("display_name_raw", stack.getDisplayName().getUnformattedComponentText());
        object.add("display_name", ITextComponent.Serializer.toJsonTree(stack.getDisplayName()));
        object.add("tooltip", stack.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL).stream().map(ITextComponent.Serializer::toJsonTree).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        object.addProperty("oversized", oversized);
        descriptorList.add(object);
    }

    public void continueProcessing() {
        Minecraft.getInstance().getProfiler().startSection("Item Texture Export");
        try {
            //take half a tick exporting items
            long stopTime = System.nanoTime() + 25_000_000L;
            while (System.nanoTime() < stopTime) {
                if (!toProcess.hasNext()) {
                    stopProcessing(alreadyProcessed);
                    Minecraft.getInstance().getProfiler().endSection();
                    return;
                }
                processSingeItemstack(toProcess.next());
                exportingScreen.receiveUpdate(100 * (alreadyProcessed++) / totalNumber);
            }
        } catch (IOException e) {
            stopProcessing(0);
            exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.error.io", e.getMessage()));
        }
        Minecraft.getInstance().getProfiler().endSection();
    }

    private void stopProcessing(int totalImgs){
        exportingScreen.setProcessing(false);
        exportingScreen.setStatus(I18n.format("gui.itemtextureexporter.processing.done", totalImgs));
        renderer.close();
        try {
            JsonObject main = new JsonObject();
            main.add("exports", descriptorList);
            JsonWriter jw = new JsonWriter(new FileWriter(new File(outputFolder, "export.json")));
            jw.setIndent("  ");
            Streams.write(main, jw);
            jw.flush();
            jw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Set<ItemStack> stacksToRender() {
        return Stream.concat(
                ForgeRegistries.ITEMS.getEntries().stream().
                        map(Map.Entry::getValue).
                                             map(ItemStack::new),
                allCreativeTabs()).collect(Collectors.toSet());
    }

    private static Stream<ItemStack> allCreativeTabs() {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemGroup group : ItemGroup.GROUPS) {
            if (ItemGroup.HOTBAR == group || ItemGroup.SEARCH == group) {
                continue;
            }
            group.fill(list);
        }
        return list.stream();
    }

}
