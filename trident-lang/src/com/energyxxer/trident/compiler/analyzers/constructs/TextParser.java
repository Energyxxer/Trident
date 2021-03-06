package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.*;
import com.energyxxer.commodore.textcomponents.events.ClickEvent;
import com.energyxxer.commodore.textcomponents.events.HoverEvent;
import com.energyxxer.commodore.textcomponents.events.InsertionEvent;
import com.energyxxer.commodore.textcomponents.events.hover.ContentHoverEvent;
import com.energyxxer.commodore.textcomponents.events.hover.ShowEntityHoverEvent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeNotFoundException;
import com.energyxxer.commodore.types.defaults.FontReference;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.sets.JsonLiteralSet;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupPropertiesTask;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.energyxxer.commodore.types.TypeAssert.assertItem;
import static com.energyxxer.trident.compiler.util.Using.using;
import static com.energyxxer.trident.extensions.EJsonElement.*;

public class TextParser {

    public static TextComponent primitiveToTextComponent(String str) {
        return new StringTextComponent(str);
    }

    public static TextComponent primitiveToTextComponent(boolean bool) {
        return new StringTextComponent(Boolean.toString(bool));
    }

    public static TextComponent primitiveToTextComponent(Number num) {
        return new StringTextComponent(num.toString());
    }

    public static TextComponent primitiveToTextComponent(JsonPrimitive prim, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(prim.isString()) {
            return primitiveToTextComponent(prim.getAsString());
        } else if(prim.isNumber()) {
            return primitiveToTextComponent(prim.getAsNumber());
        } else if(prim.isBoolean()) {
            return primitiveToTextComponent(prim.getAsBoolean());
        }
        throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
    }

