package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionComment;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.*;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.CompilerExtension;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.RawCommand;
import com.energyxxer.trident.compiler.commands.parsers.CommandParser;
import com.energyxxer.trident.compiler.commands.parsers.EntryParsingException;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class TridentFile implements CompilerExtension {
    private final TridentCompiler compiler;
    private final CommandModule module;
    private final Namespace namespace;
    private TokenPattern<?> pattern;
    private final HashMap<TokenPattern<?>, TridentUtil.ResourceLocation> requires = new HashMap<>();
    private final ArrayList<TridentUtil.ResourceLocation> tags = new ArrayList<>();

    private Function function;
    private final TridentUtil.ResourceLocation location;

    private boolean compileOnly = false;


    public TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this.compiler = compiler;
        this.module = compiler.getModule();
        this.namespace = module.createNamespace(relSourcePath.getName(0).toString());
        this.pattern = filePattern;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Matcher.quoteReplacement(File.separator), "/");
        this.location = new TridentUtil.ResourceLocation(this.namespace.getName() + ":" + functionPath);

        TokenPattern<?> directiveList = filePattern.find("..DIRECTIVES");
        if(directiveList != null) {
            TokenPattern<?>[] directives = ((TokenList) directiveList).getContents();
            for(TokenPattern<?> rawDirective : directives) {
                TokenGroup directiveBody = (TokenGroup) (((TokenStructure) ((TokenGroup) rawDirective).getContents()[1]).getContents());

                switch(directiveBody.getName()) {
                    case "ON_DIRECTIVE": {
                        String on = ((TokenItem) (directiveBody.getContents()[1])).getContents().value;
                        if(on.equals("compile")) {
                            if(!tags.isEmpty()) {
                                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-ony function may not have any tags", directiveList));
                            }
                            compileOnly = true;
                        }
                        break;
                    }
                    case "TAG_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents());
                        if(compileOnly) {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-ony function may not have any tags", directiveList));
                        }
                        tags.add(loc);
                        break;
                    }
                    case "REQUIRE_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents());
                        requires.put(directiveBody, loc);
                        break;
                    }
                    default: {
                        reportNotice(new Notice(NoticeType.DEBUG, "Unknown directive type '" + directiveBody.getName() + "'", directiveBody));
                    }
                }
            }
        }
        this.function = compileOnly ? null : namespace.functions.create(functionPath);

    }

    public TridentCompiler getCompiler() {
        return compiler;
    }

    private boolean reportedNoCommands = false;

    public void resolveEntries() {

        if(function != null) tags.forEach(l -> module.createNamespace(l.namespace).tags.functionTags.create(l.body).addValue(new FunctionReference(this.function)));


        TokenPattern<?>[] entries = ((TokenList) this.pattern.find(".ENTRIES")).getContents();

        boolean exportComments = compiler.getProperties().get("export-comments") == null || compiler.getProperties().get("export-comments").getAsBoolean();

        for(TokenPattern<?> pattern : entries) {
            if(!pattern.getName().equals("LINE_PADDING")) {
                TokenStructure entry = (TokenStructure) pattern.find("ENTRY");

                TokenPattern<?> inner = entry.getContents();

                try {

                    switch (inner.getName()) {
                        case "COMMAND":
                            if(!compileOnly) {
                                CommandParser.Static.parse(((TokenStructure) inner).getContents(), this);
                            } else if(!reportedNoCommands) {
                                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                                reportedNoCommands = true;
                            }
                            break;
                        case "COMMENT":
                            if (exportComments && function != null)
                                function.append(new FunctionComment(inner.flattenTokens().get(0).value.substring(1)));
                            break;
                        case "VERBATIM_COMMAND":
                            if(!compileOnly) {
                                function.append(new RawCommand(inner.flattenTokens().get(0).value.substring(1)));
                            } else if(!reportedNoCommands) {
                                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                                reportedNoCommands = true;
                            }
                            break;
                    }
                } catch(EntryParsingException x) {
                    //Silently ignore; serves as a multi-function break;
                }
            }
        }
    }

    public Function getFunction() {
        return function;
    }

    public Collection<TridentUtil.ResourceLocation> getRequires() {
        return requires.values();
    }

    public boolean checkCircularRequires() {
        return this.checkCircularRequires(new ArrayList<>());
    }

    public boolean checkCircularRequires(ArrayList<TridentUtil.ResourceLocation> previous) {
        boolean returnValue = false;
        previous.add(this.location);
        for(Map.Entry<TokenPattern<?>, TridentUtil.ResourceLocation> entry : requires.entrySet()) {
            if(previous.contains(entry.getValue())) {
                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Circular requirement with function '" + entry.getValue() + "'", entry.getKey()));
            } else {
                TridentFile next = getCompiler().getFile(entry.getValue());
                if(next != null) {
                    if(next.checkCircularRequires(previous)) {
                        returnValue = true;
                    }
                } else {
                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required Trident function '" + entry.getKey() + "' does not exist"));
                }
            }
        }
        return returnValue;
    }

    public TridentUtil.ResourceLocation getResourceLocation() {
        return location;
    }

    public boolean isCompileOnly() {
        return compileOnly;
    }

    @Override
    public String toString() {
        return "TDN: " + location;
    }
}
