package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NBTToDictionary;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.TagCompound")
public class TagCompoundTypeHandler implements VariableTypeHandler<TagCompound> {
    private static HashMap<String, MemberWrapper<TagCompound>> members = new HashMap<>();

    static {
        try {
            members.put("merge", new MethodWrapper<>(TagCompound.class.getMethod("merge", TagCompound.class)));
            members.put("remove", new MethodWrapper<>(TagCompound.class.getMethod("remove", String.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(TagCompound object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(object.contains(member)) return object.get(member);
        if(member.equals("toDictionary")) {
            return new MethodWrapper<TagCompound>("toDictionary", ((instance, params) -> NBTToDictionary.convert(instance, pattern, ctx))).createForInstance(object);
        }
        MemberWrapper<TagCompound> result = members.get(member);
        if(result == null) return null;
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(TagCompound object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        String key = assertOfType(index, pattern, ctx, String.class);
        if(object.contains(key)) return object.get(key);
        else return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(TagCompound object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(TagCompound object) {
        return object.getAllTags().stream().map(t -> {
            DictionaryObject entry = new DictionaryObject();
            entry.put("key", t.getName());
            entry.put("value", t);
            return entry;
        }).collect(Collectors.toList()).iterator();
    }
}
