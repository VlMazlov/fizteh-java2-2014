package ru.fizteh.java2.vlmazlov.storage.presentation.commands;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.fizteh.java2.vlmazlov.storage.shell.commands.api.CommandFailException;
import ru.fizteh.java2.vlmazlov.storage.presentation.DataBaseState;

import java.io.OutputStream;

@Lazy
@Component
public class RollBackCommand extends AbstractDataBaseCommand {
    public RollBackCommand() {
        super("rollback", 0);
    }

    public void execute(String[] args, DataBaseState state, OutputStream out) throws CommandFailException {
        if (state.getActiveTable() == null) {
            displayMessage("no table" + SEPARATOR, out);
            return;
        }

        displayMessage(state.getActiveTable().rollback() + SEPARATOR, out);
    }
}
