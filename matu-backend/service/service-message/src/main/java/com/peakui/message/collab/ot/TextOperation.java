package com.peakui.message.collab.ot;

import com.peakui.message.collab.model.dto.TextOperationDTO;
import com.peakui.message.exception.MessageException;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextOperation {
    private OperationType type;
    private int position;
    private String text;
    private int length;

    public static TextOperation fromDTO(TextOperationDTO dto) {
        OperationType type = OperationType.fromValue(dto.getOp());
        int position = dto.getPosition() == null ? 0 : dto.getPosition();
        String text = dto.getText();
        int length = dto.getLength() == null ? 0 : dto.getLength();
        TextOperation operation = new TextOperation(type, position, text, length);
        operation.validate();
        return operation;
    }

    public TextOperation copy() {
        return new TextOperation(type, position, text, length);
    }

    public TextOperationDTO toDTO() {
        TextOperationDTO dto = new TextOperationDTO();
        dto.setOp(type.value());
        dto.setPosition(position);
        dto.setText(text);
        dto.setLength(length);
        return dto;
    }

    public boolean isNoop() {
        return type == OperationType.NOOP || length == 0 && type == OperationType.DELETE;
    }

    public int insertedLength() {
        return type == OperationType.INSERT && text != null ? text.length() : 0;
    }

    public void validate() {
        if (position < 0) {
            throw new MessageException("编辑位置不能小于0");
        }
        if (type == OperationType.INSERT && (text == null || text.isEmpty())) {
            throw new MessageException("插入内容不能为空");
        }
        if (type == OperationType.DELETE && length <= 0) {
            throw new MessageException("删除长度必须大于0");
        }
    }
}
