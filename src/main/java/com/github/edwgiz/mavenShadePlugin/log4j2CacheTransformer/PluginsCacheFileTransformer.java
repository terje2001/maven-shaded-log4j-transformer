package com.github.edwgiz.mavenShadePlugin.log4j2CacheTransformer;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.config.plugins.processor.PluginCache;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE;

public class PluginsCacheFileTransformer implements ResourceTransformer {

    ArrayList<File> tempFiles = new ArrayList<File>();


    @Override
    public boolean canTransformResource(String resource) {
        return resource != null && PLUGIN_CACHE_FILE.equals(resource);
    }


    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
        final File tempFile = File.createTempFile("Log4j2Plugins", "dat");
        FileOutputStream fos = new FileOutputStream(tempFile);
        try {
            IOUtils.copyLarge(is, fos);
        } finally {
            fos.close();
        }
        tempFiles.add(tempFile);
    }


    @Override
    public boolean hasTransformedResource() {
        return tempFiles.size() > 1;
    }


    @Override
    public void modifyOutputStream(JarOutputStream jos) throws IOException {
        try {
            PluginCache aggregator = new PluginCache();
            aggregator.loadCacheFiles(getUrls());
            jos.putNextEntry(new JarEntry(PLUGIN_CACHE_FILE));
            aggregator.writeCache(new CloseShieldOutputStream(jos));
        } finally {
            for (File tempFile : tempFiles) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }


    protected Enumeration<URL> getUrls() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (File tempFile : tempFiles) {
            final URL url = tempFile.toURI().toURL();
            urls.add(url);
        }
        return Collections.enumeration(urls);
    }
}