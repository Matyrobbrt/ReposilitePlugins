package com.matyrobbrt.reposiliteplugins;

import com.reposilite.configuration.shared.SharedConfigurationFacade;
import com.reposilite.configuration.shared.api.SharedSettings;
import com.reposilite.plugin.Extensions;
import kotlin.jvm.JvmClassMappingKt;

public interface ConfigurationProvider<T extends SharedSettings> {
    T get();

    GeneralSettings getGeneralSettings();

    static <T extends SharedSettings> ConfigurationProvider<T> forFacade(Class<T> clazz, Extensions extensions) {
        final var provider = extensions.facade(SharedConfigurationFacade.class);
        final var general = provider.getDomainSettings(JvmClassMappingKt.getKotlinClass(GeneralSettings.class));
        final var specific = provider.getDomainSettings(JvmClassMappingKt.getKotlinClass(clazz));
        return new ConfigurationProvider<>() {
            @Override
            public T get() {
                return specific.get();
            }

            @Override
            public GeneralSettings getGeneralSettings() {
                return general.get();
            }
        };
    }
}
