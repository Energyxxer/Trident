package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.util.logger.Debug;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTParser {
    public static TagCompound parseCompound(TokenPattern<?> pattern) {
        if(pattern == null) return null;
        return (TagCompound)parseValue(pattern);
    }

    private static NBTTag parseValue(TokenPattern<?> pattern) {
        switch(pattern.getName()) {
            case "NBT_VALUE": {
                return parseValue(((TokenStructure)pattern).getContents());
            }
            case "NBT_COMPOUND": {
                TagCompound compound = new TagCompound();
                TokenList entries = (TokenList) pattern.find(".NBT_COMPOUND_ENTRIES");
                for(TokenPattern<?> inner : entries.getContents()) {
                    if(inner instanceof TokenGroup) {
                        String key = inner.find("NBT_KEY").flattenTokens().get(0).value;
                        if(key.startsWith("\"")) {
                            key = CommandUtils.parseQuotedString(key);
                        }
                        NBTTag value = parseValue(inner.find("NBT_VALUE"));
                        value.setName(key);
                        compound.add(value);
                    }
                }
                Debug.log(pattern.getContents());
                return compound;
            }
            case "NBT_LIST": {
                TagList list = new TagList();
                TokenList entries = (TokenList) pattern.find("..NBT_LIST_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (!inner.getName().equals("COMMA")) {
                            NBTTag value = parseValue(inner.find("NBT_VALUE"));
                            list.add(value);
                        }
                    }
                }
                return list;
            }
            case "BOOLEAN": {
                return new TagByte(pattern.flattenTokens().get(0).value.equals("true") ? 1 : 0);
            }
            case "RAW_STRING": {
                return new TagString(pattern.flattenTokens().get(0).value);
            }
            case "STRING_LITERAL": {
                return new TagString(CommandUtils.parseQuotedString(pattern.flattenTokens().get(0).value));
            }
            case "NBT_NUMBER": {
                String flat = pattern.flattenTokens().get(0).value;

                final Pattern regex = Pattern.compile("([+-]?\\d+(\\.\\d+)?)([bdfsL]?)", Pattern.CASE_INSENSITIVE);

                Matcher matcher = regex.matcher(flat);
                matcher.lookingAt(); //must be true

                String numberPart = matcher.group(1);
                switch(matcher.group(3).toLowerCase()) {
                    case "": {
                        return new TagInt(Integer.parseInt(numberPart));
                    }
                    case "b": {
                        return new TagByte(Byte.parseByte(numberPart));
                    }
                    case "d": {
                        return new TagDouble(Double.parseDouble(numberPart));
                    }
                    case "f": {
                        return new TagFloat(Float.parseFloat(numberPart));
                    }
                    case "s": {
                        return new TagShort(Short.parseShort(numberPart));
                    }
                    case "l": {
                        return new TagLong(Long.parseLong(numberPart));
                    }
                }
            }
        }
        return new TagString("ERROR WHILE PARSING TAG " + pattern.getName());
    }

    /**
     * NBTParser should not be instantiated.
     * */
    private NBTParser() {
    }

    public static NBTPath parsePath(TokenPattern<?> pattern) {
        return new NBTPath("Inventory");
    }
}
