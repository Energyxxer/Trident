package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers.SAMPLE_INT_RANGE;
import static com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers.SAMPLE_REAL_RANGE;

@ParserGroup
public interface VariableTypeHandler<T> {
    Object getMember(T object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol);

    Object getIndexer(T object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol);

    Object cast(T object, Class targetType, TokenPattern<?> pattern, TridentFile file);

    class Static {

        public static String getIdentifierForClass(Class<?> cls) {
            if(VariableMethod.class.isAssignableFrom(cls)) {
                return VariableMethod.class.getName();
            }
            if(Entity.class.isAssignableFrom(cls)) {
                return Entity.class.getName();
            }
            if(cls.isInstance(SAMPLE_INT_RANGE)) return cls.getName() + "<Integer>";
            if(cls.isInstance(SAMPLE_REAL_RANGE)) return cls.getName() + "<Double>";
            return cls.getName();
        }

        private static HashMap<String, Class> shorthands = new HashMap<>();

        static {
            shorthands.put("integer", Integer.class);
            shorthands.put("real", Double.class);
            shorthands.put("boolean", Boolean.class);
            shorthands.put("string", String.class);
            shorthands.put("entity", Entity.class);
            shorthands.put("block", Block.class);
            shorthands.put("item", Item.class);
            shorthands.put("text_component", TextComponent.class);
            shorthands.put("nbt", TagCompound.class);
            shorthands.put("nbt_value", NBTTag.class);
            shorthands.put("nbt_path", NBTPath.class);
            shorthands.put("coordinates", CoordinateSet.class);
            shorthands.put("resource", TridentUtil.ResourceLocation.class);

            //"coordinate", "integer_range", "real_range", "dictionary", "array"
        }

        public static Class getClassForShorthand(String shorthand) {
            return shorthands.get(shorthand);
        }
    }
}