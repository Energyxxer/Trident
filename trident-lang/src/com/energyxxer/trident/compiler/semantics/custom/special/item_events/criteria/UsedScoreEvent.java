package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

import java.util.ArrayList;

@ParserMember(key = "used")
public class UsedScoreEvent implements ScoreEventCriteriaHandler {
    @Override
    public void start(ScoreEventCriteriaData data) {

    }

    @Override
    public void mid(ScoreEventCriteriaData data) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(data.itemCriteriaObjective, new NumberRange<>(1, null));

        Selector initialSelector = new Selector(Selector.BaseSelector.SENDER, scores);

        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        modifiers.add(new ExecuteConditionEntity(ExecuteCondition.ConditionType.IF, new GenericEntity(initialSelector)));

        if(data.customItem != null) {
            scores.put(data.oldHeldObjective, new NumberRange<>(data.customItem.getItemIdHash()));
        }

        for(FunctionReference reference : data.functionsToCall) {
            data.function.append(new ExecuteCommand(new FunctionCommand(reference), modifiers));
        }
    }

    @Override
    public void end(ScoreEventCriteriaData data) {

    }
}