    public static TextComponent jsonToTextComponent(JsonElement elem, ISymbolContext ctx, TokenPattern<?> pattern, TextComponentContext textContext) {
        if(elem instanceof TextComponentJsonElement) return ((TextComponentJsonElement) elem).getWrapped();

        boolean strict = ctx.get(SetupPropertiesTask.INSTANCE).has("strict-text-components") && getAsBooleanOrNull(ctx.get(SetupPropertiesTask.INSTANCE).get("strict-text-components"));

        ReportDelegate delegate = new ReportDelegate(ctx.getCompiler(), strict, pattern, ctx);

        final TextComponent[] component = new TextComponent[1];

        if(elem.isJsonPrimitive()) {
            return primitiveToTextComponent(elem.getAsJsonPrimitive(), pattern, ctx);
        } else if(elem.isJsonArray()) {
            ListTextComponent list = new ListTextComponent();
            for(JsonElement sub : elem.getAsJsonArray()) {
                list.append(jsonToTextComponent(sub, ctx, pattern, textContext));
            }
            return list;
        } else if(elem.isJsonObject()) {
            JsonObject obj = getAsJsonObjectOrNull(elem);

            component[0] = null;

            if(obj.has("text")) {
                using(getAsStringOrNumberOrNull(obj.get("text")))
                        .notIfNull()
                        .run(t -> component[0] = new StringTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'text'", obj.get("text")));
            } else if(obj.has("translate")) {
                using(getAsStringOrNumberOrNull(obj.get("translate")))
                        .notIfNull()
                        .run(t -> {
                            component[0] = new TranslateTextComponent(t);
                            if(obj.has("with")) {
                                using(getAsJsonArrayOrNull(obj.get("with"))).notIfNull().run(
                                        a -> a.forEach(e -> ((TranslateTextComponent) component[0]).addWith(jsonToTextComponent(e, ctx, pattern, textContext)))
                                ).otherwise(v -> delegate.report("Expected array in 'with'", obj.get("with")));
                            }
                        }).otherwise(t -> delegate.report("Expected string in 'translate'", obj.get("translate")));
            } else if(obj.has("keybind")) {
                using(getAsStringOrNumberOrNull(obj.get("keybind")))
                        .notIfNull()
                        .run(t -> component[0] = new KeybindTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'keybind'", obj.get("keybind")));
            } else if(obj.has("score")) {
                using(getAsJsonObjectOrNull(obj.get("score"))).notIfNull().run(s -> {
                    String name = getAsStringOrNumberOrNull(s.get("name"));
                    if(name == null) delegate.report("Missing 'name' string for 'score' text component", s);
                    String objectiveName = getAsStringOrNumberOrNull(s.get("objective"));
                    if(objectiveName == null) delegate.report("Missing 'objective' string for 'score' text component", s);
                    if(name != null && objectiveName != null) {
                        Objective objective = ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().getOrCreate(objectiveName);
                        component[0] = new ScoreTextComponent(new LocalScore(objective, new RawEntity(name)));
                    }
                }).otherwise(v -> delegate.report("Expected object in 'score'", obj.get("score")));
            } else if(obj.has("selector")) {
                using(getAsStringOrNumberOrNull(obj.get("selector")))
                        .notIfNull()
                        .run(t -> {
                            TextComponent separator = null;
                            JsonElement rawSeparator = obj.get("separator");
                            if(rawSeparator != null) {
                                separator = jsonToTextComponent(rawSeparator, ctx, pattern, textContext);
                            }
                            component[0] = new SelectorTextComponent(new RawEntity(t), null, separator);
                        })
                        .otherwise(t -> delegate.report("Expected string in 'selector'", obj.get("selector")));
            } else if(obj.has("nbt")) {
                JsonElement rawSeparator = obj.get("separator");
                TextComponent separator = rawSeparator != null ? jsonToTextComponent(rawSeparator, ctx, pattern, textContext) : null;
                using(getAsStringOrNumberOrNull(obj.get("nbt"))).notIfNull().run(s -> {
                    Boolean rawInterpret = getAsBooleanOrNull(obj.get("interpret"));
                    boolean interpret = rawInterpret != null && rawInterpret;

                    using(getAsStringOrNumberOrNull(obj.get("entity"))).notIfNull()
                            .run(e -> component[0] = new RawNBTTextComponent(s, "entity", e, interpret, separator))
                            .otherwise(
                                    v -> using(getAsStringOrNumberOrNull(obj.get("block"))).notIfNull().run(b ->
                                            component[0] = new RawNBTTextComponent(s, "block", b, interpret, separator))
                                            .otherwise(w -> using(getAsStringOrNumberOrNull(obj.get("storage"))).notIfNull().run(b ->
                                            component[0] = new RawNBTTextComponent(s, "storage", b, interpret, separator)
                                                    ).otherwise(x -> delegate.report("Expected either 'entity', 'block' or 'storage' in nbt text component, got neither.", obj)))
                    );
                }).otherwise(v -> delegate.report("Expected object in 'nbt'", obj.get("nbt")));
            }
            if(component[0] == null) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Don't know how to turn this into a text component: " + elem, pattern, ctx);
            }

            TextStyle style = new TextStyle(0);
            style.setMask(0);
            if(obj.has("color")) {
                try {
                    using(getAsStringOrNumberOrNull(obj.get("color")))
                            .notIfNull()
                            .run(t -> {
                                TextColor color = TextColor.valueOf(t.toUpperCase());
                                if(color == null) {
                                    throw new IllegalArgumentException();
                                } else {
                                    style.setColor(color);
                                    if(color == TextColor.RESET && VersionFeatureManager.getBoolean("textcomponent.hex_color")) {
                                        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "The color 'reset' is no longer functional in 1.16", pattern));
                                    }
                                }
                            })
                            .otherwise(t -> delegate.report("Expected string in 'color'", obj.get("color")));
                } catch(IllegalArgumentException x) {
                    delegate.report("Illegal text color '" + getAsStringOrNumberOrNull(obj.get("color")) + "'",
                            "Unknown text color '" + getAsStringOrNumberOrNull(obj.get("color")) + "'", obj.get("color"));
                }
            }
            if(obj.has("font")) {
                try {
                    using(getAsStringOrNumberOrNull(obj.get("font")))
                            .notIfNull()
                            .run(t -> {
                                ResourceLocation fontLoc = ResourceLocation.createStrict(t);
                                if(fontLoc != null) {
                                    style.setFont(new FontReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(fontLoc.namespace), fontLoc.body));
                                } else {
                                    throw new IllegalArgumentException(t);
                                }
                            })
                            .otherwise(t -> delegate.report("Expected string in 'font'", obj.get("font")));
                } catch(IllegalArgumentException x) {
                    delegate.report("Illegal font resource location '" + getAsStringOrNumberOrNull(obj.get("font")) + "'", obj.get("font"));
                }
            }
            if(obj.has("bold")) {
                using(getAsBooleanOrNull(obj.get("bold"))).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.BOLD);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.BOLD));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.BOLD));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'bold'", obj.get("bold")));
            }
            if(obj.has("italic")) {
                using(getAsBooleanOrNull(obj.get("italic"))).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.ITALIC);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.ITALIC));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.ITALIC));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'italic'", obj.get("italic")));
            }
            if(obj.has("strikethrough")) {
                using(getAsBooleanOrNull(obj.get("strikethrough"))).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.STRIKETHROUGH);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.STRIKETHROUGH));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.STRIKETHROUGH));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'strikethrough'", obj.get("strikethrough")));
            }
            if(obj.has("underlined")) {
                using(getAsBooleanOrNull(obj.get("underlined"))).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.UNDERLINED);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.UNDERLINED));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.UNDERLINED));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'underlined'", obj.get("underlined")));
            }
            if(obj.has("obfuscated")) {
                using(getAsBooleanOrNull(obj.get("obfuscated"))).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.OBFUSCATED);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.OBFUSCATED));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.OBFUSCATED));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'obfuscated'", obj.get("obfuscated")));
            }
            component[0].setStyle(style);

            if(obj.has("hoverEvent")) {
                using(obj.getAsJsonObject("hoverEvent")).notIfNull().run(e -> {
                    if(!textContext.isHoverEnabled()) delegate.report("Hover events are not allowed in this context", "Hover events are not used in this context", e);

                    using(getAsStringOrNumberOrNull(e.get("action"))).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal hover event action '" + a + "'", "Unknown hover event action '" + a + "'", e.get("action")))
                            .run(a -> {
                        HoverEvent.Action action = HoverEvent.Action.valueOf(a.toUpperCase());
                        using(e.get("value")).notIfNull().run(v -> {
                            if(v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                                String value = getAsStringOrNumberOrNull(v);
                                component[0].addEvent(new HoverEvent(action, value));
                            } else {
                                TextComponent value = (jsonToTextComponent(v, ctx, pattern, TextComponentContext.TOOLTIP));
                                component[0].addEvent(new HoverEvent(action, value));
                            }
                        }).otherwise(v -> {
                            if(VersionFeatureManager.getBoolean("textcomponent.hover_event.content")) {
                                using(e.get("contents")).notIfNull().run(c -> {
                                    switch(action) {
                                        case SHOW_TEXT: {
                                            TextComponent value = (jsonToTextComponent(c, ctx, pattern, TextComponentContext.TOOLTIP));
                                            component[0].addEvent(new HoverEvent(action, value));
                                            break;
                                        }
                                        case SHOW_ITEM: {
                                            ResourceLocation[] itemIdToShow = new ResourceLocation[] {null};
                                            int[] count = new int[] {1};
                                            String[] rawTag = new String[] {null};

                                            if(c.isJsonPrimitive() && c.getAsJsonPrimitive().isString()) {
                                                itemIdToShow[0] = ResourceLocation.createStrict(c.getAsString());
                                            } else {
                                                using(getAsJsonObjectOrNull(c)).notIfNull()
                                                        .run(i -> {
                                                            using(getAsStringOrNumberOrNull(i.get("id"))).notIfNull()
                                                                    .run(rawId -> itemIdToShow[0] = new ResourceLocation(rawId))
                                                                    .otherwise(ignore -> delegate.report("Expected string in 'id'", i));
                                                            if(i.has("count")) {
                                                                using(getAsIntegerOrNull(i.get("count"))).notIfNull()
                                                                        .run(lambdaCount -> count[0] = lambdaCount)
                                                                        .otherwise(ignore -> delegate.report("Expected integer in 'count'", i.get("count")));
                                                            }
                                                            if(i.has("tag")) {
                                                                using(getAsStringOrNumberOrNull(i.get("tag"))).notIfNull()
                                                                        .run(lambdaRawTag -> rawTag[0] = lambdaRawTag)
                                                                        .otherwise(ignore -> delegate.report("Expected string in 'tag'", i.get("tag")));
                                                            }
                                                        }).otherwise(i -> delegate.report("Expected string or object in 'contents' for show_item hover event", c));
                                            }

                                            if(itemIdToShow[0] != null) {
                                                try {
                                                    Type itemType = ctx.get(SetupModuleTask.INSTANCE).getNamespace(itemIdToShow[0].namespace).types.item.get(itemIdToShow[0].body);
                                                    component[0].addEvent(new RawShowItemHoverEvent(itemType, count[0], rawTag[0]));
                                                } catch(TypeNotFoundException x) {
                                                    delegate.report("Illegal item type: " + itemIdToShow[0], c);
                                                }
                                            }
                                            break;
                                        }
                                        case SHOW_ENTITY: {
                                            ResourceLocation[] entityIdToShow = new ResourceLocation[] {null};
                                            UUID[] id = new UUID[] {null};
                                            TextComponent[] name = new TextComponent[] {null};

                                            using(getAsJsonObjectOrNull(c)).notIfNull()
                                                    .run(i -> {
                                                        using(getAsStringOrNumberOrNull(i.get("type"))).notIfNull()
                                                                .run(rawId -> entityIdToShow[0] = new ResourceLocation(rawId))
                                                                .otherwise(ignore -> delegate.report("Expected string in 'type'", i));
                                                        if(i.has("id")) {
                                                            using(getAsStringOrNumberOrNull(i.get("id"))).notIfNull()
                                                                    .except(IllegalArgumentException.class, (x, xobj) -> delegate.report("Illegal UUID", "Invalid UUID", i.get("id")))
                                                                    .run(rawId -> id[0] = UUID.fromString(rawId))
                                                                    .otherwise(ignore -> delegate.report("Expected string in 'id'", i.get("id")));
                                                        }
                                                        if(i.has("name")) {
                                                            name[0] = jsonToTextComponent(i.get("name"), ctx, pattern, TextComponentContext.TOOLTIP);
                                                        }
                                                    }).otherwise(i -> delegate.report("Expected object in 'contents' for show_entity hover event", c));

                                            if(entityIdToShow[0] != null) {
                                                try {
                                                    Type entityType = ctx.get(SetupModuleTask.INSTANCE).getNamespace(entityIdToShow[0].namespace).types.entity.get(entityIdToShow[0].body);
                                                    component[0].addEvent(new ShowEntityHoverEvent(entityType, id[0], name[0]));
                                                } catch(TypeNotFoundException x) {
                                                    delegate.report("Illegal entity type: " + entityIdToShow[0], c);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }).otherwise(c -> delegate.report("Missing hover event contents or value", e));
                            } else {
                                delegate.report("Missing hover event value", e);
                            }
                        });
                    }).otherwise(a -> delegate.report("Missing hover event action", e));
                });
            }
            if(obj.has("clickEvent")) {
                using(obj.getAsJsonObject("clickEvent")).notIfNull().run(e -> {
                    if(!textContext.isClickEnabled()) delegate.report("Click events are not allowed in this context", "Click events are not used in this context", e);

                    using(getAsStringOrNumberOrNull(e.get("action"))).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal click event action '" + a + "'", "Unknown click event action '" + a + "'", e.get("action")))
                            .run(a -> {
                                ClickEvent.Action action = ClickEvent.Action.valueOf(a.toUpperCase());
                                using(e.get("value")).notIfNull().run(v -> {
                                    String value = getAsStringOrNumberOrNull(v);
                                    if(value == null) delegate.report("Missing click event value", e);
                                    else component[0].addEvent(new ClickEvent(action, value));
                                }).otherwise(v -> delegate.report("Missing click event value", e));
                            }).otherwise(a -> delegate.report("Missing click event action", e));
                });
            }
            if(obj.has("insertion")) {
                using(getAsStringOrNumberOrNull(obj.get("insertion"))).notIfNull().run(
                        t -> component[0].addEvent(new InsertionEvent(t))
                ).otherwise(v -> delegate.report("Expected string in 'insertion'", obj.get("insertion")));
            }

            if(obj.has("extra")) {
                using(getAsJsonArrayOrNull(obj.get("extra"))).notIfNull().run(
                        a -> a.forEach(e -> component[0].addExtra(jsonToTextComponent(e, ctx, pattern, textContext)))
                ).otherwise(v -> delegate.report("Expected array in 'extra'", obj.get("extra")));
            }

            return component[0];
        } else {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Don't know how to turn this into a text component: " + elem, pattern, ctx);
        }
    }

    /**
     * TextParser must not be instantiated.
     */
    private TextParser() {

    }

    public static class RawNBTTextComponent extends TextComponent {
        @NotNull
        private final String path;
        @NotNull
        private final String key;
        @NotNull
        private final String toPrint;
        private final boolean interpret;
        private final TextComponent separator;

        RawNBTTextComponent(@NotNull String path, @NotNull String key, @NotNull String toPrint, boolean interpret, @Nullable TextComponent separator) {
            this.path = path;
            this.key = key;
            this.toPrint = toPrint;
            this.interpret = interpret;
            this.separator = separator;
        }

        @Override
        public boolean supportsProperties() {
            return true;
        }

        @Override
        public String toString(TextStyle parentStyle) {
            String baseProperties = this.getBaseProperties(parentStyle);

            String extra = "\"" + key + "\":\"" + CommandUtils.escape(toPrint) + "\"";
            if(interpret) extra += ",\"interpret\":true";
            return "{\"nbt\":\"" + CommandUtils.escape(path) + "\"," +
                    extra +
                    (separator != null ? ",\"separator\":" + separator.toString(style) : "") +
                    (baseProperties != null ? "," + baseProperties : "") +
                    '}';
        }

        @Override
        public void assertAvailable() {
            VersionFeatureManager.assertEnabled("textcomponent.nbt");
            super.assertAvailable();
            VersionFeatureManager.assertEnabled("nbt.access");
            if(separator != null) VersionFeatureManager.assertEnabled("textcomponent.separators");
        }
    }

    public static class TextComponentJsonElement extends com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.JSON.WrapperJsonElement<TextComponent> {
        public TextComponentJsonElement(TextComponent wrapped) {
            super(wrapped, TextComponent.class);
        }
    }

    static class ReportDelegate {
        private PrismarineCompiler compiler;
        private boolean strict;
        private TokenPattern<?> pattern;
        private ISymbolContext ctx;

        public ReportDelegate(PrismarineCompiler compiler, boolean strict, TokenPattern<?> pattern, ISymbolContext ctx) {
            this.compiler = compiler;
            this.strict = strict;
            this.pattern = pattern;
            this.ctx = ctx;
        }

        public void report(String message) {
            report(message, this.pattern);
        }

        public void report(String message, JsonElement element) {
            report(message, message, JsonLiteralSet.getPatternFor(element));
        }

        public void report(String message, TokenPattern<?> pattern) {
            report(message, message, pattern);
        }

        public void report(String strict, String notStrict) {
            report(strict, notStrict, this.pattern);
        }

        public void report(String strict, String notStrict, JsonElement element) {
            report(strict, notStrict, JsonLiteralSet.getPatternFor(element));
        }

        public void report(String strict, String notStrict, TokenPattern<?> pattern) {
            if(pattern == null) pattern = this.pattern;
            if(this.strict) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, strict, pattern, ctx);
            } else {
                compiler.getReport().addNotice(new Notice(NoticeType.WARNING, notStrict, pattern));
            }
        }
    }

    static class RawEntity extends PlayerName {

        public RawEntity(@NotNull String name) {
            super(name);
        }

        @Override
        public void assertPlayer() {

        }

        @Override
        public void assertEntityFriendly() {

        }

        @Override
        public void assertScoreHolderFriendly() {

        }

        @Override
        public void assertPlayer(String causeKey) {

        }

        @Override
        public void assertGameProfile() {

        }

        @Override
        public void assertSingle() {

        }

        @Override
        public void assertSingle(String causeKey) {

        }

        @Override
        public void assertAvailable() {

        }
    }

    static class RawShowItemHoverEvent extends ContentHoverEvent {
        private final @NotNull Type type;
        private final int count;
        private final String rawTag;

        public RawShowItemHoverEvent(@NotNull Type type, int count, String rawTag) {
            this.type = type;
            this.count = count;
            this.rawTag = rawTag;

            assertItem(type);
        }

        @Override
        public String toString() {
            String content = "\"" + CommandUtils.escape(type.toString()) + "\"";
            if(count != 1 || rawTag != null) {
                content = "{\"id\":" + content;
                if(count != 1) {
                    content += ",\"count\":" + count;
                }
                if(rawTag != null) {
                    content += ",\"tag\":\"" + CommandUtils.escape(rawTag) + "\"";
                }
                content += "}";
            }

            return "\"hoverEvent\":{\"action\":\"show_item\",\"contents\":" + content + "}";
        }
    }
}
