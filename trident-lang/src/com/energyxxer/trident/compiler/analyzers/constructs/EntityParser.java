package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.trident.compiler.analyzers.constructs.selectors.SelectorArgumentParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;

import java.util.Collection;

public class EntityParser {

    public static Selector parseSelector(TokenPattern<?> pattern, ISymbolContext ctx) {
        char header = pattern.find("SELECTOR_HEADER").flattenTokens().get(0).value.charAt(1);
        Selector selector = new Selector(Selector.BaseSelector.getForHeader(header + ""));

        TokenPattern<?> argList = pattern.find("..SELECTOR_ARGUMENT_LIST");
        if(argList instanceof TokenList) {
            TokenList list = (TokenList) argList;
            parseSelectorArguments(list, selector, pattern, ctx);
        }

        return selector;
    }

    private static void parseSelectorArguments(TokenList list, Selector selector, TokenPattern<?> pattern, ISymbolContext ctx) {
        PathContext pathContext = new PathContext().setIsSetting(false).setProtocol(PathProtocol.ENTITY);
        for(TokenPattern<?> rawArg : list.getContents()) {
            if(rawArg.getName().equals("SELECTOR_ARGUMENT")) {
                SelectorArgumentParser parser = AnalyzerManager.getAnalyzer(SelectorArgumentParser.class, rawArg.flattenTokens().get(0).value);
                if(parser != null) {
                    try {
                        Collection<SelectorArgument> args = parser.parse(((TokenStructure)((TokenStructure) rawArg).getContents().find("SELECTOR_ARGUMENT_VALUE")).getContents(), ctx, pathContext);
                        if(args != null && !args.isEmpty()) {
                            selector.addArgumentsMerging(args);
                            for(SelectorArgument arg : args) {
                                if(arg instanceof TypeArgument && !((TypeArgument) arg).isNegated()) {
                                    Type entityType = ((TypeArgument) arg).getType();
                                    if(entityType.isStandalone()) {
                                        pathContext.setProtocolMetadata(entityType);
                                    }
                                }
                            }
                        }
                    } catch(CommodoreException x) {
                        TridentException.handleCommodoreException(x, rawArg, ctx).invokeThrow();
                    }
                } else {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown selector argument analyzer for '" + rawArg.flattenTokens().get(0).value + "'", rawArg, ctx);
                }
            }
        }
    }

    /**
     * EntityParser should not be instantiated.
     * */
    private EntityParser() {
    }

    public static Entity parseEntity(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        while("ENTITY".equals(pattern.getName())) {
            pattern = ((TokenStructure) pattern).getContents();
        }
        switch(pattern.getName()) {
            case "SELECTOR":
                return parseSelector(pattern, ctx);
            case "PLAYER_NAME": return new PlayerName(CommonParsers.parseIdentifierB(pattern.find("IDENTIFIER_B"), ctx));
            case "ENTITY_VARIABLE": {
                Object symbol = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, Entity.class, String.class);
                if(symbol instanceof String && !TridentLexerProfile.IDENTIFIER_B_REGEX.matcher((String) symbol).matches()) {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "The string '" + symbol + "' is not a valid argument here", pattern, ctx);
                }
                Entity entity = symbol instanceof Entity ? (Entity) symbol : new PlayerName(((String) symbol));
                EObject.assertNotNull(entity, pattern.find("INTERPOLATION_BLOCK"), ctx);
                if(pattern.find("APPENDED_ARGUMENTS") != null) {
                    if(!(entity instanceof Selector)) {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "The entity contained in this variable does not support selector arguments", pattern, ctx);
                    }

                    Selector copy = ((Selector) entity).clone();

                    TokenList argList = (TokenList) pattern.find("APPENDED_ARGUMENTS.SELECTOR_ARGUMENT_LIST");
                    if(argList != null) parseSelectorArguments(argList, copy, pattern, ctx);

                    return copy;
                } else return entity;
            } default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }
}
