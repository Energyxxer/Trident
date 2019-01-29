package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setblock.SetblockCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "setblock")
public class SetblockParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"), file);
        Block block = CommonParsers.parseBlock(pattern.find("BLOCK"), file);
        SetblockCommand.OldBlockHandlingMode mode = SetblockCommand.OldBlockHandlingMode.DEFAULT;

        if(!block.getBlockType().isStandalone()) {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Block tags aren't allowed in this context", pattern.find("BLOCK"), file);
        }

        TokenPattern<?> rawMode = pattern.find("OLD_BLOCK_HANDLING");
        if(rawMode != null) {
            mode = SetblockCommand.OldBlockHandlingMode.valueOf(rawMode.flatten(false).toUpperCase());
        }

        return new SetblockCommand(pos, block, mode);
    }
}