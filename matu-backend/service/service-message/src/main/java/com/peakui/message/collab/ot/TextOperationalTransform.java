package com.peakui.message.collab.ot;

import org.springframework.stereotype.Component;

@Component
public class TextOperationalTransform {

    public TextOperation transform(TextOperation incoming, TextOperation committed) {
        TextOperation result = incoming.copy();
        if (result.isNoop() || committed.isNoop()) {
            return result;
        }
        if (result.getType() == OperationType.INSERT && committed.getType() == OperationType.INSERT) {
            transformInsertAgainstInsert(result, committed);
        } else if (result.getType() == OperationType.INSERT && committed.getType() == OperationType.DELETE) {
            transformInsertAgainstDelete(result, committed);
        } else if (result.getType() == OperationType.DELETE && committed.getType() == OperationType.INSERT) {
            transformDeleteAgainstInsert(result, committed);
        } else if (result.getType() == OperationType.DELETE && committed.getType() == OperationType.DELETE) {
            transformDeleteAgainstDelete(result, committed);
        }
        return result;
    }

    private void transformInsertAgainstInsert(TextOperation incoming, TextOperation committed) {
        if (committed.getPosition() <= incoming.getPosition()) {
            incoming.setPosition(incoming.getPosition() + committed.insertedLength());
        }
    }

    private void transformInsertAgainstDelete(TextOperation incoming, TextOperation committed) {
        int deleteStart = committed.getPosition();
        int deleteEnd = committed.getPosition() + committed.getLength();
        if (deleteEnd <= incoming.getPosition()) {
            incoming.setPosition(incoming.getPosition() - committed.getLength());
        } else if (deleteStart <= incoming.getPosition()) {
            incoming.setPosition(deleteStart);
        }
    }

    private void transformDeleteAgainstInsert(TextOperation incoming, TextOperation committed) {
        int insertPosition = committed.getPosition();
        int insertLength = committed.insertedLength();
        int deleteStart = incoming.getPosition();
        int deleteEnd = incoming.getPosition() + incoming.getLength();
        if (insertPosition <= deleteStart) {
            incoming.setPosition(deleteStart + insertLength);
        } else if (insertPosition < deleteEnd) {
            incoming.setLength(incoming.getLength() + insertLength);
        }
    }

    private void transformDeleteAgainstDelete(TextOperation incoming, TextOperation committed) {
        int aStart = incoming.getPosition();
        int aEnd = incoming.getPosition() + incoming.getLength();
        int bStart = committed.getPosition();
        int bEnd = committed.getPosition() + committed.getLength();
        if (bEnd <= aStart) {
            incoming.setPosition(aStart - committed.getLength());
            return;
        }
        if (bStart >= aEnd) {
            return;
        }
        int overlapStart = Math.max(aStart, bStart);
        int overlapEnd = Math.min(aEnd, bEnd);
        int newLength = incoming.getLength() - Math.max(0, overlapEnd - overlapStart);
        incoming.setPosition(Math.min(aStart, bStart));
        if (newLength <= 0) {
            incoming.setType(OperationType.NOOP);
            incoming.setLength(0);
            incoming.setText(null);
        } else {
            incoming.setLength(newLength);
        }
    }
}
