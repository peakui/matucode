package com.peakui.message.collab.ot;

import com.peakui.message.exception.MessageException;
import org.springframework.stereotype.Component;

@Component
public class TextDocumentApplier {

    public String apply(String content, TextOperation operation) {
        String current = content == null ? "" : content;
        if (operation.isNoop()) {
            return current;
        }
        if (operation.getType() == OperationType.INSERT) {
            int position = Math.min(operation.getPosition(), current.length());
            return current.substring(0, position) + operation.getText()
                    + current.substring(position);
        }
        if (operation.getType() == OperationType.DELETE) {
            if (operation.getPosition() >= current.length()) {
                operation.setType(OperationType.NOOP);
                operation.setLength(0);
                return current;
            }
            int end = Math.min(operation.getPosition() + operation.getLength(), current.length());
            return current.substring(0, operation.getPosition()) + current.substring(end);
        }
        return current;
    }
}
