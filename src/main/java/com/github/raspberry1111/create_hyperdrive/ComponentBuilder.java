package com.github.raspberry1111.create_hyperdrive;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;

public class ComponentBuilder {
    public static LangBuilder builder() {
        return CreateLang.builder(CreateHyperdrive.MODID);
    }

    public static LangBuilder translate(final String langKey, final Object... args) {
        return builder().translate(langKey, args);
    }
}
