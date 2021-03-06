package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class AliasType extends Type {

    @NotNull
    private Namespace realNamespace;
    @NotNull
    private String realName;

    public AliasType(@NotNull String category, Namespace namespace, @NotNull String name, @NotNull Namespace realNamespace, @NotNull String realName) {
        super(category, namespace, name);
        this.realNamespace = realNamespace;
        this.realName = realName;
    }

    public Namespace getAliasNamespace() {
        return this.namespace;
    }

    public String getAliasName() {
        return this.name;
    }

    public Type getRealType() {
        return realNamespace.types.getDictionary(this.category).get(realName);
    }

    @Override
    public boolean useNamespace() {
        return getRealType().useNamespace();
    }

    @Override
    public @NotNull String getName() {
        return getRealType().getName();
    }

    @Override
    public Namespace getNamespace() {
        return getRealType().getNamespace();
    }

    @Override
    public @NotNull String getCategory() {
        return super.getCategory();
    }

    @Override
    public void putProperty(String key, String value) {
        getRealType().putProperty(key, value);
    }

    @Override
    public void putProperties(HashMap<String, String> properties) {
        getRealType().putProperties(properties);
    }

    @Override
    public String getProperty(String key) {
        return getRealType().getProperty(key);
    }

    @Override
    public @NotNull HashMap<String, String> getProperties() {
        return getRealType().getProperties();
    }

    @Override
    public boolean isStandalone() {
        return getRealType().isStandalone();
    }

    @Override
    public String toString() {
        return getRealType().toString();
    }
}
