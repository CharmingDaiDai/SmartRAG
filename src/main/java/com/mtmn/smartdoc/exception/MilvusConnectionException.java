package com.mtmn.smartdoc.exception;

import com.mtmn.smartdoc.common.CustomException;

/**
 * Milvus 连接异常
 *
 * @author charmingdaidai
 */
public class MilvusConnectionException extends CustomException {

    public MilvusConnectionException(String message) {
        super(503, message);
    }

    public MilvusConnectionException(String message, Throwable cause) {
        super(message);
        // CustomException doesn't have a constructor with cause, so we init cause manually if needed
        // But RuntimeException does. CustomException extends RuntimeException.
        // Let's just use the message for now or update CustomException.
        // Since I cannot easily update CustomException without checking if it breaks other things (it's simple enough),
        // I'll just set the cause via initCause if needed, or just rely on the message.
        this.initCause(cause);
    }
}
