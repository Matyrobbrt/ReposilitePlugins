package com.matyrobbrt.reposiliteplugins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.reposilite.journalist.Logger;
import com.reposilite.storage.api.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Utils {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    public static GavData getData(String repoName, Location location) {
        final var elements = location.toString().split("/");

        if (elements.length >= 2) {
            final var name = elements[elements.length - 2];
            final var version = elements[elements.length - 1];
            List<String> groups = new ArrayList<>();
            for (int i = elements.length - 3; i >= 0; i--) {
                final var group = elements[i];
                if (repoName.equals(group)) {
                    break;
                }
                if (groups.isEmpty()) {
                    groups.add(group);
                } else {
                    groups.add(0, group);
                }
            }
            return new GavData(String.join(".", groups), name, version);
        }
        return null;
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static final class GavData {
        public GavData(String group, String name, String version) {
            this.group = group;
            this.name = name;
            this.version = version;
        }

        public final String group, name, version;
    }

    public static <T> BiConsumer<? super T, ? super Throwable> loggingErrorHandler(Logger logger) {
        return ($, e) -> {
            if (e != null) {
                logger.error("Encountered exception inside CompletableFuture: ", e);
            }
        };
    }
}
